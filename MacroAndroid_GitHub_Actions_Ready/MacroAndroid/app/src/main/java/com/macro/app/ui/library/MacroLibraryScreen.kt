package com.macro.app.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.ui.theme.GamingColors

@Composable
fun MacroLibraryScreen(
    macros: List<Macro>,
    onCreateMacro: () -> Unit,
    onPlayMacro: (Macro) -> Unit,
    onEditMacro: (Macro) -> Unit,
    onDuplicateMacro: (Macro) -> Unit,
    onDeleteMacro: (String) -> Unit,
    onExportMacroJson: (Macro) -> String,
    onExportAllJson: () -> String,
    onImportJson: (String, (Result<Int>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<GameCategory?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJsonPreview by remember { mutableStateOf<String?>(null) }

    val filtered = remember(macros, searchQuery, selectedCategory) {
        macros.filter { macro ->
            val matchesQuery = searchQuery.isBlank() ||
                macro.name.contains(searchQuery, ignoreCase = true) ||
                macro.folderName.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || macro.gameCategory == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingColors.ObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THƯ VIỆN MACRO (${macros.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GamingColors.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nhập")
                }
                OutlinedButton(
                    onClick = {
                        val json = onExportAllJson()
                        copyToClipboard(context, "MacroEngine_All", json)
                        exportedJsonPreview = json
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xuất tất cả")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Tìm theo tên Macro hoặc thư mục...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Tất cả") }
                )
            }
            items(GameCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.displayName) }
                )
            }
        }

        Button(
            onClick = onCreateMacro,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tạo Macro Mới")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { macro ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                    border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = macro.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GamingColors.TextPrimary
                                )
                                Text(
                                    text = "${macro.gameCategory.displayName} • ${macro.actions.size} bước • ${macro.defaultSpeed}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GamingColors.TextSecondary
                                )
                            }
                            Button(
                                onClick = { onPlayMacro(macro) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chạy")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onEditMacro(macro) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Sửa", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDuplicateMacro(macro) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Sao chép", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val json = onExportMacroJson(macro)
                                    copyToClipboard(context, macro.name, json)
                                    exportedJsonPreview = json
                                    Toast.makeText(context, "Đã sao chép JSON vào Clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Xuất JSON", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteMacro(macro.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = GamingColors.CrimsonRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Nhập Macro từ JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Dán nội dung JSON tại đây") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportJson(jsonInput) { result ->
                            result.onSuccess { count ->
                                Toast.makeText(context, "Đã nhập thành công $count Macro", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            }.onFailure { err ->
                                Toast.makeText(context, "JSON không hợp lệ: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Nhập JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Đóng") }
            }
        )
    }

    exportedJsonPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            title = { Text("JSON đã sao chép vào Clipboard") },
            text = {
                OutlinedTextField(
                    value = preview,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { exportedJsonPreview = null }) { Text("Xong") }
            }
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
