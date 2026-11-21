# Soul Knight Matrix AI

Android/Kotlin modular project. Milestone 1 provides the Android foundation and a user-consented MediaProjection foreground service.

## Build
Requires JDK 17, Android SDK Platform 35 and Gradle 8.10.2.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Milestone status
- [x] Android app shell
- [x] User-mediated screen capture permission
- [x] MediaProjection foreground service
- [ ] Frame analysis / OpenCV
- [ ] Character and weapon recognition
- [ ] Matrix state machine and navigation
- [ ] Input adapter feasibility testing

The current capture service acquires and immediately closes frames to validate the capture pipeline. It does not yet analyze the game or send touch input.

Workflow Android SDK bootstrap has been updated to use the runner-provided command-line tools.
