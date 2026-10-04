# Iris architecture: how the application works

This document is the contract between modules. Code sketches show shape, not final signatures; tests define the final API. Anything marked **Spike** is decided in Phase 0 and then recorded in `docs/decisions.md`.

## 1. Module graph and dependency rules

Arrows point from a module to what it may depend on. Nothing may depend upward or sideways except as listed.

| Module | Responsibility | May depend on | Platform APIs |
| --- | --- | --- | --- |
| `iris-core` | Ids, units, geometry (`Rect`, `Matrix`, `Point`), `Result` types, `Clock`, `DispatcherProvider`, `Logger` interface | none | no |
| `iris-pixels` | `TileBuffer`, `PixelFormat`, tile grid, copy-on-write tiles, mip pyramid, format conversion | core | `TileBuffer` actuals only |
| `iris-color` | Color spaces, transforms, ICC subset, blend math, gamut mapping | core, pixels | no |
| `iris-vector` | Path model, stroking, booleans, gradients, shape library, text layout interface | core | no |
| `iris-exr` | OpenEXR reader/writer | core, pixels | no |
| `iris-opc` | OPC package reader/writer, ZIP, relationships, content types | core | no |
| `iris-svg` | SVG reader/writer with `iris:` extensions | core, vector, color | no |
| `iris-model` | Document, layers, effects, selections, `Command`, `Change` | core, pixels, color, vector | no |
| `iris-io` | `.iris` reader/writer, import/export codecs, import reports | model, exr, opc, svg | no |
| `iris-render` | Render graph, scheduler, tile cache, CPU kernels, `RenderBackend` interface | model, pixels, color, vector | no |
| `iris-render-skia` | Skia GPU `RenderBackend` | render | yes (Skia/Skiko/Android) |
| `iris-input` | `StrokeSample`, normalizer, calibration, predictor (common) | core | no |
| `iris-brush` | Brush engine, dynamics, stabilizers, stroke buffer | core, pixels, color, input | no |
| `iris-editor` | `EditorSession`, tools, history, selection ops, transforms | model, render, brush, input, io | no |
| `iris-ui` | Compose screens, panels, theme, adaptive layouts | editor | Compose only |
| `iris-platform-input` | Stylus adapters per platform | input | yes |
| `iris-platform-files` | File pickers, atomic replace, clipboard, share | core | yes |
| `iris-platform-color` | Display profile, HDR capability | color | yes |
| `iris-testing` | Fixtures, golden harness, stroke replay, fakes | any (test only) | no |
| `iris-cli` | Headless JVM tool | editor, io | JVM only |
| `app-*` | Wire everything, platform entry points | all | yes |

Rules: engine modules (everything above `iris-ui` in the table except `iris-render-skia`) never import Compose or any platform API. A test (`ArchitectureTest` in `build-logic`, run by the `verifyArchitecture` task in `check`; see D-019) parses Gradle dependencies and imports and fails on a violation. Targets for engine modules: JVM (desktop), Android, `iosArm64`, `iosSimulatorArm64`. Windows uses the JVM target; there is no Kotlin/Native Windows target. A Wasm target is not built until post-1.0, but engine code must not block it (no JVM-only APIs in `commonMain`).

## 2. Runtime overview

Four execution contexts, with one-way data flow between them:

```
 input thread ──StrokeSamples──▶ editor actor ──Command──▶ document actor ──snapshot──▶ render workers
      ▲                              │                         │                           │
  platform adapter              tool state                 DocumentSnapshot            tiles + overlays
                                     │                         │                           ▼
                                     └──── UiState (StateFlow) ◀──────────────────  canvas surface
                                                    ▼
                                              Compose UI (main thread)
```

| Context | Owns | Rules |
| --- | --- | --- |
| UI (main thread) | Compose state, panels | Reads `StateFlow<EditorUiState>`. Sends `UiAction`s. Never touches pixels or the document directly |
| Input thread | Raw event capture, normalization, prediction | Allocation-free steady state. Posts samples to the editor actor without blocking |
| Editor actor | `EditorSession`: active tool, selection ops, gesture arbitration | One coroutine, one channel. Processes events in order |
| Document actor | The current `DocumentSnapshot` and history | The only writer. Applies `Command`s in order and publishes a new immutable snapshot |
| Render workers | Tile evaluation, filters, export | Read snapshots only. Cancelable. Run on a bounded pool from `DispatcherProvider` |

The editor and document actors may share a coroutine in the first implementation, but their interfaces stay separate. All dispatchers are injected so tests use a deterministic scheduler.

## 3. Document model, snapshots and ordering

A `DocumentSnapshot` is an immutable value. Updating it returns a new snapshot that shares unchanged structure.

```kotlin
public value class LayerId(public val uuid: Uuid)

public data class DocumentSnapshot(
    val id: DocumentId,
    val settings: DocumentSettings,          // ppi, depth class, working space, blendInLinear
    val artboards: PersistentMap<ArtboardId, Artboard>,
    val layers: PersistentMap<LayerId, Layer>, // flat store; tree is by parentId + index
    val resources: ResourceLibrary,
    val revision: Revision,                    // monotonically increasing per session
)

public sealed interface Layer {
    val id: LayerId; val parent: LayerId?; val index: FractionalIndex
    val common: LayerCommon                    // name, visible, opacity, blend, clip, transform, mask refs, effects
}
// RasterLayer, VectorLayer, TextLayer, GroupLayer, AdjustmentLayer, FillLayer, SmartObjectLayer, ReferenceLayer
```

- Persistent collections: use a multiplatform persistent-collections library (kotlinx.collections.immutable or equivalent; **Spike** checks license and target support).
- **Fractional index**: sibling order is a string key from a base-62 fractional-indexing scheme. Inserting between `a` and `b` generates a key strictly between them; moves rewrite one key. This avoids renumbering, keeps diffs small, and keeps the model ready for CRDT-style collaboration later. Implement it in `iris-core` with property tests (always between, always unique under concurrent generation with distinct site ids).
- Raster pixel data is referenced, not embedded: `RasterLayer.pixels: TileStore` is an immutable handle to copy-on-write tiles (section 5). A new snapshot shares the same tiles until a command replaces some.
- The snapshot has no references to UI, platform or render state.

## 4. Commands and history

```kotlin
public interface Command {
    val label: StringKey                         // for the history panel, localized
    fun apply(doc: DocumentSnapshot, ctx: CommandContext): CommandResult
}
public data class CommandResult(val doc: DocumentSnapshot, val change: Change)
public sealed interface Change { /* knows how to invert and how to describe dirty regions */ }
```

- Every user-visible edit is a `Command`. `Change` records the minimum needed to undo: tile references before/after for raster edits (`TileDelta`), structural diffs for the layer tree and vector edits, parameter before/after for effects.
- Dirty information flows from `Change`: `DirtyRegion(layerId, tileKeys | wholeLayer)`. The render graph and the save pipeline both consume it, so there is one source of truth for "what changed".
- Coalescing: consecutive commands with the same `CoalesceKey` within 750 ms (parameter drags, nudges) merge into one history entry. The window is a setting.
- History is a tree: undo moves up; a new edit after undo creates a branch rather than discarding redo. Named snapshots mark nodes. Memory is capped; old nodes' tile deltas spill to the disk scratch store (section 5) and can be dropped oldest-first with a visible notice.
- Commands must be deterministic given `(doc, ctx)`. Randomness comes from a seeded `Random` in `ctx`.
- Commands are not serialized in v1. (If a command journal is added later, it needs stable command schemas; record that decision first.)

## 5. Tile engine and memory

- A layer's pixels are a sparse grid of 256 x 256 tiles. `TileKey(tx, ty)` uses signed ints, so layers are unbounded.
- `TileBuffer` is an `expect class` backed by native memory (direct `ByteBuffer`/Panama segment on JVM and Android, `nativeHeap` on Native). It is `AutoCloseable`, reference counted, and addressed through typed views (`FloatView`, `HalfView`, `ByteView`). No `ByteArray` fallback in production paths.
- `PixelFormat`: `Rgba8`, `Rgba16`, `Rgba16F`, `Rgba32F`, `Gray8/16/16F/32F`, `Alpha8`. Premultiplied alpha everywhere. The in-memory format of a document's tiles follows its depth class; the EXR stored type is a separate mapping (see `docs/file-format.md`).
- Copy-on-write: a published tile is immutable. Writers call `TileStore.edit { tx, ty -> buffer }` which clones a shared tile on first write and returns the private mutable copy; `freeze()` publishes it. Undo deltas hold references to old tiles, so a stroke on a huge canvas costs only the touched tiles.
- Mip pyramid: tiles at levels 1..N hold box-filtered reductions, built lazily and invalidated by `DirtyRegion`.
- `MemoryBudget`: a global byte budget per platform derived from device memory (conservative on iOS and Android). Cold tiles spill to a scratch directory in app cache and reload on demand. Over-budget requests evict by cost and recency; if a single operation cannot fit it fails with a typed `OutOfMemoryBudget` error, never an `OutOfMemoryError`.
- Empty tiles are not allocated; a missing key means fully transparent.

## 6. Render graph and display pipeline

```kotlin
public interface RenderNode {
    val inputs: List<RenderNode>
    /** Region of each input needed to compute `output`. Blur radius = margin. */
    fun requiredInput(output: IntRect, input: Int): IntRect
    fun evaluate(ctx: EvalContext, output: TileKey, inputs: List<TileRef>): TileRef
}
```

- The graph is built from a snapshot: layer sources, mask nodes, adjustment/filter/effect nodes, blend/composite nodes, group isolation nodes, and a final display-transform node (working space to display profile). The graph is rebuilt incrementally from `DirtyRegion`s; unchanged subgraphs are reused by structural equality of node parameters.
- Every node has a CPU implementation (`iris-render`, scalar Kotlin) and may have a GPU implementation (`iris-render-skia`, runtime shader). A node that has no GPU version always falls back to CPU. For every node with both, a parity test compares them within a declared tolerance.
- The scheduler evaluates tiles in priority order: visible viewport, then a ring around it, then background cache fill. Stale work is canceled when the viewport changes or a parameter edit invalidates it. Evaluation reports progress; the canvas shows lower-resolution mip results first and refines.
- Cache: per `(node identity, tile key, parameters hash)`, bounded by `MemoryBudget`, evicted by cost and recency. The cache is never persisted.
- Export always uses the CPU path unless the user opts into GPU export, so output is bit-stable across platforms (within float determinism rules in `docs/coding-standards.md`).
- Display: the canvas composes visible tiles at the current zoom and rotation. Zoomed-out views sample mip levels. The display color transform is applied last, per tile, on GPU where available.
- `RenderBackend` is the only interface between `iris-render` and Skia:

```kotlin
public interface RenderBackend {
    val capabilities: BackendCapabilities       // float surfaces, runtime shaders, max texture size
    fun createSurface(spec: SurfaceSpec): RenderSurface
    fun gpuKernel(id: KernelId): GpuKernel?     // null => CPU fallback
    fun uploadTile(tile: TileRef): GpuTile
    fun composite(commands: CompositePlan, target: RenderSurface)
}
```

**Spike S1/S2** decides the Android story (platform Skia with AGSL on API 33+, or bundled Skiko) and which Compose APIs are usable for the canvas. Android 9 to 12 devices use CPU kernels and the standard stroke overlay.

## 7. A stroke, end to end

1. The platform adapter delivers raw events (with coalesced history) to the input thread as `RawPenEvent`s.
2. `StrokeNormalizer` converts to `StrokeSample` (document-space `x, y`, `t`, pressure 0..1, tilt, azimuth, twist, hover distance, tool type, buttons). Absent fields are `NaN`-free: samples carry a `present` bitmask. Calibration and the user pressure curve apply here.
3. `Predictor` (OS predictor where available, else constant-acceleration) produces predicted samples flagged `predicted = true`.
4. The editor actor routes samples to the active tool. The paint tool feeds the `Stabilizer`, then the `BrushEngine`.
5. `BrushEngine` stamps dabs into a `StrokeBuffer` (a private sparse tile set at the layer's format) and returns the dirty tile keys. Predicted samples stamp into a separate throwaway overlay buffer, never into the `StrokeBuffer`.
6. The canvas shows `layer tiles + StrokeBuffer (with the stroke's opacity and blend) + prediction overlay`. On platforms with a low-latency path, the live stroke uses it.
7. On pen-up, the paint tool issues `CommitStrokeCommand(layerId, strokeBuffer, blend, opacity, selectionMask)`. The document actor composites the buffer into the layer's tiles (copy-on-write), records a `TileDelta`, publishes a snapshot, and the overlay clears. One stroke is one history entry.
8. If the stroke is canceled (second finger lands and the gesture arbiter takes over, or the OS cancels the touch), the buffer is dropped and nothing is committed.

Determinism: the same sample sequence and the same brush produce the same pixels on the CPU path. Stroke replay tests rely on this.

## 8. Brush engine

- A `BrushDefinition` is data: tip (parametric or image), spacing, size/hardness/flow/opacity/angle/roundness/scatter/count/jitter, texture, dual brush, color dynamics, smudge/mixing mode, and `Dynamics`: a list of `(input, curve, target, amount)` mappings where input is one of pressure, tilt, azimuth, twist, speed, direction, distance, fade, random(seeded).
- The engine is a pure function from `(definition, samples, seed, canvas state)` to dab stamps. Rasterization of a dab is CPU Kotlin first; a GPU dab path is an optimization behind the same interface once benchmarks justify it.
- Stabilizers (weighted average, pulled string, predictive) are separate components so they can be unit tested on sample sequences.
- Symmetry (mirror, radial, tiling) and wrap-around are implemented by expanding each sample into several, before the engine.

## 9. Tools and gestures

```kotlin
public interface Tool {
    val id: ToolId
    fun onActivate(ctx: ToolContext); fun onDeactivate(ctx: ToolContext)
    fun onPointer(event: CanvasPointerEvent, ctx: ToolContext): ToolResult  // down/move/up/hover/cancel
    fun onKey(event: KeyEvent, ctx: ToolContext): ToolResult
    fun overlay(ctx: ToolContext): List<OverlayShape>    // handles, outlines, cursors in doc space
    val options: ToolOptionsModel
}
```

- `ToolContext` exposes the current snapshot (read), a `submit(Command)` function, selection state, viewport transform, snapping service, and a `Preview` channel for non-committed visuals.
- Tools never touch Compose or platform types. Overlay shapes are drawn by the canvas host.
- `GestureArbiter` decides what a pointer sequence means: pen draws; fingers pan/zoom/rotate while a pen is in range; two-finger tap undo and three-finger tap redo (configurable); finger painting is a setting. When the arbiter changes its mind mid-sequence, it cancels the active tool's sequence (`ToolResult.Cancel`).
- Transient modes (hold Space to pan, hold a key for eyedropper) push a temporary tool and pop it on release.
- The modifier dock on touch devices injects the same modifier state a keyboard would.
- Binding table: `(device, button/gesture) -> action`, user-editable, with presets for Apple Pencil double-tap/squeeze, S Pen button, mouse buttons, and keyboard shortcuts including GIMP/Photoshop/Illustrator/Inkscape keymaps.

## 10. Selections

- A selection is an 8-bit single-channel tile set (`SelectionMask`) in document space plus an optional vector outline (for marching ants and path conversion). It lives in the editor session, not in layers, and is saved only when converted to a channel.
- Operations (add, subtract, intersect, feather, grow, shrink, border, smooth, invert) are commands on the mask and are undoable.
- Tools that edit pixels multiply their output by the selection mask at commit.
- Magic wand, color range and flood fill share one region-growing implementation with a tolerance model tested on fixtures. Magnetic lasso uses a classical edge-cost path search.

## 11. Vector editing model

- A vector layer holds a tree of `VectorObject`s: `PathObject`, `ShapeObject` (live parameters), `TextObject`, `GroupObject`, `SymbolInstance`, each with an `AppearanceStack` (ordered fills, strokes, effects).
- Paths: cubic Bézier subpaths, nodes smooth or cusp, fill rule per path. Imported quadratics and arcs convert on load.
- Booleans, offset, simplify and stroke-to-outline sit behind `PathOps`. **Spike**: first implementation uses platform path ops if tests on tangent and near-coincident cases pass; otherwise an in-house robust implementation.
- Hit testing, snapping (nodes, paths, guides, grids, pixels, bounds, smart guides) and alignment live in `iris-vector` as pure functions over geometry, so they are testable without UI.
- Text layout is behind `TextShaper`. One shared shaping stack is required for identical layout on all targets (**Spike S6**).
- Rendering: vector layers rasterize at display scale per frame (tile-based), and at export resolution for raster output. SVG and PDF export keep vectors.

## 12. Color pipeline

- Document settings choose a working space and depth class. Imported images keep their profile; conversion on import is by user choice or saved preference.
- Blending runs in the working space or in linear light per document (`blendInLinear`). The reference behavior for blend modes follows the W3C compositing and blending definitions.
- Order inside the render graph: source (working space) then adjustments, filters, effects, masks, blend then, at the end, the display transform (working to display profile) and optional soft-proof.
- `iris-color` has no platform dependency. ICC support is a subset (matrix/TRC and LUT-based v2/v4 profiles for RGB, gray and CMYK) behind `ColorEngine`. **Spike S7** decides between an in-house subset and a wrapped native library (Little CMS is MIT-licensed).
- Display profile and HDR capability come from `iris-platform-color`; when unavailable assume sRGB and show that in the status bar.

## 13. UI architecture (Compose Multiplatform)

- Unidirectional data flow: `EditorSession` exposes `StateFlow<EditorUiState>` (a UI-oriented projection of the snapshot and tool state: layer tree rows, selected ids, tool options, history rows, navigator thumbnail handle). The UI sends `UiAction`s. Composables are stateless where practical and take state plus callbacks.
- No Compose recomposition per canvas frame. The canvas is an `expect`/`actual` `CanvasHost` composable that embeds a native surface (Android `SurfaceView`, iOS Metal-backed view, desktop Skia canvas). **Spike S2/S3** fixes the embedding mechanism on each target and how input from the surface reaches the input thread.
- Adaptive scaffold by window size class (compact under 600 dp, medium 600-840 dp, expanded over 840 dp): bottom tool rail and sheets, collapsible side panel with floating palette, and dockable panels with a menu bar respectively.
- Panels are registered as `PanelDefinition(id, title, icon, content, defaultPlacement)`. Workspaces are data (panel placement per form factor), saved as JSON in app data.
- Theme tokens: neutral dark default, light and high-contrast variants, three density presets. Touch targets are at least 44 dp on touch input.
- Accessibility: every control has semantics; the layer tree is a navigable list with actions; the canvas announces tool and selection changes; every action is reachable from the command palette.
- Localization: all user-visible strings come from resource files. No string literals in composables. Right-to-left layouts are tested.
- Large lists (layers, fonts, brushes) are lazy. Thumbnails render off the main thread and cache by `(layer, revision)`.

## 14. Platform adapter contracts

These interfaces live in `iris-input`, `iris-platform-files`, `iris-platform-color` and `iris-render-skia`. Each has an abstract contract test suite in `iris-testing` that every platform implementation must pass (run on the targets that can run it; the rest are listed as unverified in `docs/STATUS.md`).

```kotlin
public interface StylusInputSource {
    val capabilities: StylusCapabilities              // pressure, tilt, hover, eraser, rotation, predicted
    val events: Flow<RawPenEvent>                     // hot, buffered, never blocks producer
    fun predicted(horizonMillis: Int): List<RawPenEvent>
}
public interface CanvasSurface {
    fun resize(widthPx: Int, heightPx: Int, scale: Float)
    fun present(plan: CompositePlan)                  // from the render workers
    fun liveStrokeOverlay(): OverlaySurface?          // low-latency path if available
}
public interface FileAccess {
    suspend fun pickToOpen(types: Set<MediaType>): DocumentHandle?
    suspend fun pickToSave(suggestedName: String, type: MediaType): DocumentHandle?
    suspend fun openRead(h: DocumentHandle): RandomAccessSource
    suspend fun replaceContents(h: DocumentHandle, write: suspend (Sink) -> Unit)  // atomic where possible
    suspend fun cacheDir(): Path; suspend fun recoveryDir(): Path
}
public interface DisplayColorProvider {
    fun profileFor(windowId: WindowId): IccProfile?; val hdr: HdrCapability
    val changes: Flow<WindowId>
}
```

Per-platform notes (details are fixed in the spike reports):

- Windows 11: WM_POINTER for pressure, tilt, rotation, eraser, hover; Wintab as an opt-in fallback for legacy Wacom drivers.
- macOS: tablet fields on `NSEvent` for pressure, tilt, rotation.
- Linux: Wayland tablet protocol first; XInput2 on X11/XWayland as fallback. **Spike S3** picks the JVM windowing route to a native Wayland surface.
- Android 9+ and ChromeOS: `MotionEvent` stylus axes with historical batches; platform motion prediction where present; front-buffered rendering where the API level allows.
- iPadOS: `UITouch` force, altitude, azimuth, estimated properties, coalesced and predicted touches; `UIPencilInteraction`; hover where supported. iPhone: touch only.

## 15. Persistence integration

- `SaveCoordinator` listens to `Change`s, accumulates dirty (layer, chunk) sets, and runs the save pipeline from `docs/file-format.md` section 10 on explicit save, on a timer to the recovery slot, and on app backgrounding (mandatory on mobile).
- Open runs the lazy load path; the editor session starts as soon as `document.xml` and the thumbnail are parsed, and tiles stream in through the same tile cache the renderer uses.
- Crash recovery: at launch, the recovery slot is scanned. For each entry newer than its source file, offer restore. Corrupt recovery slots are discarded with a log entry.

## 16. Errors, logging, observability

- Expected failures (bad file, missing font, out of budget) are typed results (`sealed interface IrisError`). Programmer errors use `require`/`check`/`error`. Exceptions never cross module boundaries as control flow.
- `Logger` is an interface in `iris-core`; platform loggers are injected. No logging of document content or file paths in release builds by default.
- Performance counters (frame time, tile eval time, cache hit rate, stroke latency) are exposed through a `Metrics` interface, enabled in debug builds and benchmarks, and shown in a developer overlay.

## 17. Extensibility

- 1.0: declarative extension packs (shader-based filters with parameter UI schema, brush packs, styles, swatches, workspaces, recorded actions). All stored as data; no executable code.
- Post-1.0: Lua scripting (decided), sandboxed, over a stable headless document API (the same API `iris-cli` uses). Store-policy review is required before scripts can be installed from outside the app.
