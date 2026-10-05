package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntRectTest {
    private val rect = IntRect(0, 0, 256, 128)

    @Test
    fun sizeAndAreaComeFromTheEdges() {
        assertEquals(256, rect.width)
        assertEquals(128, rect.height)
        assertEquals(32_768L, rect.area)
    }

    @Test
    fun areaDoesNotOverflowInt() {
        assertEquals(4_294_967_296L, IntRect(0, 0, 65_536, 65_536).area)
    }

    @Test
    fun containsIsHalfOpen() {
        assertTrue(rect.contains(0, 0))
        assertTrue(rect.contains(255, 127))
        assertFalse(rect.contains(256, 0))
    }

    @Test
    fun intersectAndUnion() {
        assertEquals(IntRect(128, 64, 256, 128), rect.intersect(IntRect(128, 64, 512, 512)))
        assertNull(rect.intersect(IntRect(256, 0, 300, 10)))
        assertEquals(IntRect(-10, 0, 256, 200), rect.union(IntRect(-10, 100, 0, 200)))
    }

    @Test
    fun invertedEdgesAreRejected() {
        assertFailsWith<IllegalArgumentException> { IntRect(1, 0, 0, 1) }
        assertTrue(IntRect(3, 3, 3, 9).isEmpty)
    }
}
