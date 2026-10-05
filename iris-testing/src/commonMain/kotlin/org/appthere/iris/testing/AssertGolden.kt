package org.appthere.iris.testing

import kotlin.test.fail

private val GOLDEN_NAME = Regex("[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*")

/** Golden names are slash-separated segments of letters, digits, `_` and `-`, so they cannot escape their directory. */
public fun requireValidGoldenName(name: String) {
    require(GOLDEN_NAME.matches(name)) { "Invalid golden name '$name'" }
}

/**
 * Compares [actual] with the golden [name] from [store]. On a missing golden, a size mismatch or a
 * difference beyond [tolerance], writes [actual] as a candidate (and failure images when comparable) and fails.
 */
public fun assertGolden(
    name: String,
    actual: GoldenImage,
    tolerance: GoldenTolerance,
    store: GoldenStore,
) {
    requireValidGoldenName(name)
    val expected = store.readGolden(name)
    if (expected == null) {
        store.writeCandidate(name, actual)
        fail(
            "No golden '$name' yet. A candidate was written; " +
                "stage it with ./gradlew goldenUpdate -Pname=$name and review it.",
        )
    }
    if (!sameShape(expected, actual)) {
        store.writeCandidate(name, actual)
        fail("Golden '$name' is ${shapeOf(expected)} but the output is ${shapeOf(actual)}.")
    }
    val comparison = compareGoldenImages(expected, actual)
    if (!comparison.isWithin(tolerance)) {
        store.writeCandidate(name, actual)
        store.writeFailure(name, actual, expected, goldenDiff(expected, actual))
        fail(
            "Golden '$name' differs: max channel delta ${comparison.maxChannelDelta} " +
                "at (${comparison.worstX}, ${comparison.worstY}) channel ${comparison.worstChannel} " +
                "(allowed ${tolerance.maxChannelDelta}), " +
                "mean delta ${comparison.meanDelta} (allowed ${tolerance.maxMeanDelta}).",
        )
    }
}
