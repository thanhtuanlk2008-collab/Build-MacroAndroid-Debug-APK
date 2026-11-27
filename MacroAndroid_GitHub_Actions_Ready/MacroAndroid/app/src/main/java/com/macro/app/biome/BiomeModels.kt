package com.macro.app.biome

/** Matrix biomes. Hidden biomes remain review-only until confirmed in Matrix runs. */
enum class BiomeId {
    FOREST, GLACIER, RELICS, MECHANICAL_RUINS,
    KNIGHT_KINGDOM, DUNGEON, HALLOWEEN, CHISELTOWN,
    SPACESHIP, VOLCANO, NEO_ISLE,
    CAVE, GRAVE, SWAMP
}

data class BiomeFeatureVector(val values: List<Float>) {
    init {
        require(values.size == FEATURE_SIZE) { "Expected $FEATURE_SIZE feature values" }
        require(values.all { it.isFinite() && it in 0f..1f }) {
            "Feature values must be finite and normalized"
        }
    }

    fun encode(): String = values.joinToString(",") { it.toString() }

    companion object {
        const val FEATURE_SIZE = 24

        fun decode(encoded: String): BiomeFeatureVector? = runCatching {
            BiomeFeatureVector(encoded.split(',').map(String::toFloat))
        }.getOrNull()
    }
}

data class ConfirmedBiomeSample(
    val biome: BiomeId,
    val feature: BiomeFeatureVector
)

data class BiomePrediction(
    val biome: BiomeId?,
    /** Similarity score, not a calibrated probability. */
    val similarity: Float,
    val requiresPlayerReview: Boolean,
    val samplesCompared: Int
)
