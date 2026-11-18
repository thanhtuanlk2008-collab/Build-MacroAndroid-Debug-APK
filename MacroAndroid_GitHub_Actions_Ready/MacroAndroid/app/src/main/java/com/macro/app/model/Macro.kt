package com.macro.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RepeatMode {
    ONCE,
    FIVE_TIMES,
    TEN_TIMES,
    CUSTOM,
    INFINITE
}

@Serializable
enum class MacroState {
    IDLE,
    RECORDING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

@Serializable
enum class GameCategory(val displayName: String) {
    GENSHIN_IMPACT("Genshin Impact"),
    WUTHERING_WAVES("Wuthering Waves"),
    LIEN_QUAN("Liên Quân Mobile"),
    OTHER("Game Khác")
}

@Serializable
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val gameCategory: GameCategory = GameCategory.OTHER,
    val folderName: String = "Mặc định",
    val defaultSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    val customRepeatCount: Int = 1,
    val instantMode: Boolean = false,
    val actions: List<MacroAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
