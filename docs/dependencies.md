# Dependencies

One row per dependency. Versions live in `gradle/libs.versions.toml`.

| Name | Version | License | Used by | Why | Checked |
| --- | --- | --- | --- | --- | --- |
| Gradle (wrapper) | 9.7.0 | Apache-2.0 | build | Highest Gradle that Kotlin 2.4.20 supports (KGP table: 7.6.3 to 9.7.0); runs on JDK 25 (Gradle 9.1+) | 2026-10-04 |

## Proposed in A1 (awaiting Kevin's approval; not yet in `libs.versions.toml`)

Sources, all checked 2026-10-04: Maven Central and Google Maven metadata (latest stable), the POM license fields, the Kotlin Gradle plugin compatibility table (kotlinlang.org/docs/gradle-configure-project.html), the AGP release notes, the Gradle compatibility matrix, and the detekt compatibility table.

| Name | Version | License | Used by | Why | Notes |
| --- | --- | --- | --- | --- | --- |
| Kotlin (Gradle plugin, stdlib, `kotlin-test`, Compose compiler plugin) | 2.4.20 | Apache-2.0 | all | Latest stable (2.5.0 is Beta) | Needs Xcode 26.4 for iOS. `kotlin.uuid.Uuid` is stable since 2.4. Built-in ABI validation (`abiValidation()`, `checkKotlinAbi`) is experimental and needs one opt-in in `build-logic` |
| Android Gradle plugin (`com.android.kotlin.multiplatform.library`, `com.android.application`) | 9.3.1 | Apache-2.0 | Android targets | Highest AGP that Kotlin 2.4.20 supports (8.5.2 to 9.3.1). AGP 9.4.1 needs Gradle 9.6 or later and is outside Kotlin's tested range | AGP 9 with KMP requires the `com.android.kotlin.multiplatform.library` plugin (the `android {}` block in `kotlin {}`) |
| Compose Multiplatform (Gradle plugin and runtime) | 1.12.1 | Apache-2.0 | `iris-ui`, `app-*` | Latest stable (1.13.0 is alpha) | Platform minimums: iOS 14, macOS 13 arm64 |
| `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 | `app-android` | Compose entry point for the Android activity | |
| `kotlinx-coroutines-core`, `kotlinx-coroutines-test` | 1.11.0 | Apache-2.0 | engine (core), tests (test) | Actors, dispatchers, virtual time | iOS artifacts present |
| detekt (Gradle plugin `dev.detekt`) | **2.0.0-alpha.6** | Apache-2.0 | build (static analysis, size limits) | The only detekt that supports our stack: built against Kotlin 2.4.10, Gradle 9.6.1, AGP 9.3.1, JDK 25. The stable 1.23.8 targets Kotlin 2.0.21 and Gradle 8.12 | **Alpha.** See the decision below |
| Spotless (Gradle plugin) | 8.10.3 | Apache-2.0 | build (formatting) | Runs ktlint | |
| ktlint | 1.8.0 | MIT | build (formatting, via Spotless) | Kotlin official style | |
| Kover (Gradle plugin) | 0.9.11 | Apache-2.0 | build (coverage) | 85% line floor on engine modules (JVM) | |
| `io.kotest:kotest-property` | 6.2.5 | Apache-2.0 | tests only | Property tests; runs under `kotlin.test` without the Kotest engine | Has JVM and iOS artifacts. Android host tests use the JVM artifact. Transitive: rgxgen 2.0, java-diff-utils 4.16, opentest4j 1.3.0 (all Apache-2.0) |

Deferred until a task needs them: `kotlinx-collections-immutable` 0.5.2 (Apache-2.0, decided by S8), `kotlinx-benchmark` 0.5.0 (Apache-2.0, first benchmark), foojay toolchain resolver (not needed: the local JDK 25 runs the build and CI uses `setup-java`).

Decision needed: detekt 2.0.0-alpha.6 (pinned, guarded by `DetektLimitsFixtureTest` so a regression in rule behavior fails the build) versus writing our own size checks on top of the Kotlin compiler's PSI (large, and a second dependency on the compiler).
