# MacroEngine Pro (MacroAndroid)

Ứng dụng Macro Android chuyên nghiệp (không cần Root, hỗ trợ Android 10–16, tối ưu cho vivo Y31d), lấy cảm hứng từ REDMAGIC Game Space.

## Hướng dẫn Build APK

### Cách 1: Android Studio (Khuyến nghị)
1. Giải nén `MacroAndroid_Source.zip`.
2. Mở thư mục `MacroAndroid` bằng **Android Studio** (Ladybug / Meerkat trở lên).
3. Chờ Gradle Sync hoàn tất (JDK 17, Compile SDK 35).
4. Chọn **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. File APK Debug sẽ nằm tại: `app/build/outputs/apk/debug/app-debug.apk` (đổi tên thành `MacroAndroid.apk` nếu muốn).

### Cách 2: Tự động qua GitHub Actions (Không cần cài Android Studio)
1. Tạo repository mới trên GitHub và đẩy toàn bộ thư mục `MacroAndroid` lên nhánh `main`.
2. Vào tab **Actions** → workflow `Build MacroAndroid Debug APK` sẽ tự chạy.
3. Tải artifact `MacroAndroid-debug-apk` về cài trực tiếp lên vivo Y31d.

## Cài đặt & Cấp quyền trên vivo Y31d (Android 16)
1. Cài đặt APK và cho phép cài từ nguồn không xác định.
2. Vào **Cài đặt → Ứng dụng → MacroEngine Pro → menu ⋮ góc trên → Cho phép cài đặt bị hạn chế** (yêu cầu của Android 13+ đối với APK cài ngoài).
3. Mở ứng dụng → Bật **Accessibility Service** (`MacroEngine Pro Touch Automation`).
4. Cấp quyền **Hiển thị trên ứng dụng khác** (`SYSTEM_ALERT_WINDOW`) để dùng Bảng điều khiển nổi.
5. Vào **Pin → Không hạn chế / Tắt tối ưu hóa pin** để hệ thống không đóng băng Bảng nổi khi chơi game.
