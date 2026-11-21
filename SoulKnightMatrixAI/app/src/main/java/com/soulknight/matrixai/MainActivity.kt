package com.soulknight.matrixai

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.soulknight.matrixai.capture.ScreenCaptureService

private val MatrixAqua = Color(0xFF00AFC1)
private val MatrixAquaDark = Color(0xFF007C91)
private val MatrixAquaPale = Color(0xFFE5F8FA)
private val MatrixInk = Color(0xFF15343A)
private val MatrixScheme = lightColorScheme(
    primary = MatrixAquaDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F0F4),
    onPrimaryContainer = Color(0xFF00363E),
    secondary = Color(0xFF168C9C),
    onSecondary = Color.White,
    secondaryContainer = MatrixAquaPale,
    onSecondaryContainer = MatrixInk,
    background = Color(0xFFF4FAFB),
    onBackground = MatrixInk,
    surface = Color.White,
    onSurface = MatrixInk,
    surfaceVariant = Color(0xFFE6F1F2),
    onSurfaceVariant = Color(0xFF496267),
    outline = Color(0xFFB7CDD0)
)

class MainActivity : ComponentActivity() {
    private var captureStatus by mutableStateOf("Chưa cấp quyền chụp màn hình")

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            captureStatus = "Đã cấp quyền · Đang chờ khung hình"
        } else {
            captureStatus = "Chưa cấp quyền chụp màn hình"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(0, 124, 145)
        window.navigationBarColor = android.graphics.Color.rgb(244, 250, 251)

        setContent {
            MaterialTheme(colorScheme = MatrixScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                "SOUL KNIGHT",
                                color = MatrixAquaDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Matrix AI",
                                color = MatrixInk,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Nhận diện phòng và theo dõi nhân vật tự động",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MatrixAquaPale)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "TRẠNG THÁI HỆ THỐNG",
                                    color = MatrixAquaDark,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    captureStatus,
                                    color = MatrixInk,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Cấp quyền chụp màn hình để AI nhận dữ liệu hình ảnh trực tiếp từ game.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("01 · Chụp màn hình", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Dịch vụ này cung cấp khung hình cho bộ nhận diện. Bạn cần cho phép Android chụp màn hình mỗi lần khởi động dịch vụ.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = {
                                        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                        captureLauncher.launch(manager.createScreenCaptureIntent())
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cấp quyền và bắt đầu")
                                }
                                OutlinedButton(
                                    onClick = {
                                        stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                                        captureStatus = "Đã yêu cầu dừng chụp màn hình"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Dừng chụp màn hình")
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("02 · Nhận diện nhân vật", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Tự tìm vùng hình ảnh giống nhân vật trên màn hình, không yêu cầu chọn tọa độ ban đầu.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                com.soulknight.matrixai.vision.PlayerAutoDetectionPanel()
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("03 · Dữ liệu nhận diện", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Thu thập ảnh phòng để bổ sung dữ liệu màu sắc và họa tiết của các ô sàn.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                com.soulknight.matrixai.capture.MatrixDatasetPanel()
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Bản thử nghiệm · Nhận diện nhân vật hiện tìm ứng viên hình ảnh, chưa phân biệt hoàn toàn người chơi với quái hoặc hiệu ứng.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
