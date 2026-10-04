# Phase 0 task breakdown: Foundations and spikes (Gate 1)

Status: **approved 2026-10-04.** Section 4 recommendations accepted (D-019 to D-022).
Written: 2026-10-04. Source: `docs/phase-plan.md` Phase 0; rules from `CLAUDE.md`, `docs/coding-standards.md`, `docs/testing-tdd.md`.

## 0. Machine facts (this development machine)

| Item | Found | Consequence |
| --- | --- | --- |
| OS | Ubuntu 26.04, KDE Plasma on Wayland | Desktop JVM and Wayland spike (S3) can run here |
| JDK | OpenJDK 25.0.3 | Check Gradle/AGP/Kotlin support for JDK 25 in A1. If any of them lacks it, use a toolchain JDK |
| Android SDK | platforms 36 and 37, one emulator image (API 36, google_apis) | S1 needs API 28 and API 33+ images, which are not installed yet |
| Caches | Gradle 9.1.0 distribution, Kotlin/Native 2.4.10 prebuilt | These are hints only. A1 still checks the current stable versions |
| macOS / Xcode | none here; Kevin has a MacBook Air M1 | iOS and macOS evidence comes from that Mac (Kevin runs the steps) or macOS CI |
| Stylus | none on this machine; Galaxy Tab S10 Lite with S Pen available | S9: Android/S Pen evidence only. S3 and other OSes: partial per D-022 |
| Git | repository on `main`, remote `AppThere/iris-cmp` (private) | CI (A8) runs on GitHub Actions |

## 1. Order of work

Part A runs in order, because each task builds on the one before it. Part B (spikes) starts after A7. The headless spikes come first: Phase 1 depends on them and they can run fully on this machine.

```
A0 → A1 → A2 → A3 → A4 → A5 → A6 → A7 → A8 → A9 (core, several commits) → A10 → A11
                                   └──────▶ S12, S4 → S5 → S10, S8, S7, S11 → S1, S2, S6 → S3, S9
```

## 2. Part A: scaffold

Each task names the test that must fail first. "Verify here" lists what this machine can run.

| # | Task | Failing test first | Verify here |
| --- | --- | --- | --- |
| A0 | `git init`, `.gitignore`, `.editorconfig`, `LICENSE` (Apache-2.0), `NOTICE`, Gradle wrapper, `settings.gradle.kts` with `build-logic` as an included build | none (no behavior); `./gradlew help` runs | yes |
| A1 | Look up current stable versions (Kotlin, Gradle, AGP, Compose Multiplatform, coroutines, detekt, Spotless/ktlint, Kover, Kotest, ABI validation) in official sources. Pin them in `libs.versions.toml` with the date checked. Record each in `docs/dependencies.md`. **Ask Kevin to approve the list before adding it** | none | yes |
| A2 | Convention plugins in `build-logic/`: `iris.kmp.library` (JVM, Android minSdk 28, `iosArm64`, `iosSimulatorArm64`, `explicitApi()`, warnings as errors), `iris.kmp.compose`, `iris.app.desktop/android/ios`, `iris.quality` (detekt, Spotless, Kover) | `ExplicitApiConventionTest` (TestKit: a fixture module with an undeclared-visibility public function fails to compile) | JVM/Android yes; iOS klib compile only if cross-compilation works (see section 4, item 5) |
| A3 | Empty module skeletons for every module in `architecture.md` §1, each with a `README.md`, package `org.appthere.iris.<module>`, and only the dependencies the table allows | covered by A4 | yes |
| A4 | `ArchitectureTest`: a rule table copied from `architecture.md` §1. It checks (a) declared project dependencies and (b) forbidden imports in engine modules (`androidx.compose`, `android.`, `java.awt`, `javax.swing`, `platform.UIKit`, `org.jetbrains.skia`, `kotlinx.cinterop` and others). Wired into `check` | `ArchitectureTest` fails on a fixture where `iris-core` depends on `iris-model`, and on a fixture engine file that imports `java.awt.Color` | yes |
| A5 | `verifySizeLimits`: file length (400 lines, 600 for tests, `testdata/` and generated code exempt) as a small task. Function length, complexity, nesting, parameters and public members come from the detekt config. Both run in `check` | `SizeLimitsTest` on fixture files (401-line source, 601-line test, exempt fixture); `DetektLimitsFixtureTest` (51-line function, complexity 13, nesting 5) | yes |
| A6 | `licenseCheck`: resolves every configuration that ships (runtime classpaths, native klibs), reads the POM licenses, compares them to an allow-list, and fails on unknown licenses | `LicenseAllowListTest`: fixture POMs (GPL rejected, Apache accepted, missing license rejected, dual-license picks the allowed one) | yes |
| A7 | `goldenUpdate -Pname=...` writes to a pending area. `goldenReview` lists pending adds and changes against `testdata/golden/`. Both are format-agnostic | `GoldenReviewTaskTest` on fixture directories | yes |
| A8 | GitHub Actions: Linux `check`; Windows and macOS JVM tests; Android emulator (Linux, KVM); iOS simulator tests (macOS). `docs/STATUS.md` lists what cannot run in CI (real pens, GPU parity, Wayland tablet) | CI run green | needs a remote repository |
| A9 | `iris-core`, one commit pair per item: ids (value classes over UUID); `Point`, `Rect`, `IntRect`, `Matrix` (affine); `IrisResult`/`IrisError`; `Clock`; `DispatcherProvider`; `Logger`; `FractionalIndex` | `IdTest`, `RectTest`, `MatrixTest` + `MatrixProperties` (inverse, associativity), `IrisResultTest`, `FractionalIndexTest` + `FractionalIndexProperties` (strictly between, unique across site ids, bounded growth at one end) | JVM, Android unit; iOS only on CI |
| A10 | `iris-testing`: `FakeClock`, `FakeDispatcherProvider` (virtual time), `RecordingLogger`, `assertClose` (ulps/epsilon), golden compare core (`maxChannelDelta`, `maxMeanDelta`, failure files in `build/golden-failures/`), `ScriptedStylusSource`. Needs a minimal `RawPenEvent`/`StylusInputSource` in `iris-input` (the contract from `architecture.md` §14). Document builders wait for Phase 1 because `iris-model` does not exist yet | `AssertCloseTest`, `GoldenCompareTest`, `ScriptedStylusSourceTest`, `FakeClockTest` | JVM yes |
| A11 | Hello-world apps: `app-desktop`, `app-android`, `app-ios` show an empty Compose screen; `iris-cli --version` | `CliVersionTest`; `EmptyScreenTest` (Compose desktop UI test in `iris-ui`) | desktop, Android emulator; iOS only on CI or a Mac |

Expected at the end of Part A: `./gradlew check` is green on Linux, and `ArchitectureTest`, size limits, the license check and formatting all run in `check`. This is the first half of exit gate 1.

## 3. Part B: spikes

Each spike gets a report `docs/spikes/S<n>-<name>.md` and a decision-log entry. Spike code goes in `spikes/`. A finding that is a requirement gets a permanent test.

| Spike | Can run here | Needs from Kevin | Unblocks |
| --- | --- | --- | --- |
| S12 Shared OPC | yes (interface sketch only) | decision | `iris-opc` (Phase 1) |
| S4 Deflate + ZIP64 | JVM, Android; iOS on CI | approval if the iOS route uses platform zlib (a native library under the stop-and-ask rule) | `iris-opc`, `iris-exr` |
| S5 EXR feasibility | yes, with the OpenEXR tools (`exrheader`, BSD) installed as a dev-only tool | OK to install them | `iris-exr` |
| S10 Chunk size, compression | yes (desktop numbers); tablet numbers later | none | P-004 |
| S8 TileBuffer, persistent collections | JVM, Android; iOS on CI | none | `iris-pixels` |
| S7 Color management | yes (compare against a Little CMS reference tool, dev-only) | decision if it means wrapping lcms (native) | `iris-color` |
| S11 Path booleans | JVM (Skiko), Android | none | `iris-vector` (Phase 5) |
| S1 Renderer | desktop, Android emulator | API 28 and API 33+ emulator images or physical devices; a Mac for iOS | `iris-render-skia` |
| S2 Canvas embedding | desktop, Android | a Mac for iOS | `CanvasHost` |
| S6 Text shaping parity | desktop, Android | a Mac for iOS | text (Phase 5) |
| S3 Wayland + tablet-v2 | Wayland yes, tablet no | a pen tablet on this machine | P-005 |
| S9 Stylus capture | none without pens | pen devices for each OS | Phase 3 adapters |

Also at the end of Phase 0: reference devices and performance budgets go in `docs/hardware-matrix.md` (P-007), and the iOS/macOS minimum versions get set (P-002).

## 4. Proposed deviations and open questions (resolved 2026-10-04: all accepted; spec added)

1. **`docs/product-spec.md` is missing.** Exit gates 1 to 4 come from it. Part A does not need it; the spikes and Gate 1 sign-off do.
2. **`ArchitectureTest` location.** `architecture.md` §1 puts it in `iris-testing`. It has to read the Gradle dependency graph, so the proposal is to put it in `build-logic` (JVM unit tests plus a `verifyArchitecture` task in `check`). This changes a sentence in `architecture.md`, so it needs Kevin's approval.
3. **Experimental stdlib APIs.** `kotlin.uuid.Uuid` (used for ids) may still require an opt-in. A1 checks. If it does, the proposal is one wrapper site plus a decision-log entry, as for Compose.
4. **License allow-list.** Proposal: Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, Zlib for shipped code. EPL-1.0/2.0 (JUnit and similar) allowed in test scope only.
5. **iOS without a Mac.** Kotlin/Native may cross-compile Apple klibs on Linux. A1/A2 check whether that works, and whether it needs an opt-in flag. Linking, running and testing still need macOS CI or a Mac.
6. **Gate 1 and hardware.** S3 and S9 cannot be finished without pens. Should Gate 1 accept a documented partial result for these, with completion carried into Phase 3?
