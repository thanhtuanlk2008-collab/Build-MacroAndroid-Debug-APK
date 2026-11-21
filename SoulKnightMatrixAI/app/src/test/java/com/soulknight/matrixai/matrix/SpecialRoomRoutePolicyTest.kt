package com.soulknight.matrixai.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SpecialRoomRoutePolicyTest {
    private val policy = SpecialRoomRoutePolicy()

    private fun context(
        biome: MatrixBiome = MatrixBiome.OTHER,
        bossDefeated: Boolean = true,
        roomNumber: Int = 5,
        scanComplete: Boolean = true,
        routes: List<DetectedRoute> = emptyList()
    ) = BossRouteContext(
        floorNumber = 3,
        roomNumber = roomNumber,
        bossDefeated = bossDefeated,
        biome = biome,
        routeScanComplete = scanComplete,
        detectedRoutes = routes
    )

    @Test
    fun prefersSirVerdantWhenDetectedInEligibleBiome() {
        val route = DetectedRoute(RouteDestination.SIR_VERDANT, 0.93f)
        val result = policy.decide(context(routes = listOf(route)))

        assertEquals(RouteAction.ENTER_SIR_VERDANT, result.action)
        assertNotNull(result.selectedRoute)
    }

    @Test
    fun glacierAndRelicsAlwaysSkipSpecialRoom() {
        listOf(MatrixBiome.GLACIER, MatrixBiome.RELICS).forEach { biome ->
            assertEquals(
                RouteAction.SKIP_SPECIAL_ROOM,
                policy.decide(context(biome = biome)).action
            )
        }
    }

    @Test
    fun continuesNormallyWhenCompleteScanDoesNotFindSirVerdant() {
        val result = policy.decide(
            context(routes = listOf(DetectedRoute(RouteDestination.NORMAL_NEXT_STAGE, 0.99f)))
        )
        assertEquals(RouteAction.CONTINUE_NORMAL, result.action)
    }

    @Test
    fun waitsUntilRouteScanIsComplete() {
        assertEquals(
            RouteAction.WAIT_FOR_ROUTE_SCAN,
            policy.decide(context(scanComplete = false)).action
        )
    }

    @Test
    fun waitsForBiomeInsteadOfGuessing() {
        assertEquals(
            RouteAction.WAIT_FOR_BIOME,
            policy.decide(context(biome = MatrixBiome.UNKNOWN)).action
        )
    }

    @Test
    fun ignoresLowConfidenceSirVerdantDetection() {
        val result = policy.decide(
            context(routes = listOf(DetectedRoute(RouteDestination.SIR_VERDANT, 0.55f)))
        )
        assertEquals(RouteAction.CONTINUE_NORMAL, result.action)
    }

    @Test
    fun doesNotRunBeforeBossDefeatOrOutsideRoomFive() {
        assertEquals(
            RouteAction.NOT_APPLICABLE,
            policy.decide(context(bossDefeated = false)).action
        )
        assertEquals(
            RouteAction.NOT_APPLICABLE,
            policy.decide(context(roomNumber = 4)).action
        )
    }
}
