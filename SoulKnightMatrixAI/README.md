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
- [x] Robolectric tests for Android SharedPreferences profile persistence
- [x] Permission-gated InputController and user-enabled AccessibilityService registration
- [x] Dashboard link to Android Accessibility settings and live service-connected status
- [ ] Connect Macro/AI action plans to configured screen targets through InputController
- [ ] Device test for tap/hold gesture dispatch
- [ ] Character and weapon recognition
- [ ] Device test for joystick plus action-button multitouch

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
