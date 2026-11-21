package com.soulknight.matrixai.capture

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun MatrixDatasetPanel() {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var status by remember { mutableStateOf("Chưa bật thu thập") }
    var token by remember { mutableStateOf("") }
    var repository by remember { mutableStateOf("thanhtuanlk2008-collab/Build-MacroAndroid-Debug-APK") }
    var branch by remember { mutableStateOf("feature/soul-knight-matrix-ai") }
    var savedCount by remember { mutableIntStateOf(0) }
    var collector by remember { mutableStateOf<RoomChangeDatasetCollector?>(null) }

    DisposableEffect(context) {
        val instance = RoomChangeDatasetCollector(
            context = context,
            onSaved = { mainHandler.post { savedCount++ } },
            onStatus = { message -> mainHandler.post { status = message } }
        )
        collector = instance
        onDispose {
            instance.stop()
            collector = null
        }
    }

    Card {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Thu thập ảnh phòng")
            Text("Bộ lọc so sánh ảnh, chờ 3 khung hình ổn định rồi lưu một ảnh. Đây là phát hiện thay đổi hình ảnh, chưa xác nhận chắc chắn đó là phòng mới.")
            Text(status)
            Text("Ảnh đã lưu trong phiên cài đặt này: $savedCount")
            Button(
                onClick = { collector?.start() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Bắt đầu tự động thu thập") }
            OutlinedButton(
                onClick = { collector?.stop() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Dừng thu thập") }
            OutlinedTextField(
                value = repository,
                onValueChange = { repository = it },
                label = { Text("GitHub owner/repository") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it },
                label = { Text("Branch đích") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Fine-grained GitHub token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Token chỉ được giữ tạm trong bộ nhớ ứng dụng, không ghi vào source code hoặc SharedPreferences. Cấp quyền Contents: Read and write cho đúng repository.")
            Button(
                onClick = {
                    val files = collector?.savedFiles().orEmpty()
                    if (files.isEmpty()) {
                        status = "Chưa có ảnh để tải lên"
                    } else {
                        GitHubDatasetUploader.upload(files, token, repository.trim(), branch.trim()) { message ->
                            mainHandler.post { status = message }
                        }
                    }
                },
                enabled = token.isNotBlank() && repository.contains("/") && branch.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tải ảnh đã thu thập lên GitHub") }
        }
    }
}
