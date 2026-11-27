package com.macro.app.biome

import android.graphics.Bitmap

/**
 * First Android integration point for biome analysis.
 * Predictions are review-required; the caller should present the result for player correction.
 */
class BiomeRecognitionEngine(
    private val extractor: BiomeFeatureExtractor,
    private val classifier: BiomeClassifier,
    private val feedback: BiomeFeedbackRepository
) {
    suspend fun analyze(bitmap: Bitmap): Pair<BiomePrediction, String> {
        val feature = extractor.extract(bitmap)
        val prediction = classifier.predict(feature, feedback.loadConfirmedSamples())
        val feedbackId = feedback.recordPrediction(feature, prediction)
        return prediction to feedbackId
    }

    suspend fun confirm(feedbackId: String, selectedBiome: BiomeId): Boolean =
        feedback.confirm(feedbackId, selectedBiome)

    suspend fun discard(feedbackId: String): Boolean = feedback.discard(feedbackId)
}
