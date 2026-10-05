# Dependencies

One row per dependency. Versions live in `gradle/libs.versions.toml`.

| Name | Version | License | Used by | Why | Checked |
| --- | --- | --- | --- | --- | --- |
| Gradle (wrapper) | 9.7.0 | Apache-2.0 | build | Highest Gradle that Kotlin 2.4.20 supports (KGP table: 7.6.3 to 9.7.0); runs on JDK 25 (Gradle 9.1+) | 2026-10-04 |
| Kotlin (Gradle plugin, stdlib, `kotlin-test`, Compose compiler plugin) | 2.4.20 | Apache-2.0 | all | Latest stable (2.5.0 is Beta). Needs Xcode 26.4 for iOS. `kotlin.uuid.Uuid` is stable since 2.4. Built-in ABI validation (`abiValidation()`, `checkKotlinAbi`) is experimental and needs one opt-in in `build-logic` | 2026-10-04 |
| Android Gradle plugin (`com.android.kotlin.multiplatform.library`, `com.android.application`) | 9.3.1 | Apache-2.0 | Android targets | Highest AGP that Kotlin 2.4.20 supports (8.5.2 to 9.3.1). AGP 9.4.1 needs Gradle 9.6 or later and is outside Kotlin's tested range. AGP 9 with KMP requires the `com.android.kotlin.multiplatform.library` plugin (the `android {}` block in `kotlin {}`) | 2026-10-04 |
| Compose Multiplatform (Gradle plugin and runtime) | 1.12.1 | Apache-2.0 | `iris-ui`, `app-*` | Latest stable (1.13.0 is alpha). Platform minimums: iOS 14, macOS 13 arm64 (Kotlin/Native's own iOS default is 15.0; see D-024). Runtime artifact `org.jetbrains.compose.runtime:runtime` (catalog `compose-runtime`), added to `commonMain` by `iris.kmp.compose`. UI artifact `org.jetbrains.compose.ui:ui` (catalog `compose-ui`), added by `iris.app.ios` for `ComposeUIViewController`; has android, desktop, `iosArm64` and `iosSimulatorArm64` variants. Desktop runtime `org.jetbrains.compose.desktop:desktop-jvm-<os>-<arch>` for the build host, added by `iris.app.desktop` (the plugin's `compose.desktop.currentOs` accessor is deprecated in 1.12 in favor of direct coordinates) | 2026-10-04 |
| `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 | `app-android` | Compose entry point for the Android activity; added by `iris.app.android` | 2026-10-04 |
| `kotlinx-coroutines-core`, `kotlinx-coroutines-test` | 1.11.0 | Apache-2.0 | engine (core), tests (test) | Actors, dispatchers, virtual time. iOS artifacts present | 2026-10-04 |
| detekt (Gradle plugin `dev.detekt`) | 2.0.0-alpha.6 | Apache-2.0 | build (static analysis, size limits) | The only detekt that supports our stack: built against Kotlin 2.4.10, Gradle 9.6.1, AGP 9.3.1, JDK 25. The stable 1.23.8 targets Kotlin 2.0.21 and Gradle 8.12. **Alpha**; see D-023 | 2026-10-04 |
| Spotless (Gradle plugin) | 8.10.3 | Apache-2.0 | build (formatting) | Runs ktlint | 2026-10-04 |
| ktlint | 1.8.0 | MIT | build (formatting, via Spotless) | Kotlin official style | 2026-10-04 |
| Kover (Gradle plugin) | 0.9.11 | Apache-2.0 | build (coverage) | 85% line floor on engine modules (JVM) | 2026-10-04 |
| `io.kotest:kotest-property` | 6.2.5 | Apache-2.0 | tests only | Property tests; runs under `kotlin.test` without the Kotest engine. Has JVM and iOS artifacts. Android host tests use the JVM artifact. Transitive: rgxgen 2.0, java-diff-utils 4.16, opentest4j 1.3.0 (all Apache-2.0) | 2026-10-04 |
| JUnit Platform (Jupiter, launcher) | via `kotlin-test-junit5` / JUnit BOM | EPL-2.0 | tests only (JVM, `build-logic`) | Test runner behind `kotlin-test` on the JVM; allowed in test scope by D-020 | 2026-10-04 |

## CI only (GitHub Actions, not shipped)

Pinned to commit SHAs in `.github/workflows/ci.yml`.

| Action | Version | License | Checked |
| --- | --- | --- | --- |
| `actions/checkout` | v7.0.1 (`3d3c42e5`) | MIT | 2026-10-05 |
| `actions/setup-java` | v6.0.1 (`de7274f0`) | MIT | 2026-10-05 |
| `actions/cache` | v6.1.0 (`55cc8345`) | MIT | 2026-10-05 |
| `gradle/actions/setup-gradle` | v6.4.0 (`3f5f9ada`) | MIT | 2026-10-05 |
| `reactivecircus/android-emulator-runner` | v2.38.0 (`a421e438`) | Apache-2.0 | 2026-10-05 |
| `actions/upload-artifact` | v7.0.1 (`043fb46d`) | MIT | 2026-10-05 |

Runner images: `ubuntu-24.04` (Android SDK with `android-37.0`), `windows-2025`, `macos-26` (Xcode 26.4.1 selected; the image default is 26.6).

Sources for all rows: Maven Central and Google Maven metadata (latest stable), POM license fields, the Kotlin Gradle plugin compatibility table, the AGP release notes, the Gradle compatibility matrix and the detekt compatibility table. Approved by Kevin 2026-10-04.

Deferred until a task needs them: `kotlinx-collections-immutable` 0.5.2 (Apache-2.0, decided by S8), `kotlinx-benchmark` 0.5.0 (Apache-2.0, first benchmark), foojay toolchain resolver (not needed: the local JDK 25 runs the build and CI uses `setup-java`).
