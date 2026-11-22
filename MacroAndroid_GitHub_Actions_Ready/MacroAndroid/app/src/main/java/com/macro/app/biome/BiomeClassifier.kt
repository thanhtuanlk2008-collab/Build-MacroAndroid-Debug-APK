package com.macro.app.biome

/**
 * Nearest-histogram baseline. Only player-confirmed gameplay samples are accepted.
 * Wiki artwork is deliberately excluded: it is reference material, not gameplay ground truth.
 */
class BiomeClassifier(
    private val minimumSimilarity: Float = 0.82f
) {
    init {
        require(minimumSimilarity.isFinite() && minimumSimilarity in 0f..1f)
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
        val best = bestByBiome.maxByOrNull { it.value }
            ?: return BiomePrediction(null, 0f, true, 0)

        val accepted = best.value >= minimumSimilarity
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
}
