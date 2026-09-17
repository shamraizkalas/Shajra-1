package pk.shajranasab.data
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import pk.shajranasab.model.*
import java.security.MessageDigest
class FirebaseRepository {
 private val auth=FirebaseAuth.getInstance(); private val db=FirebaseFirestore.getInstance(); private val functions=FirebaseFunctions.getInstance()
 fun currentUid()=auth.currentUser?.uid; fun currentEmail()=auth.currentUser?.email
 suspend fun signIn(email:String,password:String)=auth.signInWithEmailAndPassword(email.trim(),password).await()
 suspend fun signUp(email:String,password:String,name:String,pin:String){require((pin.length==4||pin.length==6)&&pin.all(Char::isDigit)){"PIN 4 یا 6 ہندسوں کا ہونا چاہیے"};val r=auth.createUserWithEmailAndPassword(email.trim(),password).await();db.collection("users").document(r.user!!.uid).set(UserProfile(r.user!!.uid,name.trim(),email.trim(),"viewer",false,sha(pin))).await();auth.signOut()}
 fun signOut()=auth.signOut(); suspend fun profile():UserProfile?{val u=currentUid()?:return null;return db.collection("users").document(u).get().await().toObject(UserProfile::class.java)}
 fun peopleSnapshot(cb:(List<Person>)->Unit)=db.collection("people").orderBy("name").addSnapshotListener{s,e->if(e==null)cb(s?.documents?.mapNotNull{it.toObject(Person::class.java)?.copy(id=it.id)}?:emptyList())}
 fun peopleOnce(cb:(List<Person>)->Unit)=db.collection("people").orderBy("name").get().addOnSuccessListener{s->cb(s.documents.mapNotNull{it.toObject(Person::class.java)?.copy(id=it.id)})}
 suspend fun person(id:String)=db.collection("people").document(id).get().await().toObject(Person::class.java)?.copy(id=id)
 suspend fun savePerson(p:Person){val uid=currentUid()?:error("Login required");val r=if(p.id.isBlank())db.collection("people").document() else db.collection("people").document(p.id);r.set(p.copy(id=r.id,updatedBy=uid,createdBy=p.createdBy.ifBlank{uid})).await()}
 suspend fun deletePerson(id:String)=db.collection("people").document(id).delete().await()
 fun announcements(cb:(List<Announcement>)->Unit)=db.collection("announcements").orderBy("createdAt",Query.Direction.DESCENDING).addSnapshotListener{s,e->if(e==null)cb(s?.documents?.mapNotNull{it.toObject(Announcement::class.java)?.copy(id=it.id)}?:emptyList())}
 suspend fun saveAnnouncement(a:Announcement){val r=if(a.id.isBlank())db.collection("announcements").document() else db.collection("announcements").document(a.id);r.set(a.copy(id=r.id,createdAt=if(a.createdAt==0L)System.currentTimeMillis() else a.createdAt,createdBy=currentUid() ?: "")).await()}
 suspend fun deleteAnnouncement(id:String)=db.collection("announcements").document(id).delete().await()
 fun auditSnapshot(cb:(List<AuditLog>)->Unit)=db.collection("auditLogs").orderBy("timestamp",Query.Direction.DESCENDING).limit(1000).addSnapshotListener{s,e->if(e==null)cb(s?.documents?.mapNotNull{it.toObject(AuditLog::class.java)?.copy(id=it.id)}?:emptyList())}
 suspend fun approveUser(uid:String,a:Boolean)=functions.getHttpsCallable("setUserApproval").call(mapOf("uid" to uid,"approved" to a)).await()
 suspend fun makeAdmin(uid:String)=functions.getHttpsCallable("makeAdmin").call(mapOf("uid" to uid)).await()
 suspend fun createTempAccess(uid:String)=functions.getHttpsCallable("createTemporaryAccess").call(mapOf("uid" to uid)).await()
 suspend fun requestAdminReset(email:String)=functions.getHttpsCallable("requestAdminReset").call(mapOf("email" to email)).await()
 suspend fun resetByPin(email:String,pin:String,newPassword:String,newPin:String){val r=functions.getHttpsCallable("resetPasswordByPin").call(mapOf("email" to email,"pin" to pin,"newPassword" to newPassword,"newPin" to newPin)).await();val t=(r.data as Map<*,*>)["customToken"] as String;auth.signInWithCustomToken(t).await();auth.currentUser?.updatePassword(newPassword)?.await();functions.getHttpsCallable("setPinAfterTemporaryAccess").call(mapOf("pin" to newPin)).await()}
 suspend fun useTempCode(email:String,code:String,newPassword:String,newPin:String){val r=functions.getHttpsCallable("useTemporaryAccess").call(mapOf("email" to email,"code" to code)).await();val t=(r.data as Map<*,*>)["customToken"] as String;auth.signInWithCustomToken(t).await();auth.currentUser?.updatePassword(newPassword)?.await();functions.getHttpsCallable("setPinAfterTemporaryAccess").call(mapOf("pin" to newPin)).await()}
 suspend fun publishFcmToken(){val uid=currentUid()?:return;db.collection("users").document(uid).update("fcmToken",FirebaseMessaging.getInstance().token.await()).await()}
 private fun sha(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){ "%02x".format(it)}
}
