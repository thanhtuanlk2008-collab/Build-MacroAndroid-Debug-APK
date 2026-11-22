package com.macro.app.ui.biome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macro.app.biome.BiomeId
import com.macro.app.data.BiomeFeedbackEntity
import com.macro.app.ui.theme.GamingColors

private val matrixBiomeOptions = listOf(
    BiomeId.FOREST, BiomeId.GLACIER, BiomeId.RELICS, BiomeId.MECHANICAL_RUINS,
    BiomeId.KNIGHT_KINGDOM, BiomeId.DUNGEON, BiomeId.HALLOWEEN, BiomeId.CHISELTOWN,
    BiomeId.SPACESHIP, BiomeId.VOLCANO, BiomeId.NEO_ISLE,
    BiomeId.CAVE, BiomeId.GRAVE, BiomeId.SWAMP
)

private fun BiomeId.vietnameseName(): String = when (this) {
    BiomeId.FOREST -> "Rừng"
    BiomeId.GLACIER -> "Sông băng"
    BiomeId.RELICS -> "Di tích"
    BiomeId.MECHANICAL_RUINS -> "Tàn tích cơ khí"
    BiomeId.KNIGHT_KINGDOM -> "Vương quốc Hiệp sĩ"
    BiomeId.DUNGEON -> "Ngục tối"
    BiomeId.HALLOWEEN -> "Halloween"
    BiomeId.CHISELTOWN -> "Thị trấn đục đá"
    BiomeId.SPACESHIP -> "Tàu vũ trụ"
    BiomeId.VOLCANO -> "Núi lửa"
    BiomeId.NEO_ISLE -> "Neo Isle"
    BiomeId.CAVE -> "Hang động (chờ xác minh)"
    BiomeId.GRAVE -> "Nghĩa địa (chờ xác minh)"
    BiomeId.SWAMP -> "Đầm lầy (chờ xác minh)"
}

@Composable
fun BiomeReviewScreen(
    pending: List<BiomeFeedbackEntity>,
    onConfirm: (String, BiomeId) -> Unit,
    onDiscard: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val selected = remember { mutableStateMapOf<String, BiomeId>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(GamingColors.ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("BIOME REVIEW", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Xác nhận hoặc sửa kết quả nhận diện để tạo dữ liệu gameplay đáng tin cậy.",
                style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
            Text("Điểm tương đồng chỉ là điểm so khớp, không phải xác suất. Mỗi mục hiện chỉ lưu vector màu, chưa lưu ảnh màn hình.",
                style = MaterialTheme.typography.bodySmall, color = GamingColors.AmberWarn)
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Làm mới danh sách")
            }
        }

        if (pending.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                    border = BorderStroke(1.dp, GamingColors.BorderSubtle)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Chưa có dự đoán cần xác nhận", fontWeight = FontWeight.Bold, color = GamingColors.TextPrimary)
                        Text("Dự đoán sẽ xuất hiện khi engine nhận được khung hình gameplay. Tính năng thu khung hình chưa được kết nối.",
                            style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                    }
                }
            }
        } else {
            items(pending, key = { it.id }) { item ->
                val predicted = item.predictedBiome?.let { runCatching { BiomeId.valueOf(it) }.getOrNull() }
                val current = selected[item.id] ?: predicted ?: BiomeId.FOREST
                var menuOpen by remember(item.id) { mutableStateOf(false) }

                Card(colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                    border = BorderStroke(1.dp, GamingColors.BorderSubtle)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Mẫu chờ xác nhận", fontWeight = FontWeight.Bold, color = GamingColors.TextPrimary)
                        Text("Dự đoán: ${predicted?.vietnameseName() ?: "Chưa xác định"}",
                            style = MaterialTheme.typography.bodyMedium, color = GamingColors.CyberBlue)
                        Text("Điểm so khớp: ${item.similarity?.let { "%.1f".format(it * 100f) } ?: "—"}%",
                            style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { menuOpen = true }) { Text(current.vietnameseName()) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                matrixBiomeOptions.forEach { biome ->
                                    DropdownMenuItem(text = { Text(biome.vietnameseName()) }, onClick = {
                                        selected[item.id] = biome
                                        menuOpen = false
                                    })
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onConfirm(item.id, current) }, modifier = Modifier.weight(1f)) {
                                Text("Xác nhận biome")
                            }
                            OutlinedButton(onClick = { onDiscard(item.id) }) { Text("Bỏ") }
                        }
                    }
                }
            }
        }
    }
}
