package org.appthere.iris.testing

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GoldenCompareTest {
    private val golden = ArrayImage(2, 2, 4, FloatArray(16) { it / 16f })

    @Test
    fun identicalImagesHaveNoDelta() {
        val comparison = compareGoldenImages(golden, golden)

        assertEquals(0f, comparison.maxChannelDelta)
        assertEquals(0f, comparison.meanDelta)
    }

    @Test
    fun theWorstSampleAndTheMeanAreReported() {
        val actual = golden.with(x = 1, y = 0, channel = 2, value = golden.sample(1, 0, 2) + 0.5f)

        val comparison = compareGoldenImages(golden, actual)

        assertClose(0.5f, comparison.maxChannelDelta, ulps = 4)
        assertClose(0.5f / 16, comparison.meanDelta, ulps = 4)
        assertEquals(Triple(1, 0, 2), Triple(comparison.worstX, comparison.worstY, comparison.worstChannel))
    }

    @Test
    fun aMatchWithinToleranceWritesNothing() {
        val store = MemoryGoldenStore(mapOf("blend/multiply" to golden))
        val actual = golden.with(0, 0, 0, golden.sample(0, 0, 0) + 0.001f)

        assertGolden("blend/multiply", actual, GoldenTolerance(maxChannelDelta = 0.01f, maxMeanDelta = 0.001f), store)

        assertTrue(store.candidates.isEmpty() && store.failures.isEmpty())
    }

    @Test
    fun exceedingTheMaxDeltaFailsAndWritesCandidateAndFailureFiles() {
        val store = MemoryGoldenStore(mapOf("blend/multiply" to golden))
        val actual = golden.with(1, 1, 3, 0f)

        val error =
            assertFailsWith<AssertionError> {
                assertGolden("blend/multiply", actual, GoldenTolerance(0.01f, 1f), store)
            }

        assertContains(error.message.orEmpty(), "blend/multiply")
        assertContains(error.message.orEmpty(), "max channel delta 0.9375")
        assertContains(error.message.orEmpty(), "at (1, 1) channel 3")
        assertEquals(setOf("blend/multiply"), store.candidates.keys)
        val (writtenActual, writtenExpected, diff) = store.failures.getValue("blend/multiply")
        assertEquals(actual, writtenActual)
        assertEquals(golden, writtenExpected)
        assertEquals(0.9375f, diff.sample(1, 1, 3))
        assertEquals(0f, diff.sample(0, 0, 0))
    }

    @Test
    fun theMeanToleranceCanFailOnItsOwn() {
        val shifted = ArrayImage(2, 2, 4, FloatArray(16) { it / 16f + 0.01f })

        assertFailsWith<AssertionError> {
            assertGolden(
                "x",
                shifted,
                GoldenTolerance(maxChannelDelta = 0.02f, maxMeanDelta = 0.005f),
                MemoryGoldenStore(
                    mapOf("x" to golden),
                ),
            )
        }
    }

    @Test
    fun aMissingGoldenWritesACandidateAndExplainsTheNextStep() {
        val store = MemoryGoldenStore()

        val error =
            assertFailsWith<AssertionError> { assertGolden("brush/round", golden, GoldenTolerance.EXACT, store) }

        assertContains(error.message.orEmpty(), "goldenUpdate -Pname=brush/round")
        assertEquals(golden, store.candidates["brush/round"])
    }

    @Test
    fun aSizeMismatchFailsAndWritesACandidate() {
        val store = MemoryGoldenStore(mapOf("x" to golden))
        val wider = ArrayImage(3, 2, 4)

        val error = assertFailsWith<AssertionError> { assertGolden("x", wider, GoldenTolerance.EXACT, store) }

        assertContains(error.message.orEmpty(), "3x2x4")
        assertContains(error.message.orEmpty(), "2x2x4")
        assertEquals(setOf("x"), store.candidates.keys)
    }

    @Test
    fun nanMatchesOnlyNan() {
        val withNan = golden.with(0, 0, 0, Float.NaN)

        assertEquals(0f, compareGoldenImages(withNan, withNan).maxChannelDelta)
        assertEquals(Float.POSITIVE_INFINITY, compareGoldenImages(golden, withNan).maxChannelDelta)
    }

    @Test
    fun toleranceMustNotBeNegative() {
        assertFailsWith<IllegalArgumentException> { GoldenTolerance(-0.1f, 0f) }
    }
}
