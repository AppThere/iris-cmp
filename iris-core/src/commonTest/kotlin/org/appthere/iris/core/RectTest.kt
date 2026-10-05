package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RectTest {
    private val rect = Rect(10.0, 20.0, 110.0, 70.0)

    @Test
    fun sizeAndCenterComeFromTheEdges() {
        assertEquals(100.0, rect.width)
        assertEquals(50.0, rect.height)
        assertEquals(Point(60.0, 45.0), rect.center)
        assertEquals(rect, Rect.fromSize(10.0, 20.0, 100.0, 50.0))
    }

    @Test
    fun containsIsHalfOpen() {
        assertTrue(rect.contains(Point(10.0, 20.0)))
        assertTrue(rect.contains(Point(109.999, 69.999)))
        assertFalse(rect.contains(Point(110.0, 40.0)))
        assertFalse(rect.contains(Point(50.0, 70.0)))
    }

    @Test
    fun intersectionIsTheOverlapOrNull() {
        assertEquals(Rect(60.0, 20.0, 110.0, 30.0), rect.intersect(Rect(60.0, 0.0, 200.0, 30.0)))
        assertNull(rect.intersect(Rect(110.0, 20.0, 120.0, 70.0)), "touching edges do not overlap")
        assertNull(rect.intersect(Rect(0.0, 0.0, 5.0, 5.0)))
    }

    @Test
    fun unionIsTheBoundingRect() {
        assertEquals(Rect(0.0, 0.0, 110.0, 70.0), rect.union(Rect(0.0, 0.0, 5.0, 5.0)))
    }

    @Test
    fun translateMovesBothCorners() {
        assertEquals(Rect(15.0, 10.0, 115.0, 60.0), rect.translate(5.0, -10.0))
    }

    @Test
    fun emptyRectsHaveZeroWidthOrHeight() {
        assertTrue(Rect(1.0, 1.0, 1.0, 5.0).isEmpty)
        assertFalse(rect.isEmpty)
    }

    @Test
    fun invertedEdgesAreRejected() {
        assertFailsWith<IllegalArgumentException> { Rect(10.0, 0.0, 0.0, 10.0) }
        assertFailsWith<IllegalArgumentException> { Rect(0.0, Double.NaN, 1.0, 1.0) }
    }

    @Test
    fun roundOutCoversEveryTouchedPixel() {
        assertEquals(IntRect(-1, 2, 4, 6), Rect(-0.5, 2.0, 3.2, 5.01).roundOut())
    }
}
