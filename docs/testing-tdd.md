# Test-driven development and test strategy

Tests come first, and they are the specification Claude Code works against. A change that cannot be demonstrated by a test is not finished.

## 1. The loop

For every behavior change:

1. **Red.** Write the smallest test that expresses the next behavior. Run it. Confirm it fails for the right reason (a missing behavior, not a compile error or a typo). Show the failing output in the session.
2. **Green.** Write the least code that passes. No extra features, no speculative generality.
3. **Refactor.** With tests green, remove duplication, improve names, split long functions. Run `./gradlew check`.
4. **Commit.** `test:` and `feat:`/`fix:` commits may be separate or combined, but the test must be in the same pull request and must have failed before the implementation existed.

Rules:

- Bug fixes start with a test that reproduces the bug. Keep it forever.
- Never weaken a test (loosen a tolerance, delete an assertion, add `@Ignore`) to get green. If a test is wrong, say why, change it in its own commit, and tell Kevin.
- `@Ignore` needs an issue reference in `docs/STATUS.md` and is reviewed at every phase close. A phase cannot close with ignored or skipped tests that cover its exit gate.
- Spikes (Phase 0) are the one exception to test-first for exploratory code, but each spike ends with a written report and, where the finding is a requirement, a permanent test that encodes it. Spike code lives in `spikes/` and is not reused without tests.
- Tests are fast by default. A unit test over 200 ms, or a module test run over 60 s on a laptop, is a smell; move slow tests to the `slowTest` suite (nightly) and keep a fast representative in `check`.

## 2. Test pyramid

| Layer | What it covers | Where | Runs |
| --- | --- | --- | --- |
| Unit | Pure logic: geometry, color math, blend math, fractional index, brush dynamics, stabilizers, commands | `commonTest` of each module, all targets | every commit |
| Property-based | Invariants over random inputs: round trips, commutativity, ordering, bounds | `commonTest` with Kotest property testing (verify license and target support in Spike) | every commit (fixed seed) + nightly (random seeds, longer) |
| Contract | Every platform adapter implements the same behavior | abstract suites in `iris-testing`, concrete subclasses in each platform module | per target on CI; unverifiable ones listed in `STATUS.md` |
| Format conformance | EXR, OPC, SVG, `.iris` round trips and reference files | `iris-exr`, `iris-opc`, `iris-svg`, `iris-io` | every commit |
| Golden image | Rendered output of brushes, blends, filters, vector paths, text | `iris-render`, `iris-brush`, `iris-vector` using the golden harness | every commit (CPU path), nightly (GPU path) |
| Parity | GPU result matches CPU reference within tolerance per node | `iris-render-skia` | on machines with GPU, nightly |
| Stroke replay | Recorded stylus traces replayed through input, brush and commit | `iris-editor` | every commit |
| Integration | Open, edit, save, reopen through `EditorSession` headless | `iris-editor`, `iris-cli` | every commit |
| Fuzz | ZIP, XML, EXR, SVG, PSD/XCF readers do not crash or hang | `iris-io` and format modules | short run per commit, long run nightly |
| Performance | Frame time, stroke latency, kernel throughput, open/save time, allocations | `benchmarks/` with `kotlinx-benchmark` | nightly and on demand; budgets gate phase closes |
| UI | Panels, adaptive layouts, semantics, keyboard navigation | Compose Multiplatform UI test in `iris-ui` | every commit on desktop; Android/iOS nightly |
| Manual hardware | Real pens and devices | checklist in `docs/hardware-matrix.md` | before each gate |

## 3. Conventions

- Frameworks: `kotlin.test`, `kotlinx-coroutines-test` (virtual time), Kotest property testing, Compose UI test. Avoid mocking frameworks (they do not work on all targets). Use **fakes**: `FakeClock`, `FakeDispatcherProvider`, `InMemoryFileAccess`, `ScriptedStylusSource`, `FakeRenderBackend`, all in `iris-testing`.
- Structure: arrange, act, assert, with a blank line between. One concept per test. Names describe behavior.
- Test data builders in `iris-testing` (`aDocument { rasterLayer { ... } }`) keep tests short. Builders produce valid objects by default.
- No sleeping, no real time, no real randomness, no real network (there is none). Everything that waits uses virtual time.
- Platform-specific tests live in the platform source set (`androidInstrumentedTest`, `iosTest`, `desktopTest`) and are thin; shared behavior is tested in `commonTest`.
- Test code obeys the same standards as production code except the file limit (600 lines).

## 4. Golden image tests

- The harness renders a scene to an `Rgba32F` tile set, compares to a golden EXR in `testdata/golden/<module>/<name>.exr` with a metric and tolerance declared in the test (`maxChannelDelta`, plus `maxMeanDelta`), and on failure writes `actual`, `expected` and a diff image to `build/golden-failures/`.
- Goldens are updated only by running `./gradlew goldenUpdate -Pname=...`, which creates the change; `./gradlew goldenReview` lists pending changes. A human (Kevin) reviews golden changes. Claude Code never regenerates goldens to make a failing test pass without stating what changed visually and why.
- Scenes are tiny (64 to 512 px) and seeded. Every blend mode, every filter, every brush dynamics mapping and every vector feature has a scene.
- CPU goldens are bit-exact within the stated tolerance on all targets. GPU parity uses a looser, per-node tolerance defined next to the node.

## 5. Stroke replay tests

- `iris-testing` can record a pen session to a `.stroketrace` file (JSON lines of `RawPenEvent`, device capabilities and canvas settings). The recorder is a debug feature in the apps (Settings > Diagnostics > stylus test pad).
- A replay test feeds a trace through `ScriptedStylusSource`, the normalizer, predictor, tool, brush engine and commit, then compares the resulting layer to a golden. Traces from real devices (Surface Pen, Wacom, Apple Pencil, S Pen, USI) live in `testdata/traces/<device>/`.
- New brush behavior needs a trace plus a golden.

## 6. Format tests

Required before a format-related phase can close (see `docs/file-format.md` section 12):

- EXR: bit-exact round trip per depth class (every 8-bit code, sampled 16-bit codes, special float values), compression variants, header attributes, malformed files; reference files from the OpenEXR reference implementation read correctly.
- OPC: content types, relationships, part-name rules, ZIP64, raw-copy of unchanged entries, orphan-part detection.
- SVG: stable writer output, strict and tolerant reader, foreign-tool fixtures, fallback-versus-live-data conflicts.
- `.iris`: round trip of a document that uses every layer type, incremental save changes only dirty entries, unknown content survives, frozen fixtures per released minor version.

## 7. Property tests worth writing early

- Fractional index: always strictly between neighbors, unique across site ids, bounded growth under repeated insertion at one end.
- Color: round trip between spaces within tolerance; blend modes match the W3C definitions on sampled inputs; premultiply/unpremultiply is stable.
- Tiles: copy-on-write isolation (editing a tile never changes any previously published reference).
- Commands: `apply` then inverse returns a structurally equal snapshot; apply then undo then redo equals apply.
- Path ops: boolean results are valid (no self-intersections for simple cases), area identities hold within tolerance.
- Selection: algebra identities (union with empty, intersection with full, double inversion).

## 8. Coverage and mutation

- Coverage target: engine modules at least 85% line coverage on the JVM target, measured by Kover (verify multiplatform support in Phase 0 and record the actual setup). Coverage is a floor, not a goal; do not write assertion-free tests.
- Mutation testing (PIT on JVM) runs on `iris-core`, `iris-pixels`, `iris-color`, `iris-model` from Phase 4. Surviving mutants in those modules are reviewed at phase close.

## 9. Performance budgets

Budgets are measured on the reference devices chosen in Phase 0 (one low-end Android phone, one mid-range Android tablet, one iPad, one Windows 11 laptop, one Linux/Wayland machine, one Mac). Starting targets (from the product spec, to be confirmed by Phase 0 measurements):

| Metric | Budget |
| --- | --- |
| Pan/zoom/rotate, 4096 x 4096, 30 layers | 60 fps on the mid-range tablet |
| Stroke latency with prediction / without | under 25 ms / under 50 ms on stylus tablets |
| Open to first visible canvas (reference 50-layer document) | under 2 s desktop, under 3 s tablet |
| Cold start to blank canvas | under 2 s desktop, under 3 s tablet |
| Max canvas | 100 megapixels at 16-bit on desktop without a memory budget error |
| Steady-state allocation in brush stamping and compositing | zero per frame |
| Incremental save of a single-tile edit | proportional to changed chunks; under 500 ms for the reference document |

A budget miss fails the phase gate unless Kevin approves a recorded exception.

## 10. Phase gates and the TDD discipline

- Each phase in `docs/phase-plan.md` has **exit gate tests**: named test suites or benchmarks. The first task of every phase is to write those as failing (or pending) tests. The phase closes when they pass on every target the machine can run, and `docs/STATUS.md` lists the rest as unverified.
- `./gradlew check` passes at every commit on the main branch. CI runs it per target family.
- When Claude Code works with a local agent setup that locks phases by test runner (for example a TDD enforcer extension), point it at `./gradlew :<module>:jvmTest` for the module under work; the full `check` runs before each commit.

## 11. Test review checklist

1. Does the test fail without the change? (Revert the change locally and see.)
2. Does it test behavior, not implementation details?
3. Is it deterministic (virtual time, seeded randomness)?
4. Is the tolerance justified and as tight as the math allows?
5. Is the failure message useful on its own?
6. Does a contract test exist for every new adapter method?
7. Is the fixture or golden small, documented and license-clean?
