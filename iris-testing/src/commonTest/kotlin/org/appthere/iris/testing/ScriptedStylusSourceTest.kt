package org.appthere.iris.testing

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.appthere.iris.input.PenAction
import org.appthere.iris.input.PenAxes
import org.appthere.iris.input.PenTool
import org.appthere.iris.input.RawPenEvent
import org.appthere.iris.input.StylusCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class ScriptedStylusSourceTest {
    private val capabilities =
        StylusCapabilities(
            pressure = true,
            tilt = true,
            hover = false,
            eraser = true,
            rotation = false,
            predicted = true,
        )

    private val stroke =
        listOf(PenAction.DOWN, PenAction.MOVE, PenAction.MOVE, PenAction.UP).mapIndexed { i, action ->
            RawPenEvent(
                action,
                PenTool.PEN,
                x = 10f * i,
                y = 5f,
                timestamp = (4 * i).milliseconds,
                pressure = 0.25f * (i + 1),
                axes = PenAxes.PRESSURE,
            )
        }

    @Test
    fun playingDeliversTheScriptInOrder() =
        runTest {
            val source = ScriptedStylusSource(capabilities, stroke)
            val received = mutableListOf<RawPenEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.events.toList(received) }

            source.playAll()

            assertEquals(stroke, received)
        }

    @Test
    fun playNextStepsThroughTheScript() =
        runTest {
            val source = ScriptedStylusSource(capabilities, stroke)
            val received = mutableListOf<RawPenEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.events.toList(received) }

            source.playNext()
            source.playNext(2)

            assertEquals(stroke.take(3), received)
            assertEquals(1, source.remaining)
            assertFailsWith<IllegalArgumentException> { source.playNext(2) }
        }

    @Test
    fun everyCollectorSeesTheEvents() =
        runTest {
            val source = ScriptedStylusSource(capabilities, stroke)
            val first = mutableListOf<RawPenEvent>()
            val second = mutableListOf<RawPenEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.events.toList(first) }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.events.toList(second) }

            source.playAll()

            assertEquals(stroke, first)
            assertEquals(stroke, second)
        }

    @Test
    fun capabilitiesAndPredictionsComeFromTheScript() {
        val predicted = stroke.last().copy(action = PenAction.MOVE)
        val source =
            ScriptedStylusSource(capabilities, stroke) { horizon ->
                if (horizon >=
                    8
                ) {
                    listOf(predicted)
                } else {
                    emptyList()
                }
            }

        assertEquals(capabilities, source.capabilities)
        assertEquals(listOf(predicted), source.predicted(horizonMillis = 16))
        assertEquals(emptyList(), source.predicted(horizonMillis = 4))
    }
}
