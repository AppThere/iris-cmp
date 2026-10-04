# Phase plan

Phases are gated by exit tests, not dates. Dates depend on available time and are set after Phase 0. Work only in the current phase (see `docs/STATUS.md`). At the start of each phase, write `docs/phases/phase-N.md` with a task breakdown (each task one to a few hours of agent work, each with its failing test named) and get Kevin's approval before coding.

Mapping to the product roadmap: Phase 0 = M0 Spikes; Phases 1 to 3 = Alpha; Phases 4 to 6 = Beta; Phase 7 = 1.0; Phase 8 = Post-1.0. Gates 1 to 4 from the product spec close Phases 0, 3, 6 and 7.

## Phase 0: Foundations and spikes (Gate 1)

Goal: a buildable, tested, linted multi-module project on all targets, and evidence for every open stack decision.

**Part A: scaffold (test-first where there is behavior)**

1. Gradle Kotlin DSL multi-module build, `gradle/libs.versions.toml`, convention plugins in `build-logic/` (KMP library, KMP app, explicit API, detekt, Spotless, Kover, license check). Targets: JVM, Android (min SDK 28), `iosArm64`, `iosSimulatorArm64`. Look up current stable versions; do not guess.
2. `ArchitectureTest` (module dependency rules) written first and failing against a deliberately bad fixture, then passing.
3. `verifySizeLimits`, `licenseCheck`, `goldenReview` tasks with their own tests (use fixture files).
4. CI workflow files per target family (desktop JVM on Linux/Windows/macOS runners, Android emulator, iOS simulator). Document what cannot run in CI.
5. `iris-core` basics driven by tests: ids, `Rect`/`Matrix`/`Point`, `IrisResult`/`IrisError`, `Clock`, `DispatcherProvider`, `Logger`, fractional index with property tests.
6. `iris-testing` skeleton: fakes, builders, golden harness, `ScriptedStylusSource`.
7. Hello-world apps: `app-desktop`, `app-android`, `app-ios` open a window with an empty Compose screen. `iris-cli --version` runs.

**Part B: spikes.** Each spike produces `docs/spikes/S<n>-<name>.md` (question, method, results with numbers and screenshots/logs, recommendation, risks) and a decision-log entry. Spike code is in `spikes/` and may be thrown away.

| Spike | Question | Minimum evidence |
| --- | --- | --- |
| S1 | Android renderer: platform Skia (AGSL on API 33+) or bundled Skiko? What does Compose expose for custom shaders and float surfaces on each target? | A runtime-shader blend and a half-float surface drawing on Android (API 28, 33+), desktop, iOS; list of unavailable APIs |
| S2 | Canvas embedding: how does a native surface sit inside Compose on desktop, Android, iOS, with input delivered off the UI thread? | A pannable surface with raw pointer events logged with timestamps on each target |
| S3 | Linux Wayland: how does a JVM Compose Desktop app get a native Wayland surface and tablet-v2 events? Options: JetBrains Runtime Wayland toolkit (experimental), custom native window, others | A window receiving pressure/tilt on a Wayland compositor, or a documented blocker and fallback |
| S4 | Deflate provider and OPC/ZIP: which multiplatform library or small implementation gives Deflate/Inflate on all targets, and ZIP64 read/write with raw entry copy? | Round trip of 1 GB of EXR-like data; raw copy benchmark |
| S5 | OpenEXR feasibility: write and read tiled `HALF`/`FLOAT` ZIP files in pure Kotlin that the reference tools open | Files written by our code pass `exrheader`/`exrstdattr`-style checks and read in a reference viewer; reference files read by our code; throughput numbers |
| S6 | Text shaping parity: one shaping stack across targets (Skia paragraph/shaper, HarfBuzz bindings, other) | Same string and font laid out on desktop, Android, iOS; glyph positions compared |
| S7 | Color management: in-house ICC subset or wrapped Little CMS (MIT)? | ICC matrix/TRC and LUT transforms compared against a reference; packaging notes for each store |
| S8 | `TileBuffer` native memory per target and persistent collections availability | Allocation, view access, release, leak test on each target; benchmark of a 256 x 256 `Rgba16F` blend loop |
| S9 | Stylus capture per platform: Windows pointer API, macOS tablet events, Android MotionEvent, iOS UITouch | A line with pressure and tilt drawn on each OS available to Kevin; capability report per device |
| S10 | Chunk size and EXR compression choice (ZIP, ZIPS, PIZ) | Benchmark of save/load time and size for the reference documents at chunk sizes 1024, 2048, 4096 |
| S11 | Path boolean quality: platform path ops versus in-house | Test set of tangent, coincident and self-intersecting cases with pass rates |
| S12 | Shared OPC code: should `iris-opc` be the same library as the OPC layer of the planned KMP Office library? | Interface sketch and a decision; do not merge repositories without Kevin's approval |

**Exit gate 1 (tests and artifacts):**

- `./gradlew check` green on every target that CI can run; unrunnable targets listed in `STATUS.md`.
- `ArchitectureTest`, size limits, license check, formatting all enforced.
- All spike reports written; `docs/decisions.md` updated with the outcome of S1 to S12; `docs/file-format.md` adjusted only if a spike requires it (with Kevin's approval).
- Reference devices and performance budgets fixed in `docs/hardware-matrix.md`.

## Phase 1: Core engine and file format (Alpha, part 1)

Goal: headless engine that can create, edit (via commands), save and load an Iris document with raster layers, with no UI.

Order of work (each item starts with failing tests):

1. `iris-pixels`: `PixelFormat`, `TileBuffer` actuals, `TileStore` with copy-on-write, tile conversion between formats, mip pyramid.
2. `iris-color`: color space definitions (sRGB, Display P3, Adobe RGB, Rec. 2020, ProPhoto, linear variants), transfer functions, transforms, premultiplied helpers, Normal blend and the full W3C blend set as pure math with tests against the definitions. ICC per Spike S7.
3. `iris-model`: `DocumentSnapshot`, layer types (raster, group first; others as data classes), settings, artboards, fractional ordering, `Command`/`Change`, history tree with coalescing.
4. `iris-exr`: reader and writer per `docs/file-format.md` section 5 (tiled, `HALF`/`FLOAT`, ZIP and no compression), header attributes, depth-class mapping, hostile-input limits.
5. `iris-opc`: package reader/writer, content types, relationships, raw entry copy, ZIP64, limits.
6. `iris-io`: `.iris` save/load for raster and group layers, incremental save, lazy load, thumbnail, unknown content preserved, versioned fixtures `testdata/format/v1.0/`.
7. `iris-cli`: `validate <file>`, `info <file>`, `render <file> --out png`, `new` (creates a document).
8. Fuzz harnesses for ZIP, XML, EXR readers.

**Exit gate (named suites):** `FormatRoundTripTest`, `ExrPixelExactnessTest` (every depth class), `IncrementalSaveTest`, `UnknownContentTest`, `HostileInputTest`, `CommandInverseProperties`, `CowTileProperties`, `FractionalIndexProperties`; benchmark: open and save of the reference document within budget.

## Phase 2: Rendering and canvas (Alpha, part 2)

Goal: see and navigate a document on screen on every target, with correct compositing.

1. `iris-render`: render graph, ROI propagation, scheduler, tile cache, CPU kernels (source, transform, Normal blend, group isolation), progress and cancellation.
2. Display pipeline: mip selection, zoom/rotate/pan matrices, display color transform (sRGB first).
3. `iris-render-skia` per S1: `RenderBackend`, GPU composite, surface lifecycle; CPU fallback.
4. `CanvasHost` per S2 on desktop, Android, iOS; viewport gestures; rulers; checkerboard; zoom to fit; navigator data.
5. `iris-ui` shell: adaptive scaffold for compact/medium/expanded, layers panel (view only), menu bar, document tabs, open/save through `iris-platform-files`.
6. Golden scenes for compositing; parity tests CPU versus GPU for Normal blend and transform.

**Exit gate:** `CompositeGoldenTests`, `ViewportMathProperties`, `ParityNormalBlend`, UI semantic tests for the shell, benchmark: pan/zoom 60 fps on the mid-range tablet with 30 layers at 4096 x 4096 (static content).

## Phase 3: Painting (Alpha complete, Gate 2)

Goal: paint with a stylus on every Tier 1 platform, undo, save, reopen.

1. `iris-input`: `StrokeSample`, normalizer, calibration, pressure curve, predictor, `present` bitmask semantics.
2. `iris-platform-input` adapters: Windows (pointer API), macOS, Linux (Wayland per S3, X11 fallback), Android/ChromeOS, iPadOS; mouse and touch for all. Contract suite for all adapters.
3. `iris-brush`: dab engine, parametric round brush with hardness/spacing/flow/opacity, dynamics mapping (pressure, tilt, speed), stabilizers, eraser, stroke buffer, commit command.
4. `iris-editor`: `EditorSession`, tool framework, `GestureArbiter`, paint, eraser, eyedropper, pan/zoom/rotate tool, rectangle/ellipse/lasso selection, move, transform (basic), undo/redo, history panel data.
5. `iris-ui`: tool rail, tool options, color picker, brush panel, history panel, layers panel with add/delete/reorder/opacity/blend (Normal, Multiply, Screen first), modifier dock for touch, palm rejection setting.
6. Recovery slot autosave and background save; mobile lifecycle handling.
7. Stylus test pad and trace recorder; traces from each available device; replay goldens.
8. Hardware matrix run on available pens.

**Exit gate 2:** `StrokeReplayGoldens` for every recorded device trace, `StylusContractSuite` on each target that can run it, `UndoRedoStrokeProperties`, `RecoveryTest`; benchmarks: stroke latency under 25 ms with prediction on the reference tablet, zero-allocation stamping; manual checklist in `docs/hardware-matrix.md` completed and attached.

## Phase 4: Layers and non-destructive editing (Beta, part 1)

1. Full blend mode set in graph and shaders; fill opacity, knockout, isolated groups, clipping, Blend If.
2. Masks: raster, vector (after Phase 5 stub), clipping, luminosity.
3. Adjustment layers: tone and color sets from the spec, curves editor UI, 3D LUT (`.cube`).
4. Filters as smart filters: blur family, sharpen family, noise, stylize, distort, render; each with CPU reference, GPU kernel where useful, parity test, golden scene.
5. Layer effects and styles (shadow, glow, bevel, overlays, stroke).
6. Smart objects: embedded and linked raster/Iris sources, transforms and filters non-destructive, stale-link handling.
7. Full color management: working spaces, embedded profiles, display transform, soft proof; HDR where exposed.
8. Selections complete (magic wand, color range, quick mask, feather/grow/shrink/border/smooth, refine edge, saved channels).
9. Tools: gradient, bucket fill, clone, heal, patch, dodge/burn/sponge, smudge with mixing, symmetry, wrap-around.
10. Mutation testing on core modules; quality pass.

**Exit gate:** blend and filter golden suites complete; `ParityAllNodes`; `ColorRoundTripProperties`; non-destructive edit tests (change a filter parameter, only affected tiles recompute); benchmark budgets re-run.

## Phase 5: Vector and text (Beta, part 2)

1. `iris-vector`: path model, node editing ops, shapes with live parameters, strokes (caps, joins, dashes, alignment, markers, variable width), gradients, boolean ops and offset/simplify (per S11), hit testing, snapping, align/distribute.
2. `iris-svg`: strict writer/reader with `iris:` extensions, tolerant reader, fallback rules from `docs/file-format.md` section 6; foreign-tool fixtures.
3. `.iris` vector layers and vector masks end to end.
4. Text: shaping (per S6), point/area/path text, character and paragraph styles, outlines, embedded font subsets, missing-font substitution.
5. Appearance stacks, symbols and instances, artboards, guides, grids, rulers, pixel preview.
6. Tools: pen, curvature, node, pencil with stabilizer and pressure width, shape tools, knife, scissors, width, blend; text tool.
7. UI: properties panel (contextual), appearance panel, character/paragraph, paths and artboards panels, layout tools.

**Exit gate:** `SvgRoundTripTests` (including foreign fixtures), `PathOpsConformance`, `VectorGoldens`, `ShapeLiveParamProperties`, `TextLayoutParityTests` across targets, vector-layer additions to the `.iris` round trip.

## Phase 6: Interoperability and Beta polish (Gate 3)

1. Import: PNG, JPEG, WebP, GIF frames, BMP, TIFF, OpenEXR (third-party, all compressions), ORA, XCF read, PSD/PSB read, SVG import (tolerant), HEIC/AVIF where decoders exist; import reports.
2. Export: PNG, JPEG, WebP, TIFF, OpenEXR (standalone, linear), ORA, SVG, icons; export presets; multi-scale export; metadata handling.
3. Clipboard and drag/drop; share sheets on mobile.
4. Feature matrix against GIMP, Inkscape, Photoshop, Illustrator in `docs/parity-matrix.md`, each row marked.
5. Accessibility pass (screen readers, keyboard-only, high contrast, reduced motion), localization infrastructure and RTL tests.
6. Full hardware matrix and performance run.

**Exit gate 3:** import fixture corpus with expected import reports; fuzz long-run clean; parity matrix reviewed by Kevin; accessibility checklist; all Phase 1 to 5 gates still green.

## Phase 7: 1.0 (Gate 4)

1. Warp, liquify, mesh gradient, envelope distort; image trace; magnetic lasso.
2. Actions recorder, batch processing, declarative extension packs, workspaces and keymap presets.
3. PDF export (vector, fonts, CMYK/spot, multi-page from artboards), PDF as smart object (limited), `.ai` best-effort read.
4. Performance tuning to budgets on all reference devices; memory budget tuning; crash recovery hardening.
5. Store packaging for each target (signing, entitlements, privacy declarations with no network), license notices (`NOTICE`, third-party list).
6. Documentation: user guide skeleton, format documentation published from `docs/file-format.md`.

**Exit gate 4:** every success target from the product spec met on every Tier 1 platform (or exception recorded), release candidate passes the full test and hardware matrix, `licenseCheck` clean, `NOTICE` complete.

## Phase 8: Post-1.0 (outline only)

Lua scripting over the headless API (store-policy check first); true CMYK editing; camera RAW develop as smart object; Web (Wasm) build; frame animation question; real-time collaboration using the stable ids and fractional ordering already in the model.
