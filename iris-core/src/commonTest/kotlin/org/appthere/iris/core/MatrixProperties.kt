package org.appthere.iris.core

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.math.PI
import kotlin.test.Test

class MatrixProperties {
    private val config = PropTestConfig(seed = 20261005L)

    /** Well-conditioned affine matrices: translate * rotate * scale, with scales kept away from zero. */
    private val matrices: Arb<Matrix> =
        Arb.bind(
            coordinate(),
            coordinate(),
            Arb.double(-PI, PI, includeNaNs = false),
            Arb.double(0.1, 10.0, includeNaNs = false),
            Arb.double(-10.0, -0.1, includeNaNs = false),
        ) { tx, ty, angle, sx, sy ->
            Matrix.translate(tx, ty) * Matrix.rotate(Radians(angle)) * Matrix.scale(sx, sy)
        }

    private val points: Arb<Point> = Arb.bind(coordinate(), coordinate()) { x, y -> Point(x, y) }

    @Test
    fun inverseUndoesEveryWellConditionedMatrix() =
        runTest {
            checkAll(config, matrices, points) { m, p ->
                val inverse = m.inverse() ?: error("$m should be invertible")
                assertNear(p, inverse.map(m.map(p)), tolerance = 1e-6)
                assertNear(Matrix.IDENTITY, m * inverse, tolerance = 1e-6)
            }
        }

    @Test
    fun multiplicationIsAssociative() =
        runTest {
            checkAll(config, matrices, matrices, matrices) { a, b, c ->
                assertNear((a * b) * c, a * (b * c), tolerance = 1e-6)
            }
        }

    @Test
    fun aProductAppliesItsFactorsRightToLeft() =
        runTest {
            checkAll(config, matrices, matrices, points) { a, b, p ->
                assertNear(a.map(b.map(p)), (a * b).map(p), tolerance = 1e-6)
            }
        }

    private fun coordinate(): Arb<Double> = Arb.double(-1_000.0, 1_000.0, includeNaNs = false)
}
