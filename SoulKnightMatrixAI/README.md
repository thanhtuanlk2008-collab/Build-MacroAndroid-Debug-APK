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
- [x] Matrix finite-state machine, including a special-route-check state
- [x] Grid A* pathfinder
- [x] Post-boss X-5 Sir Verdant route decision policy with Glacier/Relics exclusions
- [x] Unit tests for Sir Verdant route decisions
- [x] Macro configuration model with interval validation
- [x] SharedPreferences persistence for the default macro configuration
- [x] Multiple named macro profiles (save, load, delete)
- [x] Independent timed scheduler for attack, skill and weapon-switch events
- [x] Scheduler handles callback exceptions without silently cancelling recurring jobs
- [x] Compose UI for macro settings, profile management, start, stop and emergency stop
- [x] Main-thread dispatch for Compose state updates from scheduler callbacks
- [x] Unit tests for MacroConfig and MacroScheduler
- [x] Robolectric tests for Android SharedPreferences profile persistence
- [x] Permission-gated InputController and user-enabled AccessibilityService registration
- [x] Dashboard link to Android Accessibility settings and live service-connected status
- [ ] Visual biome recognition (Glacier / Relics / other)
- [ ] Visual recognition and tracking of the Sir Verdant entrance
- [ ] Connect route decisions to GridMap/A* navigation and live game input
- [ ] Connect Macro/AI action plans to configured screen targets through InputController
- [ ] Device test for tap/hold gesture dispatch
- [ ] Character and weapon recognition
- [ ] Device test for joystick plus action-button multitouch

## Sir Verdant route policy

After a boss is confirmed defeated in room X-5:
- Glacier and Relics always skip the Sir Verdant side room and continue the normal route.
- In other confirmed biomes, the AI waits for a completed route scan and always prioritizes a Sir Verdant route when its detection confidence is at least 0.80.
- If a completed scan finds no sufficiently confident Sir Verdant route, the policy selects the normal route.
- If the biome is unknown or route scanning is incomplete, the policy waits instead of guessing.

Implementation: `matrix/SpecialRoomRoutePolicy.kt`. The policy is a deterministic decision layer and is unit-tested. It consumes biome and route detections; it does not yet visually recognize the biome/entrance or issue movement commands. The state machine now includes `SPECIAL_ROUTE_CHECK`, but live vision-to-navigation wiring remains a later step.

## Important implementation boundary

The current MacroScheduler emits timed events only. It does **not** inject touch input into Soul Knight. The UI explicitly identifies this as test mode. An InputController gateway and AccessibilityService bridge now exist. The user must explicitly enable the service in Android Settings. MacroScheduler is still event-only and is not yet wired to screen coordinates; no game action is sent automatically. The service supports a single dispatched gesture path, not independent simultaneous multitouch. Android's AccessibilityService API has no dedicated cancel-current-gesture method, so emergency stop currently stops scheduling but cannot promise cancellation of an already-dispatched gesture.

Screen capture requires the user to approve Android's MediaProjection prompt. The current capture service validates the capture lifecycle but does not yet expose processed frames to a semantic game-recognition model.

## InputController – trạng thái tích hợp

- `InputTarget` / `InputTargetConfig`: tọa độ màn hình do người dùng nhập cho nút tấn công, kỹ năng và đổi vũ khí.
- `InputTargetStore`: lưu tọa độ trong SharedPreferences.
- `InputActionQueue`: xếp hàng tuần tự, giới hạn backlog 32 thao tác để tránh queue tăng vô hạn.
- Dashboard cho phép lưu tọa độ X/Y; MacroScheduler chuyển sự kiện đến InputActionQueue và InputController.
- Bật AccessibilityService bằng tay trong Android Settings. Không có thao tác cấp quyền tự động.

### Giới hạn và cách kiểm thử

Tọa độ là pixel màn hình hiện tại, phụ thuộc độ phân giải, hướng màn hình, vùng hiển thị và bố cục nút trong game. Người dùng cần nhập lại nếu các yếu tố này thay đổi. Không có tự động nhận diện vị trí nút.

Queue chỉ gửi tuần tự, không có multitouch độc lập. Stop ngăn các mục đang chờ được gửi tiếp; Android không cung cấp API Accessibility công khai để hủy chắc chắn gesture đã được gửi và đang chạy. Vì vậy Emergency Stop không thể bảo đảm thu hồi một gesture đang thực thi.

Chưa đánh dấu InputController hoàn tất cho tới khi workflow build/test thành công và thao tác được kiểm thử trên thiết bị Android thật.


## Visual Sir Verdant route analysis

The visual baseline now includes:
- `vision/TemplateMatcher.kt`: deterministic normalized RGB reference-template matching.
- `vision/MatrixRouteVisionAnalyzer.kt`: compares reference crops for biome and the Sir Verdant entrance.
- `matrix/MatrixRouteCoordinator.kt`: passes visual results into the post-boss route policy.
- Unit tests cover exact template matching, Sir Verdant priority and Glacier exclusion.

This is **not a trained AI model**. It requires reference crops taken from actual Soul Knight screenshots at matching scale and orientation. No game-specific templates are bundled. The screen-capture service still closes incoming frames instead of forwarding them to this analyzer, so this code is currently a tested analysis component, not live in-game recognition. It also does not move the character.

Remaining work:
- Forward approved MediaProjection frames into the vision pipeline.
- Add/collect reference crops for Glacier, Relics, other biomes and the Sir Verdant entrance.
- Validate matching against animation, scaling and visual effects.
- Connect the detected entrance to GridMap/A* and a separately verified navigation/input controller.

## Screen-frame delivery

The MediaProjection service now converts approved RGBA frames into bounded-size ARGB `FrameSnapshot` objects (maximum width 480, approximately 5 frames/second) on a dedicated handler thread. `FrameSnapshotStore` exposes the latest frame to in-process vision consumers, and the dashboard displays the incoming frame dimensions.

This connects screen capture to a reusable frame stream. It does not yet automatically call the route analyzer: game-specific reference templates and calibrated screen regions are still required, and the user must grant Android screen-capture permission each session.

## Player tracking and bounded joystick input (2026-11-21)

- `PlayerPositionTracker` stores a small image patch around a user-selected player centre and searches locally in subsequent captured frames. It returns a confidence score and timestamp; low-confidence matches are rejected. This is a lightweight tracking primitive, not a trained character detector, and animated effects or camera changes can cause tracking loss.
- `InputController.swipe` adds a single path gesture through the user-enabled AccessibilityService.
- `JoystickController` converts a normalized direction into a bounded joystick drag. Calibration gestures are limited to 80–600 ms and an 8–180 px radius, with endpoint checks against supplied screen dimensions.
- Unit tests cover patch tracking on synthetic frames and rejection of out-of-bounds/overlong joystick requests.

### Integration status and safety limits

The tracker and joystick gesture APIs are committed as reusable components, but are not yet connected to an in-game floating calibration overlay or a full automatic calibration sequence. A player seed point must come from a real gameplay frame, and the game must be foreground when a joystick gesture is deliberately dispatched. The current MainActivity does not provide an overlay; therefore do not treat this milestone as a one-button in-game speed scan. A follow-up milestone must add an explicit, user-visible overlay/arming flow, live tracker session lifecycle, measured start/end positions and stop-on-low-confidence behavior before automatic trials are enabled. Accessibility gestures cannot guarantee cancellation after dispatch.
