package org.appthere.iris.testing

import kotlin.math.abs

/** The result of comparing two images of the same size: the worst sample, where it is, and the mean delta. */
public data class GoldenComparison(
    val maxChannelDelta: Float,
    val meanDelta: Float,
    val worstX: Int,
    val worstY: Int,
    val worstChannel: Int,
) {
    public fun isWithin(tolerance: GoldenTolerance): Boolean =
        maxChannelDelta <= tolerance.maxChannelDelta && meanDelta <= tolerance.maxMeanDelta
}

/** Compares every sample of two images with the same width, height and channel count. */
public fun compareGoldenImages(
    expected: GoldenImage,
    actual: GoldenImage,
): GoldenComparison {
    require(sameShape(expected, actual)) { "Cannot compare ${shapeOf(actual)} with ${shapeOf(expected)}" }
    var worst = GoldenComparison(0f, 0f, 0, 0, 0)
    var sum = 0.0
    for (y in 0 until expected.height) {
        for (x in 0 until expected.width) {
            for (c in 0 until expected.channels) {
                val delta = sampleDelta(expected.sample(x, y, c), actual.sample(x, y, c))
                sum += delta
                if (delta >
                    worst.maxChannelDelta
                ) {
                    worst =
                        worst.copy(maxChannelDelta = delta, worstX = x, worstY = y, worstChannel = c)
                }
            }
        }
    }
    val count = expected.width.toLong() * expected.height * expected.channels
    return worst.copy(meanDelta = if (count == 0L) 0f else (sum / count).toFloat())
}

/** The absolute difference of two images, computed on demand rather than copied. */
public fun goldenDiff(
    expected: GoldenImage,
    actual: GoldenImage,
): GoldenImage {
    require(sameShape(expected, actual)) { "Cannot diff ${shapeOf(actual)} with ${shapeOf(expected)}" }
    return object : GoldenImage {
        override val width = expected.width
        override val height = expected.height
        override val channels = expected.channels

        override fun sample(
            x: Int,
            y: Int,
            channel: Int,
        ): Float = sampleDelta(expected.sample(x, y, channel), actual.sample(x, y, channel))
    }
}

/** NaN matches only NaN; an infinity matches only itself. */
private fun sampleDelta(
    expected: Float,
    actual: Float,
): Float =
    when {
        expected == actual || (expected.isNaN() && actual.isNaN()) -> 0f
        expected.isNaN() || actual.isNaN() -> Float.POSITIVE_INFINITY
        else -> abs(actual - expected)
    }

internal fun sameShape(
    a: GoldenImage,
    b: GoldenImage,
): Boolean = a.width == b.width && a.height == b.height && a.channels == b.channels

internal fun shapeOf(image: GoldenImage): String = "${image.width}x${image.height}x${image.channels}"
