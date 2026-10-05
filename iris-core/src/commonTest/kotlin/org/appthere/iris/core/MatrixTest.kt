package org.appthere.iris.core

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatrixTest {
    @Test
    fun identityLeavesPointsAlone() {
        assertEquals(Point(3.0, -4.0), Matrix.IDENTITY.map(Point(3.0, -4.0)))
    }

    @Test
    fun componentsFollowSvgOrder() {
        // x' = a x + c y + e, y' = b x + d y + f
        val m = Matrix(a = 1.0, b = 2.0, c = 3.0, d = 4.0, e = 5.0, f = 6.0)

        assertEquals(Point(1.0 * 10 + 3.0 * 20 + 5.0, 2.0 * 10 + 4.0 * 20 + 6.0), m.map(Point(10.0, 20.0)))
    }

    @Test
    fun translateScaleAndRotate() {
        assertEquals(Point(11.0, 18.0), Matrix.translate(10.0, 20.0).map(Point(1.0, -2.0)))
        assertEquals(Point(2.0, -6.0), Matrix.scale(2.0, 3.0).map(Point(1.0, -2.0)))
        assertNear(Point(0.0, 1.0), Matrix.rotate(Radians(PI / 2)).map(Point(1.0, 0.0)))
    }

    @Test
    fun timesAppliesTheRightOperandFirst() {
        val scaleThenMove = Matrix.translate(10.0, 0.0) * Matrix.scale(2.0)

        assertEquals(Point(12.0, 2.0), scaleThenMove.map(Point(1.0, 1.0)))
    }

    @Test
    fun inverseUndoesTheTransform() {
        val m = Matrix.translate(5.0, -3.0) * Matrix.rotate(Radians.fromDegrees(30.0)) * Matrix.scale(2.0, 0.5)

        assertNear(Point(7.0, 9.0), m.inverse()!!.map(m.map(Point(7.0, 9.0))))
        assertNear(Matrix.IDENTITY, m * m.inverse()!!)
    }

    @Test
    fun aSingularMatrixHasNoInverse() {
        assertNull(Matrix.scale(0.0, 1.0).inverse())
        assertEquals(0.0, Matrix.scale(0.0, 1.0).determinant)
    }

    @Test
    fun mapRectGivesTheBoundingBoxOfTheCorners() {
        val rotated = Matrix.rotate(Radians(PI / 2)).mapRect(Rect(0.0, 0.0, 2.0, 1.0))

        assertNear(-1.0, rotated.left)
        assertNear(0.0, rotated.top)
        assertNear(0.0, rotated.right)
        assertNear(2.0, rotated.bottom)
    }
}
