package org.appthere.iris.testing

/** Where goldens are read from and where candidates and failure images are written. */
public interface GoldenStore {
    /** The golden called [name] (such as `blend/multiply`), or null if there is none yet. */
    public fun readGolden(name: String): GoldenImage?

    /** Output for review; `./gradlew goldenUpdate -Pname=<name>` stages it (docs/testing-tdd.md section 4). */
    public fun writeCandidate(
        name: String,
        image: GoldenImage,
    )

    /** Actual, expected and diff images kept together for a failed comparison. */
    public fun writeFailure(
        name: String,
        actual: GoldenImage,
        expected: GoldenImage,
        diff: GoldenImage,
    )
}
