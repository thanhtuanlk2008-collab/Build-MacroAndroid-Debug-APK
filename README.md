# Soul Knight Matrix AI

Dự án Android Native Kotlin tập trung vào chế độ Matrix của Soul Knight.

## Dự án chính

- Ứng dụng Android: [SoulKnightMatrixAI](SoulKnightMatrixAI/)
- Package: `com.soulknight.matrixai`
- UI: Jetpack Compose + Material 3
- Build workflow: [Build Soul Knight Matrix AI APK](.github/workflows/build-matrix-ai.yml)

## Build và kiểm thử

Mở tab **Actions**, chọn workflow **Build Soul Knight Matrix AI APK** và chạy trên nhánh `feature/soul-knight-matrix-ai`.

Workflow chạy Unit Tests trước, sau đó build APK Debug. Artifact có tên `SoulKnightMatrixAI-debug-apk`.

## Cấu trúc

Mã nguồn ứng dụng hiện tại nằm hoàn toàn trong `SoulKnightMatrixAI/`. Mã Macro Engine trong ứng dụng được giữ lại vì đây là một thành phần chức năng của Matrix AI; nó không phải ứng dụng MacroAndroid độc lập.

Thư mục `MacroAndroid_GitHub_Actions_Ready/` được giữ nguyên làm bản lưu mã nguồn cũ trên nhánh này để tránh mất dữ liệu. Nó không thuộc source set, không được biên dịch cùng Soul Knight Matrix AI và workflow cũ của nó đã được loại khỏi nhánh phát triển này. Nhánh `main` không bị thay đổi.

## Trạng thái hiện tại

- Android Compose application shell
- Macro configuration, named profiles and timed event scheduler
- User-approved MediaProjection capture service
- Frame analysis baseline, Matrix state machine and A* pathfinding
- InputController and semantic game recognition remain future milestones

**Giới hạn hiện tại:** MacroScheduler chỉ phát sự kiện theo lịch, chưa gửi thao tác chạm vào game. MediaProjection yêu cầu người dùng cấp quyền; pipeline hiện chưa nhận diện ngữ nghĩa nhân vật/kẻ địch.
