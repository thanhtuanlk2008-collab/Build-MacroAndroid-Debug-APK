package com.macro.app.biome

/**
 * Conservative nearest-histogram baseline.
 *
 * A single known biome is not enough to classify a new frame: otherwise every
 * sufficiently similar room can be forced into that one label. A prediction is
 * emitted only when at least two biome classes are represented and the best
 * class wins by a clear margin. All predictions still require player review.
 */
class BiomeClassifier(
    private val minimumSimilarity: Float = 0.82f,
    private val minimumClassMargin: Float = 0.05f
) {
    init {
        require(minimumSimilarity.isFinite() && minimumSimilarity in 0f..1f)
        require(minimumClassMargin.isFinite() && minimumClassMargin in 0f..1f)
    }

    fun predict(
        query: BiomeFeatureVector,
        confirmedSamples: List<ConfirmedBiomeSample>
    ): BiomePrediction {
        if (confirmedSamples.isEmpty()) {
            return BiomePrediction(null, 0f, true, 0)
        }

        val bestByBiome = confirmedSamples
            .groupBy { it.biome }
            .mapValues { (_, samples) ->
                samples.maxOf { similarity(query, it.feature) }
            }

        // Do not let a one-class training set label every vaguely similar room.
        if (bestByBiome.size < MINIMUM_BIOME_CLASSES) {
            val bestSimilarity = bestByBiome.values.maxOrNull() ?: 0f
            return BiomePrediction(null, bestSimilarity, true, confirmedSamples.size)
        }

        val ranked = bestByBiome.entries.sortedByDescending { it.value }
        val best = ranked.first()
        val runnerUp = ranked[1]
        val margin = best.value - runnerUp.value
        val accepted = best.value >= minimumSimilarity && margin >= minimumClassMargin

        return BiomePrediction(
            biome = if (accepted) best.key else null,
            similarity = best.value,
            requiresPlayerReview = true,
            samplesCompared = confirmedSamples.size
        )
    }

    private fun similarity(a: BiomeFeatureVector, b: BiomeFeatureVector): Float {
        val meanAbsoluteDifference = a.values.indices
            .map { kotlin.math.abs(a.values[it] - b.values[it]) }
            .average()
            .toFloat()
        return (1f - meanAbsoluteDifference).coerceIn(0f, 1f)
    }

    companion object {
        private const val MINIMUM_BIOME_CLASSES = 2
    }
}
