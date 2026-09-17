# Build fix — v2

The Android app had Compose dependencies without a Compose BOM/version.
The app module now imports the Compose BOM `2026.08.00`, so Compose
dependencies such as `ui`, `foundation`, Material 3, and icons resolve
as a compatible set.

Firebase remains on the current Firebase Android BoM `34.19.0`.

The GitHub Actions workflow continues to build:
`gradle :app:assembleDebug --stacktrace --no-daemon`

This source was statically corrected from the failed project. A successful
GitHub Actions run is still required to verify the APK in the target
environment.
