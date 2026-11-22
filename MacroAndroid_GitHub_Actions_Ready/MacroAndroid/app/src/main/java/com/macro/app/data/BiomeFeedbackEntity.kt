package com.macro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biome_feedback")
data class BiomeFeedbackEntity(
    @PrimaryKey val id: String,
    val predictedBiome: String?,
    val confirmedBiome: String?,
    val featureVector: String,
    val similarity: Float?,
    /** PENDING or CONFIRMED. Predictions never become training data automatically. */
    val status: String,
    val createdAt: Long
)
