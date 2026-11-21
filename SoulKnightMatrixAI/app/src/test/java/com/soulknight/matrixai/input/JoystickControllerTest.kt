package com.soulknight.matrixai.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoystickControllerTest {
    @Test fun rejectsUnsafeOutOfScreenEndpoint() {
        // A controller without an enabled AccessibilityService cannot dispatch anything.
        val controller = InputController { null }
        val joystick = JoystickController(controller)
        var result: InputResult? = null
        val id = joystick.move(
            center = InputTarget(20f, 20f),
            directionX = -1f,
            directionY = 0f,
            radiusPixels = 60f,
            durationMillis = 250L,
            screenWidth = 100,
            screenHeight = 100
        ) { result = it }
        assertEquals(-1L, id)
        assertEquals(InputResult.InvalidCoordinates, result)
    }

    @Test fun rejectsLongCalibrationGesture() {
        val joystick = JoystickController(InputController { null })
        var result: InputResult? = null
        val id = joystick.move(
            InputTarget(50f, 50f), 1f, 0f, 30f, 900L, 200, 200
        ) { result = it }
        assertEquals(-1L, id)
        assertEquals(InputResult.InvalidDuration, result)
    }
}
