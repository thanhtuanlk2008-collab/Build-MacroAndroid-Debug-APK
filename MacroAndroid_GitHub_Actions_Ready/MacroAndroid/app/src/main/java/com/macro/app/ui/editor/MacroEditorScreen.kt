package com.macro.app.ui.editor

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macro.app.engine.SpeedEngine
import com.macro.app.model.ActionType
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.model.MacroAction
import com.macro.app.model.RepeatMode
import com.macro.app.ui.theme.GamingColors
import com.macro.app.utils.CoordinateConverter
import kotlin.math.roundToInt

@Composable
fun MacroEditorScreen(
    macro: Macro,
    onMacroChange: (Macro) -> Unit,
    onSave: (Macro) -> Unit,
    onPlayTest: (Macro) -> Unit
) {
    val context = LocalContext.current
    var editingActionIndex by remember { mutableStateOf<Int?>(null) }
    var showTouchPad by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingColors.ObsidianBg)
    ) {
        // Top Header + Save/Test Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GamingColors.CarbonSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MACRO TIMELINE EDITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(macro.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GamingColors.TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onPlayTest(macro) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chạy thử")
                }
                Button(
                    onClick = {
                        onSave(macro)
                        Toast.makeText(context, "Đã lưu ${macro.name}", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lưu")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Macro Metadata Config
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                    border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = macro.name,
                            onValueChange = { onMacroChange(macro.copy(name = it)) },
                            label = { Text("Tên Macro") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Game liên kết:", style = MaterialTheme.typography.labelMedium, color = GamingColors.TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(GameCategory.entries) { _, cat ->
                                FilterChip(
                                    selected = macro.gameCategory == cat,
                                    onClick = { onMacroChange(macro.copy(gameCategory = cat)) },
                                    label = { Text(cat.displayName) }
                                )
                            }
                        }

                        Text("Chế độ lặp:", style = MaterialTheme.typography.labelMedium, color = GamingColors.TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(RepeatMode.entries) { _, mode ->
                                FilterChip(
                                    selected = macro.repeatMode == mode,
                                    onClick = { onMacroChange(macro.copy(repeatMode = mode)) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                RepeatMode.ONCE -> "1 lần"
                                                RepeatMode.FIVE_TIMES -> "5 lần"
                                                RepeatMode.TEN_TIMES -> "10 lần"
                                                RepeatMode.CUSTOM -> "Tùy chỉnh (${macro.customRepeatCount})"
                                                RepeatMode.INFINITE -> "Vô hạn"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        if (macro.repeatMode == RepeatMode.CUSTOM) {
                            OutlinedTextField(
                                value = macro.customRepeatCount.toString(),
                                onValueChange = { txt ->
                                    val count = txt.toIntOrNull()?.coerceIn(1, 9999) ?: 1
                                    onMacroChange(macro.copy(customRepeatCount = count))
                                },
                                label = { Text("Số lần lặp tùy chỉnh") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tốc độ mặc định: ${macro.defaultSpeed}x", style = MaterialTheme.typography.bodyMedium, color = GamingColors.TextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Instant Mode", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = macro.instantMode,
                                    onCheckedChange = { onMacroChange(macro.copy(instantMode = it)) }
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(SpeedEngine.PRESET_SPEEDS) { _, spd ->
                                FilterChip(
                                    selected = kotlin.math.abs(macro.defaultSpeed - spd) < 0.05f,
                                    onClick = { onMacroChange(macro.copy(defaultSpeed = spd)) },
                                    label = { Text("${spd}x") }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showTouchPad = !showTouchPad },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showTouchPad) "Ẩn Khung Ghi & Chọn Tọa Độ Trực Quan" else "Mở Khung Ghi & Chọn Tọa Độ Trực Quan")
                        }
                    }
                }
            }

            // Interactive Visual Touch Recorder / Calibrator Pad
            if (showTouchPad) {
                item {
                    TouchCalibrationPad(
                        actions = macro.actions,
                        onRecordTap = { nx, ny ->
                            val next = MacroAction(
                                type = ActionType.TAP,
                                x = nx,
                                y = ny,
                                durationMs = 45L,
                                waitAfterMs = 150L,
                                orderIndex = macro.actions.size,
                                label = "Chạm trực quan"
                            )
                            onMacroChange(macro.copy(actions = macro.actions + next))
                        },
                        onRecordLongPress = { nx, ny ->
                            val next = MacroAction(
                                type = ActionType.LONG_PRESS,
                                x = nx,
                                y = ny,
                                durationMs = 700L,
                                waitAfterMs = 150L,
                                orderIndex = macro.actions.size,
                                label = "Nhấn giữ trực quan"
                            )
                            onMacroChange(macro.copy(actions = macro.actions + next))
                        },
                        onRecordSwipe = { sx, sy, ex, ey ->
                            val next = MacroAction(
                                type = ActionType.SWIPE,
                                x = sx,
                                y = sy,
                                endX = ex,
                                endY = ey,
                                durationMs = 300L,
                                waitAfterMs = 150L,
                                orderIndex = macro.actions.size,
                                label = "Vuốt trực quan"
                            )
                            onMacroChange(macro.copy(actions = macro.actions + next))
                        }
                    )
                }
            }

            // Timeline Header
            item {
                Text(
                    text = "TIMELINE (${macro.actions.size} HÀNH ĐỘNG)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GamingColors.TextSecondary
                )
            }

            // Timeline Items
            itemsIndexed(macro.actions, key = { _, item -> item.id }) { index, action ->
                TimelineActionCard(
                    index = index,
                    totalCount = macro.actions.size,
                    action = action,
                    pixelPreview = CoordinateConverter.formatPixelPreview(context, action.x, action.y),
                    onEdit = { editingActionIndex = index },
                    onDuplicate = {
                        val copy = action.copy(id = java.util.UUID.randomUUID().toString())
                        val mutable = macro.actions.toMutableList()
                        mutable.add(index + 1, copy)
                        onMacroChange(macro.copy(actions = reindex(mutable)))
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val mutable = macro.actions.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            onMacroChange(macro.copy(actions = reindex(mutable)))
                        }
                    },
                    onMoveDown = {
                        if (index < macro.actions.lastIndex) {
                            val mutable = macro.actions.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            onMacroChange(macro.copy(actions = reindex(mutable)))
                        }
                    },
                    onDelete = {
                        val mutable = macro.actions.toMutableList()
                        mutable.removeAt(index)
                        onMacroChange(macro.copy(actions = reindex(mutable)))
                    }
                )
            }
        }

        // Bottom Add-Action Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(GamingColors.CarbonSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AddActionButton("+ TAP") {
                    val a = MacroAction(type = ActionType.TAP, orderIndex = macro.actions.size)
                    onMacroChange(macro.copy(actions = macro.actions + a))
                }
            }
            item {
                AddActionButton("+ LONG PRESS") {
                    val a = MacroAction(type = ActionType.LONG_PRESS, durationMs = 800L, orderIndex = macro.actions.size)
                    onMacroChange(macro.copy(actions = macro.actions + a))
                }
            }
            item {
                AddActionButton("+ SWIPE") {
                    val a = MacroAction(
                        type = ActionType.SWIPE,
                        x = 0.7f, y = 0.6f, endX = 0.3f, endY = 0.6f,
                        durationMs = 300L, orderIndex = macro.actions.size
                    )
                    onMacroChange(macro.copy(actions = macro.actions + a))
                }
            }
            item {
                AddActionButton("+ WAIT") {
                    val a = MacroAction(type = ActionType.WAIT, durationMs = 500L, waitAfterMs = 0L, orderIndex = macro.actions.size)
                    onMacroChange(macro.copy(actions = macro.actions + a))
                }
            }
            item {
                AddActionButton("+ REPEAT") {
                    val a = MacroAction(type = ActionType.REPEAT, repeatCount = 2, durationMs = 0L, waitAfterMs = 0L, orderIndex = macro.actions.size)
                    onMacroChange(macro.copy(actions = macro.actions + a))
                }
            }
        }
    }

    val editIdx = editingActionIndex
    if (editIdx != null && editIdx in macro.actions.indices) {
        ActionEditDialog(
            action = macro.actions[editIdx],
            onDismiss = { editingActionIndex = null },
            onConfirm = { updated ->
                val mutable = macro.actions.toMutableList()
                mutable[editIdx] = updated
                onMacroChange(macro.copy(actions = reindex(mutable)))
                editingActionIndex = null
            }
        )
    }
}

@Composable
private fun TouchCalibrationPad(
    actions: List<MacroAction>,
    onRecordTap: (Float, Float) -> Unit,
    onRecordLongPress: (Float, Float) -> Unit,
    onRecordSwipe: (Float, Float, Float, Float) -> Unit
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GamingColors.CarbonSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "KHUNG GHI & HIỆU CHỈNH TỌA ĐỘ TRỰC QUAN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GamingColors.CyberBlue
        )
        Text(
            text = "Chạm = +TAP • Nhấn giữ = +LONG PRESS • Vuốt = +SWIPE (tọa độ chuẩn hóa 0.0–1.0)",
            style = MaterialTheme.typography.bodySmall,
            color = GamingColors.TextSecondary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(GamingColors.ObsidianBg, RoundedCornerShape(10.dp))
                .border(1.dp, GamingColors.CyberBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val nx = (offset.x / size.width).coerceIn(0f, 1f)
                            val ny = (offset.y / size.height).coerceIn(0f, 1f)
                            onRecordTap(nx, ny)
                        },
                        onLongPress = { offset ->
                            val nx = (offset.x / size.width).coerceIn(0f, 1f)
                            val ny = (offset.y / size.height).coerceIn(0f, 1f)
                            onRecordLongPress(nx, ny)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            dragStart = start
                            dragEnd = start
                        },
                        onDrag = { change, _ ->
                            dragEnd = change.position
                        },
                        onDragEnd = {
                            val s = dragStart
                            val e = dragEnd
                            if (s != null && e != null) {
                                onRecordSwipe(
                                    (s.x / size.width).coerceIn(0f, 1f),
                                    (s.y / size.height).coerceIn(0f, 1f),
                                    (e.x / size.width).coerceIn(0f, 1f),
                                    (e.y / size.height).coerceIn(0f, 1f)
                                )
                            }
                            dragStart = null
                            dragEnd = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                actions.forEach { act ->
                    if (act.type == ActionType.TAP || act.type == ActionType.LONG_PRESS) {
                        drawCircle(
                            color = if (act.type == ActionType.TAP) GamingColors.CrimsonRed else GamingColors.AmberWarn,
                            radius = 14f,
                            center = Offset(act.x * size.width, act.y * size.height)
                        )
                    } else if (act.type == ActionType.SWIPE) {
                        val s = Offset(act.x * size.width, act.y * size.height)
                        val e = Offset(act.endX * size.width, act.endY * size.height)
                        drawLine(color = GamingColors.CyberBlue, start = s, end = e, strokeWidth = 5f)
                        drawCircle(color = GamingColors.CyberBlue, radius = 10f, center = s)
                    }
                }
                val ds = dragStart
                val de = dragEnd
                if (ds != null && de != null) {
                    drawLine(color = Color.White, start = ds, end = de, strokeWidth = 4f)
                }
            }
        }
    }
}

@Composable
private fun TimelineActionCard(
    index: Int,
    totalCount: Int,
    action: MacroAction,
    pixelPreview: String,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (action.type) {
        ActionType.TAP -> GamingColors.CrimsonRed
        ActionType.LONG_PRESS -> GamingColors.AmberWarn
        ActionType.SWIPE -> GamingColors.CyberBlue
        ActionType.WAIT -> GamingColors.TextSecondary
        ActionType.REPEAT -> GamingColors.NeonGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
        border = BorderStroke(1.dp, GamingColors.BorderSubtle),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${index + 1} ${action.type.name}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                    if (action.label.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("• ${action.label}", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                    }
                }
                if (action.groupId != null) {
                    Text("Nhóm: ${action.groupId}", style = MaterialTheme.typography.labelSmall, color = GamingColors.NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val details = when (action.type) {
                ActionType.TAP, ActionType.LONG_PRESS ->
                    "Tọa độ: (${String.format("%.3f", action.x)}, ${String.format("%.3f", action.y)}) $pixelPreview • Giữ: ${action.durationMs}ms • Chờ sau: ${action.waitAfterMs}ms"
                ActionType.SWIPE ->
                    "Từ (${String.format("%.2f", action.x)}, ${String.format("%.2f", action.y)}) → (${String.format("%.2f", action.endX)}, ${String.format("%.2f", action.endY)}) • ${action.durationMs}ms"
                ActionType.WAIT ->
                    "Chờ ${action.durationMs} ms"
                ActionType.REPEAT ->
                    "Lặp ${action.repeatCount} lần"
            }

            Text(details, style = MaterialTheme.typography.bodySmall, color = GamingColors.TextPrimary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Lên", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Xuống", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Nhân bản", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = GamingColors.CrimsonRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AddActionButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ActionEditDialog(
    action: MacroAction,
    onDismiss: () -> Unit,
    onConfirm: (MacroAction) -> Unit
) {
    var xStr by remember { mutableStateOf(action.x.toString()) }
    var yStr by remember { mutableStateOf(action.y.toString()) }
    var endXStr by remember { mutableStateOf(action.endX.toString()) }
    var endYStr by remember { mutableStateOf(action.endY.toString()) }
    var durationStr by remember { mutableStateOf(action.durationMs.toString()) }
    var waitStr by remember { mutableStateOf(action.waitAfterMs.toString()) }
    var repeatStr by remember { mutableStateOf(action.repeatCount.toString()) }
    var groupStr by remember { mutableStateOf(action.groupId ?: "") }
    var labelStr by remember { mutableStateOf(action.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa ${action.type.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = labelStr, onValueChange = { labelStr = it }, label = { Text("Ghi chú") }, singleLine = true)
                if (action.type != ActionType.WAIT && action.type != ActionType.REPEAT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = xStr, onValueChange = { xStr = it }, label = { Text("X (0..1)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = yStr, onValueChange = { yStr = it }, label = { Text("Y (0..1)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                if (action.type == ActionType.SWIPE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = endXStr, onValueChange = { endXStr = it }, label = { Text("End X") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = endYStr, onValueChange = { endYStr = it }, label = { Text("End Y") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                if (action.type != ActionType.REPEAT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = durationStr, onValueChange = { durationStr = it }, label = { Text("Thời lượng (ms)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = waitStr, onValueChange = { waitStr = it }, label = { Text("Chờ sau (ms)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = groupStr, onValueChange = { groupStr = it }, label = { Text("Group ID (tuỳ chọn)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = repeatStr, onValueChange = { repeatStr = it }, label = { Text("Số lần lặp") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        action.copy(
                            x = (xStr.toFloatOrNull() ?: action.x).coerceIn(0f, 1f),
                            y = (yStr.toFloatOrNull() ?: action.y).coerceIn(0f, 1f),
                            endX = (endXStr.toFloatOrNull() ?: action.endX).coerceIn(0f, 1f),
                            endY = (endYStr.toFloatOrNull() ?: action.endY).coerceIn(0f, 1f),
                            durationMs = (durationStr.toLongOrNull() ?: action.durationMs).coerceAtLeast(0L),
                            waitAfterMs = (waitStr.toLongOrNull() ?: action.waitAfterMs).coerceAtLeast(0L),
                            repeatCount = (repeatStr.toIntOrNull() ?: action.repeatCount).coerceAtLeast(1),
                            groupId = groupStr.trim().ifBlank { null },
                            label = labelStr.trim()
                        )
                    )
                }
            ) {
                Text("Áp dụng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

private fun reindex(list: List<MacroAction>): List<MacroAction> {
    return list.mapIndexed { index, action -> action.copy(orderIndex = index) }
}
