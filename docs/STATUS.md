# Status

Update at the end of every session. Keep it short and true.

## Current phase
Phase 0: Foundations and spikes (in progress)

## Done
- Product spec, architecture, file format, standards, TDD guide, phase plan and decision log written (design only; no code yet).
- `docs/phases/phase-0.md` approved 2026-10-04; its open questions resolved as D-019 to D-022.
- Git repository initialized (branch `main`), pushed to public GitHub repo `AppThere/iris-cmp` (pushed through A8 on 2026-10-05); commits use the GitHub noreply email (repo-local git config).
- Available hardware recorded in `docs/hardware-matrix.md`: Galaxy Tab S10 Lite with S Pen, MacBook Air M1, Linux/Wayland dev machine.

- A0 done: `LICENSE` (canonical Apache-2.0 text), `NOTICE`, `.gitignore`, `.gitattributes`, `.editorconfig`, Gradle 9.7.0 wrapper with distribution checksum, `settings.gradle.kts` including `build-logic`. `./gradlew help` passes on JDK 25.
- A1 done: versions approved and pinned in `gradle/libs.versions.toml`, licenses in `docs/dependencies.md` (checked 2026-10-04), D-023 (detekt 2.0 alpha).
- A2 done. `build-logic` has unit tests (`test`) and TestKit fixture tests (`functionalTest`), both run by root `./gradlew check`.
  - `iris.kmp.library`: JVM, Android (min SDK 28), `iosArm64`, `iosSimulatorArm64`, `explicitApi()`, JVM 17 bytecode, warnings as errors, kotlin-test in commonTest.
  - `iris.quality` (applied by `iris.kmp.library`): Spotless with ktlint, detekt over all of `src/`, Kover; all three in `check`.
  - `iris.kmp.compose`: `iris.kmp.library` plus the Compose compiler plugin and the Compose runtime in `commonMain`. Verified on JVM and Android; not the `org.jetbrains.compose` plugin (left for the app conventions, which need packaging and resources).
  - `iris.app.desktop`: Kotlin/JVM app with the Compose compiler, the `org.jetbrains.compose` plugin and the Compose desktop runtime for the build host; main class `MainKt` in the module's package. Packaging formats are not configured yet. A desktop build only bundles the host's native Skiko, so each OS is packaged on its own runner (A8).
  - `iris.jvm.app` (added for A3): Kotlin/JVM `application` with main class `MainKt` in the module's package; shares the JVM setup with `iris.app.desktop`.
  - iOS on Linux answered (phase-0 §4 item 5; `IosOnLinuxTest`): Kotlin/Native 2.4.20 compiles `iosArm64` and `iosSimulatorArm64` klibs on Linux with no opt-in flag. That covers main and test sources, Foundation/UIKit calls and composables. A type error in `iosMain` fails the build here. Linking and `iosSimulatorArm64Test` are SKIPPED (not failed), so a green Linux build has not run any iOS test.
  - `iris.app.android`: AGP application with built-in Kotlin (AGP 9 fails if `org.jetbrains.kotlin.android` is applied), the Compose compiler and `activity-compose`. `applicationId` is the package root `org.appthere.iris` (placeholder, P-001); min SDK 28, target SDK 36, compile SDK 37 (D-025). The app module supplies its own manifest. Signing, versionCode and release builds are not configured yet.
  - `iris.app.ios`: Kotlin Multiplatform with only `iosArm64` and `iosSimulatorArm64`, each with a static `IrisApp` framework (debug and release) for Xcode's `embedAndSignAppleFrameworkForXcode`; Compose compiler, runtime and UI. Minimum iOS 15.0 (D-026, superseding D-024) on every iOS binary, in libraries and the app. The Xcode project comes with A11.
- A3 done: 23 module skeletons (all of `architecture.md` §1) in `settings.gradle.kts`. Each has a build script with its convention plugin and only the allowed project dependencies, and a README (package, plugin, dependency rule). No sources yet. `iris-testing` declares no dependencies until A10; the apps depend on `iris-ui`, `iris-editor`, `iris-render-skia` and the three platform modules. `./gradlew check` is green across all modules.
- A4 done: `ArchitectureTest` (unit tests on the rule table and checker), `ArchitectureTableSyncTest` (rule table equals `architecture.md` §1), and `verifyArchitecture` from the root plugin `iris.architecture`, run by `check`. It checks declared project dependencies (main and test scope) and Kotlin imports. Engine modules and `iris-testing` may not import `android.`, `androidx.`, `java.awt.`, `javax.swing.`, `javafx.`, `platform.`, `org.jetbrains.skia.`, `org.jetbrains.skiko.`, `org.jetbrains.compose.` or `kotlinx.cinterop.`. `iris-testing` is allowed in test scope only. Checked against the real repo with a temporary violation in `iris-core`.
- A5 done: `verifySizeLimits` per module (400 lines, 600 in test source sets; `testdata/` and `generated/` exempt), in `check`; `./gradlew verifySizeLimits` runs it everywhere. detekt config ships in `build-logic` (`detekt.yml`): function length 50, complexity 12, nesting 4, parameters 7, public functions per class 20. `DetektLimitsFixtureTest` pins each limit at its boundary. `check` also runs the per-compilation detekt tasks (`detektMainJvm`, `detektTestJvm`, `detektMainAndroid`, `detektHostTestAndroid`; `detektMain`/`detektTest` in JVM apps), because plain `detekt` has no type resolution and skips rules such as `LongParameterList`.
- A6 done: `licenseCheck` per module, in `check` (`./gradlew licenseCheck` runs all). Shipped scope: `runtimeClasspath`, `<target>RuntimeClasspath`, `releaseRuntimeClasspath` and `<target>CompileKlibraries`; test scope: their test counterparts. Not checked: `debug*` and the desktop hot-reload configurations (`dev*`, `composeHotReload*`), which are never distributed. POM licenses follow parent POMs and map to SPDX ids; a bare "BSD License" is treated as unrecognized. First real run: 234 components, all shipped ones Apache-2.0; test-only JUnit 4 (EPL-1.0) and Hamcrest (New BSD). Reports in `<module>/build/reports/licenses/dependencies.txt`.
- A7 done: root plugin `iris.golden` with `goldenUpdate -Pname=<pattern>` (candidates from `<module>/build/golden-candidates/` to `testdata/golden-pending/<module>/`) and `goldenReview` (`add`/`change`/`same` against `testdata/golden/`). Byte-level, so format-agnostic. Workflow documented in `docs/testing-tdd.md` section 4; the A10 harness must write candidates there.
- A8 done: `.github/workflows/ci.yml` (actions pinned to SHAs) is green on GitHub (run 37267791752, 2026-10-05): Linux `check`; JVM tests and build-logic unit tests on Windows and macOS; Android emulator API 28 and 35 (`connectedCheck`); iOS simulator tests and `IrisApp` framework links on macOS 26 with Xcode 26.4.1. The first run (37267256192) failed only in the iOS minos step, because `app-ios` has no sources to link yet; that step now waits for A11. Cold runs take 3 to 5 min per job.
- A9 done: `iris-core` with ids (`DocumentId`, `LayerId`, `ArtboardId`, `IdSource`), geometry (`Point`, `Rect`, `IntRect`, `Matrix`, `Radians`), `IrisResult`/`IrisError`, `Clock`/`SystemClock`, `DispatcherProvider`/`DefaultDispatcherProvider`, `Logger`/`LogLevel`/`NoOpLogger`, `FractionalIndex`/`SiteId`. 54 tests on JVM and Android host (iOS tests compile; CI runs them on the simulator), including seeded property tests (`MatrixProperties`, `FractionalIndexProperties`). Line coverage 92%. `FractionalIndexProperties` caught a real collision (a midpoint that is a prefix of the upper bound); pinned as an example test and fixed. `kotlin.uuid.Uuid`, `kotlin.time.Instant` and `kotlin.time.Clock` need no opt-in in Kotlin 2.4; Kotest's seeded `PropTestConfig` needs `@OptIn(ExperimentalKotest::class)`, one site per property class.
- A10 done: `iris-testing` with `assertClose`, `FakeClock`, `FakeDispatcherProvider`, `RecordingLogger`, the golden harness (`GoldenImage`, `compareGoldenImages`, `goldenDiff`, `assertGolden`, `GoldenStore`, `GoldenCodec`, JVM `FileGoldenStore` writing `build/golden-candidates/` and `build/golden-failures/<name>/`) and `ScriptedStylusSource`. `iris-input` has the minimal §14 contract (`StylusInputSource`, `StylusCapabilities`, `RawPenEvent` with a `PenAxes` present-bitmask, `PenAction`, `PenTool`). Coverage: `iris-testing` 99%, `iris-input` 80% (`StylusCapabilities` is exercised only from `iris-testing`). The coroutines-test time and unconfined-dispatcher APIs are experimental, so tests use `runCurrent` and `runTest`'s timeout instead. CI ran the `iris-core` tests on the iOS simulator for the first time (run 37306657833).
- A11 done: `IrisApp()` (empty screen) in `iris-ui`, tested by `EmptyScreenTest` (Compose UI test on the desktop JVM). `iris-cli --version` prints `iris 0.1.0-dev` (`CliVersionTest`; `version=0.1.0-dev` in `gradle.properties` is a placeholder). Verified by running each app: desktop window on this Linux machine (XWayland), Android on an API 36 emulator, iOS on CI (hand-written Xcode project in `app-ios/iosApp`, built with `xcodebuild`, launched on an iPhone simulator, screenshot kept as an artifact; green in run 37326053534). Conventions gained: Compose UI tests in `jvmTest` of compose modules, kotlin-test on JUnit 5 in JVM apps, `--enable-native-access=ALL-UNNAMED` for Skia on JDK 25, PascalCase names for `@Composable` only. T-001 fixed (placeholder adaptive icon). The first iOS build showed Kotlin/Native's debug platform caches at minos 15.0, so the iOS minimum moved to 15 (D-026).
- **Part A complete.**
- S12 done (D-027, resolves P-003): `iris-opc` is built here, extraction-ready for the planned KMP Office library: package `org.appthere.opc`, may depend on no module (rule table and `architecture.md` §1 updated; `verifyArchitecture` enforces it), own error types. Report and API sketch: `docs/spikes/S12-shared-opc.md`. Open from it: Deflate placement (S4) and XML in common code (decide before Phase 1 needs `[Content_Types].xml`, `.rels` and `document.xml`).
- S4 done (D-028): Deflate from the platform zlib through Okio (approved dependency for `iris-io`, added to the catalog when Phase 1 uses it; system zlib on iOS approved); `iris-opc`/`iris-exr` take a codec interface. ZIP64 and raw entry copy are pure Kotlin in `iris-opc`. Measured on 1 GiB of EXR-like tiles: zlib 62-126 MiB/s deflate, 340-445 MiB/s inflate (JVM and Kotlin/Native); pure Kotlin inflate about 7x slower on Kotlin/Native; korlibs-compression does not compress. Raw copy of a 1 GiB package: 0.6 s. Spike ZIP archives (70 000 entries; 4.5 GiB) pass Info-ZIP and Python checks. Report: `docs/spikes/S4-deflate-zip64.md`; code: `spikes/s4-deflate/`.

## Next
1. S5 (EXR feasibility). Needs Kevin's OK to install the OpenEXR command-line tools (BSD-3-Clause, dev-only) for checking our files. Then S10, S8, S7, S11, S1, S2, S6, S3, S9.
2. Kevin: XML in common code (raised by S12): a small in-house reader/writer, or a library such as xmlutil (license and target check first)? Needed by `iris-opc`, `iris-io` and `iris-svg` in Phase 1.
3. Kevin: `architecture.md` §16 says `sealed interface IrisError`. A sealed type in `iris-core` cannot be extended by other modules, so `IrisError` is an open interface and each module defines a sealed hierarchy implementing it. Approve updating §16 to say so?
4. Kevin: the 85% line-coverage floor for engine modules (`docs/dependencies.md`, Kover row) is not enforced yet. Add a Kover verification rule to `iris.kmp.library` for engine modules?
5. Kevin: should accepting a golden be a task (`goldenAccept -Pname=...` moving pending files into `testdata/golden/`), or stay a manual move?
6. Kevin: `iris-ui` is "Compose only" in `architecture.md` §1, but §13 has `CanvasHost` embed native surfaces (Android `SurfaceView`, iOS Metal view) as an `expect`/`actual` composable. Imports in `iris-ui` are not checked until this is settled (S2 may decide where `CanvasHost` lives).
7. S8: add the `iris-pixels` exemption for `TileBuffer` actuals (likely `kotlinx.cinterop.` and `platform.posix.` in `iosMain`) to the rule table, with a test.

## Notes
- CI flake seen once (run 37326053534): the emulator job's system image download failed ("Premature EOF"); a rerun passed. If it recurs, cache the system image/AVD in the emulator job.
- Android status-bar icons are white on the light empty screen (no theme or edge-to-edge handling yet); fix with the real UI theme.
- Version `0.1.0-dev`, bundle id and application id `org.appthere.iris` are placeholders (P-001).
- A clean first `./gradlew check` with all modules took 41 min, mostly downloading Android Lint 32.3.1. Warm runs take under a minute. CI (A8) should cache `~/.gradle/caches` as well as `~/.konan`.
- First Kotlin/Native use downloads the 2.4.20 distribution into `~/.konan` (~300 MB; 21 min on this connection). CI (A8) should cache `~/.konan`.
- On a non-Mac host every KMP module prints the configuration warning "Native task 'iosSimulatorArm64Test' is disabled". Silenced with `kotlin.native.ignoreDisabledTargets=true` in the root `gradle.properties` (Kevin, 2026-10-04). Fixture builds keep the warning, which `IosOnLinuxTest` checks.
- No Android NDK on the dev machine, so AGP packages Compose's `libandroidx.graphics.path.so` unstripped (build message "Unable to strip the following libraries"). Harmless for debug; decide on installing an NDK (or pinning `ndkVersion`) before release builds.
- services.gradle.org downloads time out from the dev machine (Gradle wrapper has a 10 s read timeout). Workaround used: download with `curl --retry`, check the SHA-256, place the zip in `~/.gradle/wrapper/dists/`.
- Kevin's Mac needs Xcode 26.4 for Kotlin 2.4.20 iOS builds.

## Not covered by CI
- Real pens on any platform (S3, S9) and the Wayland tablet protocol: manual checks in `docs/hardware-matrix.md`.
- GPU parity (`iris-render-skia`): hosted runners have no usable GPU; nightly on a machine with a GPU, later.
- Android: the emulator jobs run `connectedCheck`, but no module has device tests yet (the KMP libraries do not enable them).
- iOS: simulator tests only; nothing runs on a physical iPhone or iPad.

## Unverified (could not be tested on the available machine)
- S4 numbers come from this PC (JVM, Kotlin/Native linuxX64). macOS/iOS arm64 and Android devices are not measured; they also use zlib.
- detekt rules that need type resolution do not run on iOS-only code (`iosMain`, `appleMain`): detekt 2.0.0-alpha.6 has no type-resolving task for Kotlin/Native compilations. Plain `detekt` still covers those files.
- iOS linking, frameworks and running iOS tests: compile-only on Linux; CI's macOS job runs them (`iris-core` simulator tests ran in run 37306657833). CI checks that no object in the framework or the app binary needs an iOS newer than 15.0 (`.github/scripts/check-minos.sh`, exemptions in `minos-exempt.txt`; Skiko's prebuilt objects are 14.0, ours and the Kotlin/Native caches 15.0). Xcode 26 ships no iOS 15 simulator, so the oldest supported iOS is not run anywhere yet. Needs macOS CI or Kevin's Mac.
- iOS and macOS: no macOS on the development machine. Evidence comes from Kevin's MacBook Air M1 or macOS CI.
- No iPad available: iPadOS Apple Pencil input (S9) is unverified.
- No pen on Windows, macOS or Linux: those parts of S3 and S9 are partial (D-022).

## Ignored tests
- None.

## Open TODOs (each must have an id)
- None. (T-001, the missing Android icon, was fixed in A11 with a placeholder icon; real artwork is still to come.)

## Phase reviews
- None yet.
