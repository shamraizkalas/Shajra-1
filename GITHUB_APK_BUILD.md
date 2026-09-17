# Shajra Nasab — GitHub APK Build

This package is prepared for building the Android APK through GitHub Actions without Android Studio.

## Important
The repository must contain the extracted project files at its root. Do not upload only the ZIP file.

## Build
1. Upload/extract all project files into the GitHub repository root.
2. Commit the `.github/workflows/build-apk.yml` file.
3. Open GitHub -> Actions -> Build Shajra Nasab APK.
4. The workflow also runs automatically when code is pushed to `main`.
5. After a successful run, open the workflow run and download the `ShajraNasab-debug-apk` artifact.

The workflow uses Java 17 and Gradle 8.13 on GitHub's runner.
