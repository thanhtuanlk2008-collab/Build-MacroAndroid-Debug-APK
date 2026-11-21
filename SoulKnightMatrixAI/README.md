# Soul Knight Matrix AI

Android/Kotlin modular project. The app separates user-configured Macro scheduling from the future AI/vision/navigation modules.

## Build
Requires JDK 17, Android SDK Platform 35 and Gradle 8.10.2.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Milestone status
- [x] Android app shell
- [x] User-mediated MediaProjection foreground service
- [x] Macro configuration model and SharedPreferences persistence
- [x] Independent timed scheduler for attack, skill and weapon-switch events
- [x] Compose UI to configure, save, start and stop macro scheduling
- [x] Frame data model and lightweight RGB frame statistics (baseline, not semantic AI)
- [x] Matrix finite-state machine and grid A* pathfinder (requires a populated map)
- [ ] InputController to send game touch gestures
- [ ] Character and weapon recognition
- [ ] Device test for joystick plus action-button multitouch

## Important implementation boundary
The current MacroScheduler emits timed events only. It does **not** inject touch input into Soul Knight. A real InputController must be implemented and tested separately with Android's permission model; independent simultaneous touch streams are not guaranteed by AccessibilityService. Screen capture requires the user to approve Android's MediaProjection prompt.
