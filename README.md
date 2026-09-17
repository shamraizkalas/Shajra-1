# شجرہ نسب — خاندان محمد علی

Android Kotlin/Jetpack Compose + Firebase backend.

## Firebase used
- Authentication: Email/Password
- Cloud Firestore: family tree, users, announcements, audit logs, notifications and reset workflow
- Cloud Functions: approval, Admin promotion, temporary access, PIN reset and audit/notification automation
- Firebase Cloud Messaging: push notifications

Firebase Storage and Realtime Database are intentionally not used.

## Firebase project
- Project ID: `fintrack-ai-z8r9w`
- Android application ID: `com.shajranasab`
- Cloud Functions region: `asia-south1`

The app's `google-services.json` is included for the configured project.

## Super Admin
The configured Super Admin email is:
`shamraizkalas@gmail.com`

No manual UID entry is required. Firestore Rules recognize this email as the Super Admin. The first login can bootstrap a `users/{uid}` profile as `superAdmin`.

## Deploy backend
From the project root with Firebase CLI installed and authenticated:

```bash
firebase deploy --only firestore:rules,functions
```

## Firestore collections
- `users`
- `people`
- `announcements`
- `auditLogs`
- `notifications`
- `resetRequests`
- `tempAccess`
- `pinAttempts`

`auditLogs`, `tempAccess`, and `pinAttempts` are server-controlled; clients cannot write them directly.

## Important security note
The Firebase Android API key in `google-services.json` is not a password. Never add Firebase Admin SDK service-account private keys, passwords, or other server credentials to this repository or send them in chat.

## Build note
This source has been prepared for Android Studio/Gradle, but an APK should only be claimed as built after running a real Gradle Android build in an environment with the Android SDK installed.
