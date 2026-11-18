package com.macro.app.data

import com.macro.app.model.ActionType
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.model.MacroAction
import com.macro.app.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MacroRepository(private val dao: MacroDao) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeAllMacros(): Flow<List<Macro>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain(json) }
        }
    }

    suspend fun getAllMacrosOnce(): List<Macro> {
        return dao.getAllOnce().map { it.toDomain(json) }
    }

    suspend fun getMacroById(id: String): Macro? {
        return dao.getById(id)?.toDomain(json)
    }

    suspend fun saveMacro(macro: Macro) {
        val updated = macro.copy(updatedAt = System.currentTimeMillis())
        dao.upsert(MacroEntity.fromDomain(updated, json))
    }

    suspend fun duplicateMacro(macro: Macro): Macro {
        val now = System.currentTimeMillis()
        val copy = macro.copy(
            id = UUID.randomUUID().toString(),
            name = "${macro.name} (Bản sao)",
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(MacroEntity.fromDomain(copy, json))
        return copy
    }

    suspend fun deleteMacro(id: String) {
        dao.deleteById(id)
    }

    fun exportMacroToJson(macro: Macro): String {
        return json.encodeToString(macro)
    }

    fun exportAllToJson(macros: List<Macro>): String {
        return json.encodeToString(macros)
    }

    suspend fun importFromJson(rawJson: String): Result<Int> {
        return runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("[")) {
                val list = json.decodeFromString<List<Macro>>(trimmed)
                val normalized = list.map { m ->
                    m.copy(
                        id = UUID.randomUUID().toString(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                dao.upsertAll(normalized.map { MacroEntity.fromDomain(it, json) })
                normalized.size
            } else {
                val single = json.decodeFromString<Macro>(trimmed)
                val normalized = single.copy(
                    id = UUID.randomUUID().toString(),
                    updatedAt = System.currentTimeMillis()
                )
                dao.upsert(MacroEntity.fromDomain(normalized, json))
                1
            }
        }
    }

    suspend fun seedStarterMacroIfEmpty() {
        if (dao.count() > 0) return
        val starter = Macro(
            name = "Combo Attack Mẫu",
            gameCategory = GameCategory.LIEN_QUAN,
            folderName = "Mặc định",
            defaultSpeed = 1.0f,
            repeatMode = RepeatMode.ONCE,
            actions = listOf(
                MacroAction(type = ActionType.TAP, x = 0.82f, y = 0.78f, durationMs = 45L, waitAfterMs = 200L, orderIndex = 0, label = "Đánh thường"),
                MacroAction(type = ActionType.WAIT, durationMs = 300L, waitAfterMs = 0L, orderIndex = 1, label = "Chờ nhịp"),
                MacroAction(type = ActionType.LONG_PRESS, x = 0.75f, y = 0.65f, durationMs = 600L, waitAfterMs = 150L, orderIndex = 2, label = "Tụ chiêu"),
                MacroAction(type = ActionType.SWIPE, x = 0.25f, y = 0.72f, endX = 0.35f, endY = 0.60f, durationMs = 250L, waitAfterMs = 100L, orderIndex = 3, label = "Lướt hướng")
            )
        )
        dao.upsert(MacroEntity.fromDomain(starter, json))
    }
}
