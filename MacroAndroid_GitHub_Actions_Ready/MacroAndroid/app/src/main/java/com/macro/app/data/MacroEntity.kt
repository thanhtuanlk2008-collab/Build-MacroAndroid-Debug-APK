package com.macro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.model.MacroAction
import com.macro.app.model.RepeatMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val gameCategory: String,
    val folderName: String,
    val defaultSpeed: Float,
    val repeatMode: String,
    val customRepeatCount: Int,
    val instantMode: Boolean,
    val actionsJson: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(json: Json = defaultJson): Macro {
        val actions = runCatching {
            json.decodeFromString<List<MacroAction>>(actionsJson)
        }.getOrDefault(emptyList())

        return Macro(
            id = id,
            name = name,
            gameCategory = runCatching { GameCategory.valueOf(gameCategory) }.getOrDefault(GameCategory.OTHER),
            folderName = folderName,
            defaultSpeed = defaultSpeed,
            repeatMode = runCatching { RepeatMode.valueOf(repeatMode) }.getOrDefault(RepeatMode.ONCE),
            customRepeatCount = customRepeatCount,
            instantMode = instantMode,
            actions = actions,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        private val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromDomain(macro: Macro, json: Json = defaultJson): MacroEntity {
            return MacroEntity(
                id = macro.id,
                name = macro.name,
                gameCategory = macro.gameCategory.name,
                folderName = macro.folderName,
                defaultSpeed = macro.defaultSpeed,
                repeatMode = macro.repeatMode.name,
                customRepeatCount = macro.customRepeatCount,
                instantMode = macro.instantMode,
                actionsJson = json.encodeToString(macro.actions),
                createdAt = macro.createdAt,
                updatedAt = macro.updatedAt
            )
        }
    }
}
