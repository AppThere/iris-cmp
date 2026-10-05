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
- A2 in progress. `build-logic` has a TestKit `functionalTest` suite run by root `./gradlew check` (11 tests, green).
  - `iris.kmp.library`: JVM, Android (min SDK 28), `iosArm64`, `iosSimulatorArm64`, `explicitApi()`, JVM 17 bytecode, warnings as errors, kotlin-test in commonTest.
  - `iris.quality` (applied by `iris.kmp.library`): Spotless with ktlint, detekt over all of `src/`, Kover; all three in `check`.
  - `iris.kmp.compose`: `iris.kmp.library` plus the Compose compiler plugin and the Compose runtime in `commonMain`. Verified on JVM and Android; not the `org.jetbrains.compose` plugin (left for the app conventions, which need packaging and resources).

## Next
1. A2, remaining: `iris.app.desktop`, `iris.app.android`, `iris.app.ios` (test first for each).
2. A2: check whether iOS klibs cross-compile on Linux (phase-0 §4 item 5). So far the iOS targets are only declared; nothing has compiled them.

## Notes
- services.gradle.org downloads time out from the dev machine (Gradle wrapper has a 10 s read timeout). Workaround used: download with `curl --retry`, check the SHA-256, place the zip in `~/.gradle/wrapper/dists/`.
- Kevin's Mac needs Xcode 26.4 for Kotlin 2.4.20 iOS builds.

## Unverified (could not be tested on the available machine)
- iOS and macOS: no macOS on the development machine. Evidence comes from Kevin's MacBook Air M1 or macOS CI.
- No iPad available: iPadOS Apple Pencil input (S9) is unverified.
- No pen on Windows, macOS or Linux: those parts of S3 and S9 are partial (D-022).

## Ignored tests
- None.

## Open TODOs (each must have an id)
- None.

## Phase reviews
- None yet.
