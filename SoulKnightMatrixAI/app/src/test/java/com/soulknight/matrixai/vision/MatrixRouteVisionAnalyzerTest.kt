package com.soulknight.matrixai.vision

import com.soulknight.matrixai.matrix.MatrixBiome
import com.soulknight.matrixai.matrix.RouteAction
import com.soulknight.matrixai.matrix.MatrixRouteCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MatrixRouteVisionAnalyzerTest {
    private fun frameWithPatch(patch: IntArray): FrameSnapshot {
        val width = 8
        val height = 8
        val pixels = IntArray(width * height) { 0xff101010.toInt() }
        // Put a 2x2 patch at (3, 2).
        for (y in 0 until 2) for (x in 0 until 2) {
            pixels[(y + 2) * width + (x + 3)] = patch[y * 2 + x]
        }
        return FrameSnapshot(width, height, 1L, pixels)
    }

    @Test
    fun matcherFindsExactReferenceCrop() {
        val patch = intArrayOf(
            0xffff0000.toInt(), 0xff00ff00.toInt(),
            0xff0000ff.toInt(), 0xffffffff.toInt()
        )
        val frame = frameWithPatch(patch)
        val template = ImageTemplate("portal", 2, 2, patch)
        val match = TemplateMatcher(pixelSampleStride = 1).findBestMatch(
            frame, template, ScreenRegion(0, 0, 8, 8), searchStep = 1
        )

        assertNotNull(match)
        assertEquals(3, match!!.left)
        assertEquals(2, match.top)
        assertEquals(1f, match.confidence, 0.0001f)
    }

    @Test
    fun coordinatorPrioritizesPortalWhenBiomeAndEntranceTemplatesMatch() {
        val biomePatch = intArrayOf(
            0xffff0000.toInt(), 0xff00ff00.toInt(),
            0xff0000ff.toInt(), 0xffffffff.toInt()
        )
        val portalPatch = intArrayOf(
            0xffffff00.toInt(), 0xffff00ff.toInt(),
            0xff00ffff.toInt(), 0xffaaaaaa.toInt()
        )
        val pixels = IntArray(12 * 8) { 0xff101010.toInt() }
        for (y in 0 until 2) for (x in 0 until 2) {
            pixels[y * 12 + x] = biomePatch[y * 2 + x]
            pixels[(y + 3) * 12 + (x + 8)] = portalPatch[y * 2 + x]
        }
        val frame = FrameSnapshot(12, 8, 1L, pixels)
        val coordinator = MatrixRouteCoordinator(
            visionAnalyzer = MatrixRouteVisionAnalyzer(TemplateMatcher(1))
        )

        val result = coordinator.evaluateAfterBoss(
            floorNumber = 4,
            roomNumber = 5,
            bossDefeated = true,
            frame = frame,
            biomeTemplates = mapOf(MatrixBiome.OTHER to ImageTemplate("other-biome", 2, 2, biomePatch)),
            sirVerdantEntranceTemplate = ImageTemplate("sir-verdant-entrance", 2, 2, portalPatch),
            biomeRegion = ScreenRegion(0, 0, 4, 4),
            routeRegion = ScreenRegion(6, 2, 12, 8)
        )

        assertEquals(MatrixBiome.OTHER, result.vision.biome)
        assertEquals(RouteAction.ENTER_SIR_VERDANT, result.decision.action)
        assertNotNull(result.vision.sirVerdantMatch)
    }

    @Test
    fun glacierTemplateOverridesDetectedSirVerdantEntrance() {
        val biomePatch = intArrayOf(
            0xffff0000.toInt(), 0xff00ff00.toInt(),
            0xff0000ff.toInt(), 0xffffffff.toInt()
        )
        val portalPatch = intArrayOf(
            0xffffff00.toInt(), 0xffff00ff.toInt(),
            0xff00ffff.toInt(), 0xffaaaaaa.toInt()
        )
        val pixels = IntArray(12 * 8) { 0xff101010.toInt() }
        for (y in 0 until 2) for (x in 0 until 2) {
            pixels[y * 12 + x] = biomePatch[y * 2 + x]
            pixels[(y + 3) * 12 + (x + 8)] = portalPatch[y * 2 + x]
        }
        val result = MatrixRouteCoordinator(
            visionAnalyzer = MatrixRouteVisionAnalyzer(TemplateMatcher(1))
        ).evaluateAfterBoss(
            floorNumber = 4,
            roomNumber = 5,
            bossDefeated = true,
            frame = FrameSnapshot(12, 8, 1L, pixels),
            biomeTemplates = mapOf(MatrixBiome.GLACIER to ImageTemplate("glacier", 2, 2, biomePatch)),
            sirVerdantEntranceTemplate = ImageTemplate("sir-verdant-entrance", 2, 2, portalPatch),
            biomeRegion = ScreenRegion(0, 0, 4, 4),
            routeRegion = ScreenRegion(6, 2, 12, 8)
        )
        assertEquals(RouteAction.SKIP_SPECIAL_ROOM, result.decision.action)
    }
}
