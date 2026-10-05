package org.appthere.iris.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PenAxesTest {
    @Test
    fun axesCombineAndAreQueriedIndividually() {
        val axes = PenAxes.PRESSURE + PenAxes.TILT

        assertTrue(PenAxes.PRESSURE in axes)
        assertTrue(PenAxes.TILT in axes)
        assertFalse(PenAxes.TWIST in axes)
    }

    @Test
    fun noneHasNoAxesAndAllHasEveryAxis() {
        val each = listOf(PenAxes.PRESSURE, PenAxes.TILT, PenAxes.AZIMUTH, PenAxes.TWIST, PenAxes.HOVER_DISTANCE)

        each.forEach {
            assertFalse(it in PenAxes.NONE, "$it")
            assertTrue(it in PenAxes.ALL, "$it")
        }
        assertEquals(PenAxes.ALL, each.reduce(PenAxes::plus))
    }

    @Test
    fun aMouseEventReportsNoOptionalAxes() {
        val event = RawPenEvent(PenAction.MOVE, PenTool.MOUSE, x = 10f, y = 20f, timestamp = kotlin.time.Duration.ZERO)

        assertEquals(PenAxes.NONE, event.axes)
    }
}
