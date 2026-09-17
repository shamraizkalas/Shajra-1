const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentWrittenWithAuthContext } = require('firebase-functions/v2/firestore');
const { setGlobalOptions } = require('firebase-functions/v2');
const admin = require('firebase-admin');
const crypto = require('crypto');

admin.initializeApp();
setGlobalOptions({ region: 'asia-south1', maxInstances: 10 });

const db = admin.firestore();
const auth = admin.auth();
const SUPER_ADMIN_EMAIL = 'shamraizkalas@gmail.com';
const sha = (value) => crypto.createHash('sha256').update(String(value)).digest('hex');

function normalizeEmail(email) { return String(email || '').trim().toLowerCase(); }

async function getProfile(uid) {
  if (!uid) return null;
  const snap = await db.doc(`users/${uid}`).get();
  return snap.exists ? snap.data() : null;
}

async function requireAdmin(uid, superOnly = false) {
  if (!uid) throw new HttpsError('unauthenticated', 'Login required');
  const user = await auth.getUser(uid);
  const emailIsSuper = normalizeEmail(user.email) === SUPER_ADMIN_EMAIL;
  const profile = await getProfile(uid);
  const role = profile?.role;
  if (superOnly && !emailIsSuper) throw new HttpsError('permission-denied', 'Super Admin only');
  if (!superOnly && !emailIsSuper && !['admin', 'superAdmin'].includes(role)) {
    throw new HttpsError('permission-denied', 'Admin permission required');
  }
  return { user, profile, emailIsSuper };
}

async function sendToApproved(title, body, route) {
  const snap = await db.collection('users').where('approved', '==', true).get();
  const tokens = [...new Set(snap.docs.map(d => d.data().fcmToken).filter(Boolean))];
  if (!tokens.length) return;
  for (let i = 0; i < tokens.length; i += 500) {
    await admin.messaging().sendEachForMulticast({
      tokens: tokens.slice(i, i + 500),
      notification: { title, body },
      data: { title, body, route: route || 'tree' }
    });
  }
}

async function writeNotification(uid, title, body, route) {
  await db.collection('notifications').add({
    uid, title, body, route: route || 'tree', read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

exports.setUserApproval = onCall(async (req) => {
  await requireAdmin(req.auth?.uid);
  const uid = String(req.data?.uid || '');
  const approved = !!req.data?.approved;
  if (!uid) throw new HttpsError('invalid-argument', 'uid required');
  const target = await getProfile(uid);
  if (!target) throw new HttpsError('not-found', 'User profile not found');
  await db.doc(`users/${uid}`).update({ approved });
  await writeNotification(uid, approved ? 'اکاؤنٹ منظور ہو گیا' : 'اکاؤنٹ مسترد کر دیا گیا',
    approved ? 'آپ اب مکمل طور پر ایپ استعمال کر سکتے ہیں۔' : 'آپ کے اکاؤنٹ کی منظوری نہیں دی گئی۔', 'tree');
  return { ok: true };
});

exports.makeAdmin = onCall(async (req) => {
  await requireAdmin(req.auth?.uid, true);
  const uid = String(req.data?.uid || '');
  if (!uid) throw new HttpsError('invalid-argument', 'uid required');
  await db.doc(`users/${uid}`).update({ role: 'admin', approved: true });
  return { ok: true };
});

exports.createTemporaryAccess = onCall(async (req) => {
  await requireAdmin(req.auth?.uid);
  const uid = String(req.data?.uid || '');
  if (!uid) throw new HttpsError('invalid-argument', 'uid required');
  const code = String(crypto.randomInt(100000, 1000000));
  await db.doc(`tempAccess/${uid}`).set({
    hash: sha(code), expiresAt: Date.now() + 10 * 60 * 1000,
    createdBy: req.auth.uid, createdAt: Date.now()
  });
  return { code, expiresAt: Date.now() + 10 * 60 * 1000 };
});

exports.requestAdminReset = onCall(async (req) => {
  const email = normalizeEmail(req.data?.email);
  if (!email) throw new HttpsError('invalid-argument', 'Email required');
  const q = await db.collection('users').where('email', '==', email).limit(1).get();
  if (q.empty) throw new HttpsError('not-found', 'User not found');
  const u = q.docs[0];
  await db.doc(`resetRequests/${u.id}`).set({
    uid: u.id, email, name: u.data().name || '', createdAt: Date.now(), status: 'pending'
  });
  const admins = await db.collection('users').where('approved', '==', true).get();
  for (const d of admins.docs) {
    const p = d.data();
    if (['admin', 'superAdmin'].includes(p.role) || normalizeEmail(p.email) === SUPER_ADMIN_EMAIL) {
      await writeNotification(d.id, 'پاسورڈ ری سیٹ درخواست', `${u.data().name || email} نے ری سیٹ کی درخواست کی ہے`, 'reset');
    }
  }
  return { ok: true };
});

exports.resetPasswordByPin = onCall(async (req) => {
  const email = normalizeEmail(req.data?.email);
  const pin = String(req.data?.pin || '');
  if (!email || !pin) throw new HttpsError('invalid-argument', 'Email and PIN required');
  const q = await db.collection('users').where('email', '==', email).limit(1).get();
  if (q.empty) throw new HttpsError('not-found', 'User not found');
  const uid = q.docs[0].id;
  const profile = q.docs[0].data();
  const ref = db.doc(`pinAttempts/${uid}`);
  const attempts = (await ref.get()).data() || {};
  if ((attempts.blockedUntil || 0) > Date.now()) {
    throw new HttpsError('resource-exhausted', '5 منٹ بعد دوبارہ کوشش کریں');
  }
  if (!profile.pinHash || profile.pinHash !== sha(pin)) {
    const count = (attempts.count || 0) + 1;
    if (count >= 3) {
      await ref.set({ count, blockedUntil: Date.now() + 5 * 60 * 1000 });
      throw new HttpsError('resource-exhausted', '3 غلط کوششیں۔ 5 منٹ کے لیے بلاک');
    }
    await ref.set({ count, blockedUntil: 0 });
    throw new HttpsError('permission-denied', 'غلط PIN');
  }
  await ref.set({ count: 0, blockedUntil: 0 });
  const token = await auth.createCustomToken(uid, { temporaryReset: true });
  return { customToken: token, expiresAt: Date.now() + 10 * 60 * 1000 };
});

exports.useTemporaryAccess = onCall(async (req) => {
  const email = normalizeEmail(req.data?.email);
  const code = String(req.data?.code || '');
  const q = await db.collection('users').where('email', '==', email).limit(1).get();
  if (q.empty) throw new HttpsError('not-found', 'User not found');
  const uid = q.docs[0].id;
  const ref = db.doc(`tempAccess/${uid}`);
  const data = (await ref.get()).data();
  if (!data || !data.expiresAt || data.expiresAt < Date.now() || data.hash !== sha(code)) {
    throw new HttpsError('permission-denied', 'Invalid or expired code');
  }
  await ref.delete();
  const token = await auth.createCustomToken(uid, { temporaryAccess: true });
  return { customToken: token, expiresAt: Date.now() + 10 * 60 * 1000 };
});

exports.setPinAfterTemporaryAccess = onCall(async (req) => {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Login required');
  if (!req.auth.token.temporaryAccess && !req.auth.token.temporaryReset) {
    throw new HttpsError('permission-denied', 'Temporary access required');
  }
  const pin = String(req.data?.pin || '');
  if (!/^\d{4}(\d{2})?$/.test(pin)) throw new HttpsError('invalid-argument', 'PIN must be 4 or 6 digits');
  await db.doc(`users/${req.auth.uid}`).update({ pinHash: sha(pin) });
  return { ok: true };
});

exports.auditPeople = onDocumentWrittenWithAuthContext('people/{id}', async (event) => {
  const before = event.data?.before?.data() || null;
  const after = event.data?.after?.data() || null;
  if (!before && !after) return;
  const action = !before ? 'add' : !after ? 'delete' : 'update';
  const actorUid = event.auth?.uid || after?.updatedBy || before?.updatedBy || 'system';
  const actor = actorUid === 'system' ? null : await getProfile(actorUid);
  await db.collection('auditLogs').add({
    action, personId: event.params.id,
    personName: after?.name || before?.name || '',
    actorUid, actorName: actor?.name || '',
    timestamp: Date.now(), before, after
  });
  const snap = await db.collection('auditLogs').orderBy('timestamp', 'desc').limit(1001).get();
  if (snap.size > 1000) {
    const batch = db.batch();
    snap.docs.slice(1000).forEach(doc => batch.delete(doc.ref));
    await batch.commit();
  }
  if (action === 'delete') await sendToApproved('اہم تبدیلی', 'ایک فرد شجرہ نسب سے حذف کیا گیا ہے', 'tree');
});

exports.auditAnnouncements = onDocumentWrittenWithAuthContext('announcements/{id}', async (event) => {
  const before = event.data?.before?.data() || null;
  const after = event.data?.after?.data() || null;
  if (!after) return;
  const isNew = !before;
  if (isNew) await sendToApproved('نیا اعلان آ گیا ہے', after.title || 'نیا اعلان', 'ann');
  await db.collection('auditLogs').add({
    action: isNew ? 'announcement_add' : 'announcement_update',
    personId: event.params.id, personName: after.title || '',
    actorUid: event.auth?.uid || after.createdBy || '', actorName: '',
    timestamp: Date.now(), before, after
  });
});

exports.notifyNewUser = onDocumentWrittenWithAuthContext('users/{uid}', async (event) => {
  if (!event.data?.after?.exists || event.data?.before?.exists) return;
  const u = event.data.after.data();
  if (u.role === 'superAdmin' && normalizeEmail(u.email) === SUPER_ADMIN_EMAIL) return;
  const admins = await db.collection('users').where('approved', '==', true).get();
  for (const d of admins.docs) {
    const p = d.data();
    if (['admin', 'superAdmin'].includes(p.role) || normalizeEmail(p.email) === SUPER_ADMIN_EMAIL) {
      await writeNotification(d.id, 'نیا یوزر منظوری کا منتظر ہے', u.name || u.email, 'users');
    }
  }
});
