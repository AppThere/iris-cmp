# AppThere Iris: instructions for Claude Code

Iris is an Apache 2.0 licensed 2D image editor that keeps raster and vector layers in one non-destructive document. It is written once in Kotlin Multiplatform with Compose Multiplatform UI for Windows 11, macOS, Linux (Wayland first), Android 9+, ChromeOS, iPadOS and iPhone. Pressure-sensitive stylus input is a core feature. There are no AI features of any kind.

The owner (Kevin) is a solo developer. You are the main implementer. Work in small, verified steps.

## Read these first, in this order

1. `docs/STATUS.md`: current phase, what is done, what is next. Read it at the start of every session and update it at the end.
2. `docs/phase-plan.md`: phases, exit gates. Work only inside the current phase.
3. `docs/product-spec.md`: the product specification (export of the Claude Doc; if missing, ask Kevin to add it).
4. `docs/architecture.md`: how the application works, module graph, contracts.
5. `docs/file-format.md`: the `.iris` format (OPC package, OpenEXR raster layers, SVG vector layers).
6. `docs/coding-standards.md` and `docs/testing-tdd.md`: how code and tests are written.
7. `docs/decisions.md`: decisions already made. Do not relitigate them; propose a change as a new entry.

## Non-negotiable rules

1. **Test first.** No production code without a failing test that demands it. Show the failing run before writing the implementation. See `docs/testing-tdd.md`.
2. **No AI features.** No model inference, no ML-based selection, upscaling, denoising or generative anything, local or remote. Classical algorithms are fine.
3. **No network, no telemetry.** The app works fully offline. Crash reports are opt-in and local-first.
4. **Apache 2.0 only.** Every dependency must be Apache-2.0-compatible. No GPL code in the app. LGPL only if it can be dynamically linked on every store. Never copy code from GIMP, Inkscape or other GPL projects. Check the license before adding any dependency, record it in `docs/dependencies.md`, and ask first if unsure.
5. **Platform code stays in adapters.** Platform APIs (AWT, Android, UIKit, Win32, Wayland, Skia surface handles) appear only in `iris-platform-*`, `iris-render-skia` and the `app-*` shells. Engine modules are plain Kotlin and never depend on Compose.
6. **No pixel data on the JVM heap graph.** Pixels live in `TileBuffer` (native memory). No `List<Float>`, no per-pixel objects, no allocation inside per-pixel loops.
7. **Size limits are enforced by CI.** Files up to 400 lines, functions up to 50 lines, cyclomatic complexity up to 12, nesting up to 4. If a limit blocks you, split the code; do not raise the limit.
8. **Phase discipline.** Do not start work from a later phase. Do not mark a phase done until its exit gate tests pass.
9. **Do not invent versions or APIs.** Look up current library versions and API surfaces in official docs, pin them in `gradle/libs.versions.toml`, and note the date checked. If you cannot verify an API exists on a target, write a spike test (see Phase 0) instead of assuming.
10. **Ask before deciding** anything listed under "Stop and ask" below.

## Repository layout

```
iris/
  CLAUDE.md
  docs/                 specs, plans, decisions, spike reports
  build-logic/          Gradle convention plugins
  gradle/libs.versions.toml
  iris-core/            value types, ids, result types, clock, dispatchers, logging interface
  iris-pixels/          TileBuffer, pixel formats, tile grid, copy-on-write tiles
  iris-color/           color spaces, transforms, ICC subset, blend math
  iris-vector/          path model, boolean ops, stroking, gradients
  iris-exr/             OpenEXR codec (pure Kotlin)
  iris-opc/             Open Packaging Conventions reader/writer
  iris-svg/             SVG reader/writer with iris: extensions
  iris-model/           document, layers, effects, selections, commands
  iris-io/              .iris reader/writer, import/export codecs
  iris-render/          render graph, scheduler, tile cache, CPU kernels
  iris-render-skia/     Skia GPU backend (per-target adapters)
  iris-brush/           brush engine, dynamics, stabilizer
  iris-input/           StrokeSample, normalizer, predictor (common part)
  iris-editor/          tools, editor session, history (the headless app)
  iris-ui/              Compose screens and panels
  iris-platform-input/  per-platform stylus adapters
  iris-platform-files/  file access, clipboard, share
  iris-platform-color/  display color profile and HDR queries
  iris-testing/         fixtures, golden harness, stroke replay, fakes
  iris-cli/             headless JVM tool: validate, convert, render
  app-desktop/ app-android/ app-ios/
  testdata/             fixtures and golden images
```

Package root: `org.appthere.iris` (placeholder; Kevin to confirm before the first release, see `docs/decisions.md`).

## Commands

These tasks are created in Phase 0. Keep this section accurate as the build evolves.

```
./gradlew check                # everything below that is cheap enough for every commit
./gradlew allTests             # all targets that can run on this machine
./gradlew :iris-render:jvmTest # one module, JVM target
./gradlew detekt spotlessCheck # static analysis and formatting
./gradlew verifySizeLimits     # file, function, complexity limits
./gradlew licenseCheck         # dependency licenses against the allow-list
./gradlew goldenReview         # list pending golden-image changes for human review
./gradlew benchmark            # performance budgets (not part of check)
```

## Workflow for every task

1. State the task and which acceptance criteria in `docs/phase-plan.md` it serves.
2. Write the failing test. Run it. Show the failure.
3. Write the minimum code to pass. Run the module tests.
4. Refactor with tests green. Run `./gradlew check`.
5. Commit using Conventional Commits (`test:`, `feat:`, `fix:`, `refactor:`, `docs:`, `build:`). One logical change per commit. A test commit may precede its implementation commit.
6. Update `docs/STATUS.md` before ending the session.

## Stop and ask

Stop and ask Kevin before you:

- add or change a dependency, or change a minimum OS version
- change anything in `docs/file-format.md`, a public module API listed in `docs/architecture.md`, or a decision in `docs/decisions.md`
- work around a failing test by weakening it, skipping it or raising a tolerance
- find that the spec, architecture and file-format documents contradict each other
- hit something that needs a native library (cinterop, JNI, Panama)
- need to touch a platform API you cannot test on the current machine (say what you could not verify)

## Definition of done for a task

- Tests written first, passing on every target the module supports and the machine can run.
- `./gradlew check` is green; no new warnings; no `@Ignore` without an issue link.
- Public API has KDoc. New decisions are in `docs/decisions.md`.
- No TODO without an issue reference in `docs/STATUS.md`.
- Unverified platform behavior is listed in `docs/STATUS.md` under "Unverified".

## Quality pass at the end of every phase

Before closing a phase, review the whole phase's code for the smells in `docs/coding-standards.md` section 12 (long methods, god classes, feature envy, primitive obsession, duplicated logic, leaky platform types, hidden global state, swallowed exceptions, blocking calls on the wrong dispatcher). Fix them in a dedicated `refactor:` series and record the review in `docs/STATUS.md`.

## Style of work

- Prefer small modules, immutable data, explicit types at module boundaries.
- Prefer fakes over mocks. Prefer deterministic tests; inject `Clock`, `Random` seeds and dispatchers.
- When you are unsure how a platform behaves, say so and write a probe test or a spike note rather than guessing.
- Keep prose in docs short and specific. Update the docs you change.
