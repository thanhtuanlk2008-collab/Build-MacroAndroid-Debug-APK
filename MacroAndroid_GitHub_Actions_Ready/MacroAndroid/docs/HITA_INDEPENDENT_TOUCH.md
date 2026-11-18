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
| Shizuku UserService | Not implemented | Must probe permissions from the shell-side process; checking the app UID says nothing about UID 2000 |
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
