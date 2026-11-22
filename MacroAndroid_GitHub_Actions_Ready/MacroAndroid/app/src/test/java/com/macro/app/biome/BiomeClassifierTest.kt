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
    fun exactConfirmedSampleIsRecognizedWhenCompetingBiomeExists() {
        val forest = feature(0, 8, 16)
        val glacier = feature(1, 9, 17)
        val prediction = BiomeClassifier(0.8f).predict(
            forest,
            listOf(
                ConfirmedBiomeSample(BiomeId.FOREST, forest),
                ConfirmedBiomeSample(BiomeId.GLACIER, glacier)
            )
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
    fun oneKnownBiomeIsNotUsedToForceClassification() {
        val forest = feature(0, 8, 16)
        val prediction = BiomeClassifier().predict(
            forest,
            listOf(ConfirmedBiomeSample(BiomeId.FOREST, forest))
        )

        assertNull(prediction.biome)
        assertEquals(1, prediction.samplesCompared)
        assertTrue(prediction.requiresPlayerReview)
    }

    @Test
    fun ambiguousMatchBetweenBiomeClassesReturnsUnknown() {
        val sharedAppearance = feature(0, 8, 16)
        val prediction = BiomeClassifier(minimumClassMargin = 0.05f).predict(
            sharedAppearance,
            listOf(
                ConfirmedBiomeSample(BiomeId.FOREST, sharedAppearance),
                ConfirmedBiomeSample(BiomeId.RELICS, sharedAppearance)
            )
        )

        assertNull(prediction.biome)
        assertTrue(prediction.requiresPlayerReview)
    }

    @Test
    fun dissimilarSampleIsNotForcedIntoBiomeLabel() {
        val prediction = BiomeClassifier(0.95f).predict(
            feature(0),
            listOf(
                ConfirmedBiomeSample(BiomeId.VOLCANO, feature(23)),
                ConfirmedBiomeSample(BiomeId.FOREST, feature(22))
            )
        )
        assertNull(prediction.biome)
        assertTrue(prediction.requiresPlayerReview)
    }
}
