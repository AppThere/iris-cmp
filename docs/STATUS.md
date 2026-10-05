# Status

Update at the end of every session. Keep it short and true.

## Current phase
Phase 0: Foundations and spikes (in progress)

## Done
- Product spec, architecture, file format, standards, TDD guide, phase plan and decision log written (design only; no code yet).
- `docs/phases/phase-0.md` approved 2026-10-04; its open questions resolved as D-019 to D-022.
- Git repository initialized (branch `main`), pushed to public GitHub repo `AppThere/iris-cmp`; commits use the GitHub noreply email (repo-local git config).
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
  - `iris.app.ios`: Kotlin Multiplatform with only `iosArm64` and `iosSimulatorArm64`, each with a static `IrisApp` framework (debug and release) for Xcode's `embedAndSignAppleFrameworkForXcode`; Compose compiler, runtime and UI. Minimum iOS 14.0 (D-024) on every iOS binary, in libraries and the app. The Xcode project comes with A11.
- A3 done: 23 module skeletons (all of `architecture.md` §1) in `settings.gradle.kts`. Each has a build script with its convention plugin and only the allowed project dependencies, and a README (package, plugin, dependency rule). No sources yet. `iris-testing` declares no dependencies until A10; the apps depend on `iris-ui`, `iris-editor`, `iris-render-skia` and the three platform modules. `./gradlew check` is green across all modules.
- A4 done: `ArchitectureTest` (unit tests on the rule table and checker), `ArchitectureTableSyncTest` (rule table equals `architecture.md` §1), and `verifyArchitecture` from the root plugin `iris.architecture`, run by `check`. It checks declared project dependencies (main and test scope) and Kotlin imports. Engine modules and `iris-testing` may not import `android.`, `androidx.`, `java.awt.`, `javax.swing.`, `javafx.`, `platform.`, `org.jetbrains.skia.`, `org.jetbrains.skiko.`, `org.jetbrains.compose.` or `kotlinx.cinterop.`. `iris-testing` is allowed in test scope only. Checked against the real repo with a temporary violation in `iris-core`.
- A5 done: `verifySizeLimits` per module (400 lines, 600 in test source sets; `testdata/` and `generated/` exempt), in `check`; `./gradlew verifySizeLimits` runs it everywhere. detekt config ships in `build-logic` (`detekt.yml`): function length 50, complexity 12, nesting 4, parameters 7, public functions per class 20. `DetektLimitsFixtureTest` pins each limit at its boundary. `check` also runs the per-compilation detekt tasks (`detektMainJvm`, `detektTestJvm`, `detektMainAndroid`, `detektHostTestAndroid`; `detektMain`/`detektTest` in JVM apps), because plain `detekt` has no type resolution and skips rules such as `LongParameterList`.
- A6 done: `licenseCheck` per module, in `check` (`./gradlew licenseCheck` runs all). Shipped scope: `runtimeClasspath`, `<target>RuntimeClasspath`, `releaseRuntimeClasspath` and `<target>CompileKlibraries`; test scope: their test counterparts. Not checked: `debug*` and the desktop hot-reload configurations (`dev*`, `composeHotReload*`), which are never distributed. POM licenses follow parent POMs and map to SPDX ids; a bare "BSD License" is treated as unrecognized. First real run: 234 components, all shipped ones Apache-2.0; test-only JUnit 4 (EPL-1.0) and Hamcrest (New BSD). Reports in `<module>/build/reports/licenses/dependencies.txt`.
- A7 done: root plugin `iris.golden` with `goldenUpdate -Pname=<pattern>` (candidates from `<module>/build/golden-candidates/` to `testdata/golden-pending/<module>/`) and `goldenReview` (`add`/`change`/`same` against `testdata/golden/`). Byte-level, so format-agnostic. Workflow documented in `docs/testing-tdd.md` section 4; the A10 harness must write candidates there.

## Next
1. A8: workflow written (`.github/workflows/ci.yml`), not yet run: needs a push to GitHub. Its exit test is a green run.
2. Kevin: should accepting a golden be a task (`goldenAccept -Pname=...` moving pending files into `testdata/golden/`), or stay a manual move?
3. Kevin: `iris-ui` is "Compose only" in `architecture.md` §1, but §13 has `CanvasHost` embed native surfaces (Android `SurfaceView`, iOS Metal view) as an `expect`/`actual` composable. Imports in `iris-ui` are not checked until this is settled (S2 may decide where `CanvasHost` lives).
4. S8: add the `iris-pixels` exemption for `TileBuffer` actuals (likely `kotlinx.cinterop.` and `platform.posix.` in `iosMain`) to the rule table, with a test.

## Notes
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
- detekt rules that need type resolution do not run on iOS-only code (`iosMain`, `appleMain`): detekt 2.0.0-alpha.6 has no type-resolving task for Kotlin/Native compilations. Plain `detekt` still covers those files.
- iOS linking, frameworks (`iris.app.ios` framework link tasks are SKIPPED here) and running iOS tests: compile-only on Linux. Also unverified: that linked binaries really carry `minos 14.0` (check with `vtool -show-build` on a Mac), and that the Xcode project's deployment target matches (A11). Needs macOS CI or Kevin's Mac.
- iOS and macOS: no macOS on the development machine. Evidence comes from Kevin's MacBook Air M1 or macOS CI.
- No iPad available: iPadOS Apple Pencil input (S9) is unverified.
- No pen on Windows, macOS or Linux: those parts of S3 and S9 are partial (D-022).

## Ignored tests
- None.

## Open TODOs (each must have an id)
- T-001: `app-android` lint warning MissingApplicationIcon (skeleton manifest has no icon). Fix in A11 with the hello-world app resources.

## Phase reviews
- None yet.
