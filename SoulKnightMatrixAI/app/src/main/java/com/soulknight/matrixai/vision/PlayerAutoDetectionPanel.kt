package com.soulknight.matrixai.vision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.Handler
import android.os.Looper
import com.soulknight.matrixai.capture.FrameSnapshotStore

@Composable
fun PlayerAutoDetectionPanel() {
    val detector = remember { PlayerCharacterDetector() }
    var result by remember {
        mutableStateOf(
            PlayerDetectionResult(
                PlayerDetectionStatus.WARMING_UP,
                message = "Chờ khung hình từ dịch vụ chụp màn hình."
            )
        )
    }
    DisposableEffect(detector) {
        val mainHandler = Handler(Looper.getMainLooper())
        val listener: (FrameSnapshot) -> Unit = { frame ->
            val next = detector.analyze(frame)
            mainHandler.post { result = next }
        }
        FrameSnapshotStore.addListener(listener)
        onDispose {
            FrameSnapshotStore.removeListener(listener)
            detector.reset()
        }
    }

    Card {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Nhận diện nhân vật tự động", style = MaterialTheme.typography.titleLarge)
            Text(result.message, style = MaterialTheme.typography.bodyMedium)
            result.candidate?.let {
                Text("Tọa độ ứng viên: X=${it.centerX}, Y=${it.centerY}")
                Text("Vùng ảnh: ${it.left},${it.top} – ${it.right},${it.bottom}")
                Text("Điểm hình ảnh: ${"%.2f".format(it.score)} · số mẫu chuyển động: ${it.changedSamples}")
            }
            if (result.alternatives.isNotEmpty()) {
                Text("Số vùng ứng viên khác: ${result.alternatives.size}")
            }
            Text(
                "Bộ dò hiện tại tìm vùng sprite chuyển động bằng sai khác khung hình, màu sắc và hình dạng; chưa phải mô hình đã học để phân biệt chắc chắn nhân vật người chơi với quái hoặc hiệu ứng.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
