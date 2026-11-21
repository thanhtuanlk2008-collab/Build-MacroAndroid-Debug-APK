package com.soulknight.matrixai

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.soulknight.matrixai.capture.ScreenCaptureService

class MainActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var status by remember { mutableStateOf("Chưa cấp quyền chụp màn hình") }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Soul Knight Matrix AI", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Milestone 1 · Android Foundation")
                        Spacer(Modifier.height(24.dp))
                        Text(status)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            captureLauncher.launch(manager.createScreenCaptureIntent())
                            status = "Đang chờ bạn cấp quyền..."
                        }) { Text("Cấp quyền chụp màn hình") }
                        OutlinedButton(onClick = {
                            stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                            status = "Đã yêu cầu dừng dịch vụ"
                        }) { Text("Dừng chụp màn hình") }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Bản Milestone 1 chỉ kiểm tra khả năng chụp màn hình. " +
                                "AI nhận diện và điều khiển game sẽ được triển khai ở các milestone sau.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
