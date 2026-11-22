package com.macro.app.biome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BiomeClassifierTest {
    private fun feature(vararg activeBins: Int): BiomeFeatureVector {
        val values = MutableList(BiomeFeatureVector.FEATURE_SIZE) { 0f }
        activeBins.forEach { values[it] = 1f }
        return BiomeFeatureVector(values)
    }

    @Test
    fun exactConfirmedSampleIsRecognizedButStillRequiresReview() {
        val forest = feature(0, 8, 16)
        val prediction = BiomeClassifier(0.8f).predict(
            forest,
            listOf(ConfirmedBiomeSample(BiomeId.FOREST, forest))
        )

        assertEquals(BiomeId.FOREST, prediction.biome)
        assertEquals(1f, prediction.similarity, 0.0001f)
        assertTrue(prediction.requiresPlayerReview)
    }

    @Test
    fun noConfirmedSamplesReturnsUnknown() {
        val prediction = BiomeClassifier().predict(feature(0), emptyList())
        assertNull(prediction.biome)
        assertEquals(0, prediction.samplesCompared)
        assertTrue(prediction.requiresPlayerReview)
    }

    @Test
    fun dissimilarSampleIsNotForcedIntoBiomeLabel() {
        val prediction = BiomeClassifier(0.95f).predict(
            feature(0),
            listOf(ConfirmedBiomeSample(BiomeId.VOLCANO, feature(23)))
        )
        assertNull(prediction.biome)
        assertTrue(prediction.requiresPlayerReview)
    }
}
