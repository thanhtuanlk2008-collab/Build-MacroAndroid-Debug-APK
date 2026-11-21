# Soul Knight Matrix AI

Dự án Android Kotlin được chuyển hướng từ MacroAndroid sang kiến trúc **Soul Knight Matrix AI**.

## Source hiện tại
- Thư mục dự án mới: [SoulKnightMatrixAI](SoulKnightMatrixAI/)
- Milestone 1: Android Foundation + MediaProjection screen capture.
- GitHub Actions: [Build Soul Knight Matrix AI APK](.github/workflows/build-matrix-ai.yml)

## Build APK
Mở tab **Actions**, chọn workflow **Build Soul Knight Matrix AI APK**, chạy workflow trên nhánh `feature/soul-knight-matrix-ai`. APK được xuất dưới dạng artifact `SoulKnightMatrixAI-debug-apk`.

## Lộ trình
1. Android Foundation
2. Vision Perception
3. Character & Weapon Recognition
4. Matrix State & Navigation
5. Input Capability Validation
6. Decision Engine Integration

## Lưu ý
Bản hiện tại mới xác minh nền tảng chụp màn hình qua quyền MediaProjection do Android yêu cầu người dùng cấp. Chưa có chức năng tự động điều khiển game. Các tệp MacroAndroid cũ vẫn được giữ trên nhánh chuyển đổi để đối chiếu và tránh mất mã nguồn trong lúc kiểm thử.
