package com.macro.app.biome

import com.macro.app.data.BiomeFeedbackDao
import com.macro.app.data.BiomeFeedbackEntity
import java.util.UUID

/**
 * Local-only feedback store. The caller must ask the player before calling confirm().
 * No screenshots or player identifiers are persisted; only a compact feature vector.
 */
class BiomeFeedbackRepository(
    private val dao: BiomeFeedbackDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun recordPrediction(
        feature: BiomeFeatureVector,
        prediction: BiomePrediction
    ): String {
        val id = UUID.randomUUID().toString()
        dao.insert(
            BiomeFeedbackEntity(
                id = id,
                predictedBiome = prediction.biome?.name,
                confirmedBiome = null,
                featureVector = feature.encode(),
                similarity = prediction.similarity,
                status = "PENDING",
                createdAt = clock()
            )
        )
        return id
    }

    /** Returns false if the sample is missing or is no longer pending. */
    suspend fun confirm(id: String, playerSelectedBiome: BiomeId): Boolean =
        dao.confirmPending(id, playerSelectedBiome.name) == 1

    suspend fun discard(id: String): Boolean = dao.discardPending(id) == 1

    suspend fun pendingCount(): Int = dao.getPending().size

    suspend fun loadConfirmedSamples(): List<ConfirmedBiomeSample> =
        dao.getConfirmed().mapNotNull { row ->
            val biome = runCatching { BiomeId.valueOf(row.confirmedBiome!!) }.getOrNull()
            val feature = BiomeFeatureVector.decode(row.featureVector)
            if (biome == null || feature == null) null
            else ConfirmedBiomeSample(biome, feature)
        }
}
