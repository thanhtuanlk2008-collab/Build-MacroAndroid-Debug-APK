# HITA — Independent Touch Research

## Current status

This change adds the first architecture layer only. It does **not** enable independent physical and macro touch streams.

Android's AccessibilityService `dispatchGesture()` is still the current playback/recording-forwarding mechanism in this project. It can cancel an in-progress gesture, so it cannot satisfy the project's strongest requirement: a real finger gesture and a macro-generated touch remaining active independently at the same time.

## Added components

- `input/MacroInputBackend`: contract for future injection backends.
- `input/TouchSessionManager`: logical bookkeeping keyed by source and pointer ID.
- `input/InputBackendSelector`: reports the permissions/capabilities this app process can actually prove.

The session manager does not create MotionEvent streams or bypass Android's input dispatcher. Separate logical IDs are not proof of separate kernel/input streams.

## Backend status

| Backend | Status | Reason |
|---|---|---|
| Accessibility `dispatchGesture()` | Existing fallback | Synthetic gestures can conflict with/cancel ongoing gestures |
| Shizuku UserService | Diagnostic only; no input injector | The probe reports the UserService UID and permission state; it does not send touch events or establish independent streams |
| ADB shell | Not implemented | `input tap/swipe` is not proof of concurrent independent touchscreen streams |
| Virtual input device/uinput | Research only | Usually requires device-node, SELinux and/or privileged access not available to an ordinary app |

The selector deliberately reports `independentTouchStreamAvailable = false`. Do not change this to true merely because Shizuku is running or a permission check passes.

## Validation gates before enabling an independent backend

1. Probe the exact execution UID and permission set inside the backend process.
2. Confirm the target Android build permits the required injection API.
3. Test a sustained physical pointer while a macro pointer is pressed and moved.
4. Verify the physical pointer's coordinates, duration and release are unchanged.
5. Verify macro pointer down/move/up are delivered without cancelling the physical pointer.
6. Repeat on the target OEM firmware and in a controlled test app before testing a game.
7. Only then advertise the backend as supporting `INDEPENDENT_TOUCH_STREAM`.

## Current user-visible behavior

The existing Accessibility fallback remains available when Accessibility is connected, but the app must not promise true parallel touch operation. The floating tap button remains a small overlay; this does not solve the independent-input problem by itself.


## Shizuku UserService diagnostic phase

The project now includes the Shizuku API/provider and an AIDL-backed UserService probe.

- The diagnostics screen has a **Kiểm tra Shizuku** action.
- After the user grants MacroAndroid permission in Shizuku, the app starts a UserService.
- The UserService reports its actual Linux UID, PID, and whether that process passes the `android.permission.INJECT_EVENTS` permission check.
- On a non-root Shizuku start, the expected identity is usually shell UID 2000; the probe reports the actual value rather than assuming it.
- This probe does not inject taps, swipes, or any other input.

A positive `INJECT_EVENTS` permission result is only a prerequisite for further investigation. It does not prove that synthetic and physical touchscreen streams remain independent, and it does not yet enable the Shizuku backend for macro playback.

## User test procedure (after a debug build is explicitly requested)

1. Install and start Shizuku using its supported setup for the device.
2. Open MacroAndroid → Testing.
3. Tap **Kiểm tra Shizuku** and approve the app permission prompt.
4. Share the diagnostic text showing UID and `INJECT_EVENTS` status.

Do not test simultaneous touch injection yet; this version only performs a permission/identity probe.
