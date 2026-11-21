# Soul Knight Matrix AI

Android/Kotlin modular project. The app separates user-configured Macro scheduling from the future AI/vision/navigation modules.

## Build

Requires JDK 17, Android SDK Platform 35 and Gradle 8.10.2.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Current implementation status

- [x] Android app shell with Jetpack Compose and Material 3
- [x] User-mediated MediaProjection foreground service
- [x] Frame data model and lightweight RGB frame statistics (baseline, not semantic AI)
- [x] Matrix finite-state machine
- [x] Grid A* pathfinder
- [x] Macro configuration model with interval validation
- [x] SharedPreferences persistence for the default macro configuration
- [x] Multiple named macro profiles (save, load, delete)
- [x] Independent timed scheduler for attack, skill and weapon-switch events
- [x] Scheduler handles callback exceptions without silently cancelling recurring jobs
- [x] Compose UI for macro settings, profile management, start, stop and emergency stop
- [x] Main-thread dispatch for Compose state updates from scheduler callbacks
- [x] Unit tests for MacroConfig and MacroScheduler
- [ ] Automated tests for Android SharedPreferences profile persistence
- [ ] InputController to send game touch gestures
- [ ] Character and weapon recognition
- [ ] Device test for joystick plus action-button multitouch

## Important implementation boundary

The current MacroScheduler emits timed events only. It does **not** inject touch input into Soul Knight. The UI explicitly identifies this as test mode. A real InputController must be implemented and tested separately with Android's permission model; independent simultaneous touch streams are not guaranteed by AccessibilityService.

Screen capture requires the user to approve Android's MediaProjection prompt. The current capture service validates the capture lifecycle but does not yet expose processed frames to a semantic game-recognition model.
