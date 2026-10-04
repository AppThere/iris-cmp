# AppThere Iris: product and technical specification (Kotlin Multiplatform)

Source: the Claude Doc "AppThere Iris: Product and Technical Specification", exported 2026-10-04. Where this file and `architecture.md`, `file-format.md` or `decisions.md` disagree, those documents win (they are newer and more detailed); report the conflict to Kevin.

## 1. Vision, goals and principles

Iris is an Apache 2.0-licensed 2D image editor that treats raster and vector as equals inside one layered, non-destructive document, built once in Kotlin Multiplatform with Compose Multiplatform UI on every target.

**Goals**

- Replace GIMP as the default open-source editor: match its raster feature set, then beat it on UI coherence, non-destructive workflow and touch/stylus support.
- Approach Photoshop and Illustrator usability: adjustment layers, smart objects, live effects, artboards, symbols, and one document that holds pixels, paths and text.
- Make mobile and tablet first-class: phone and tablet layouts are designed, not shrunk from desktop.
- Treat pressure-sensitive stylus input as a core capability on Windows, macOS, Linux, Android, ChromeOS and iPadOS.
- Keep every edit reversible: effects, masks, transforms and vector operations stay editable until the user flattens or rasterizes explicitly.

**Non-goals**

- No AI features: no generative fill, ML-based selection, upscaling, denoising or any model inference, local or remote. Classical algorithms (magic wand, PatchMatch-style fill, Lanczos resampling) are allowed.
- No cloud account or online dependency; fully functional offline.
- No web-first design; a WebAssembly build is later and best-effort.
- No video or animation editing in v1.

**Principles**

1. One document model: a layer can be raster, vector, text, group, adjustment, smart object or reference; all share transform, mask, blend and effect slots.
2. Non-destructive by default; destructive operations are explicit and labelled.
3. Shared code first; platform code is limited to input, windowing, file access, display-color queries and GPU surface integration.
4. Open formats: the native format is documented and versioned; ORA, SVG, PSD and ODG interoperate where feasible.
5. Responsiveness is a feature: canvas interaction never blocks on document-wide work; heavy work is tiled, cancelable and progressive.

**Success targets** (reference hardware fixed in Phase 0)

| Metric | Target |
| --- | --- |
| Pan/zoom/rotate, 4096 x 4096 px, 30 layers | 60 fps on a mid-range tablet |
| Brush stroke latency, stylus tablets | under 25 ms with prediction, under 50 ms without |
| Maximum supported canvas | 100 megapixels, 16-bit per channel, desktop |
| Cold start to blank canvas | under 2 s desktop, under 3 s tablet |
| Crash recovery | no more than 30 s of work lost |

## 2. Platforms and input matrix

Seven targets from one codebase. Stylus data reaches the engine through a platform input adapter, because Compose's common pointer API does not carry tilt, azimuth, hover distance or predicted samples.

| Platform | Runtime | Pen input source | Tier |
| --- | --- | --- | --- |
| Windows 11 | Compose Desktop (JVM) | Windows pointer API (WM_POINTER: pressure, tilt, rotation, barrel, eraser) via FFM or JNA; Wintab fallback for older Wacom drivers | 1 |
| macOS | Compose Desktop (JVM) | NSEvent tablet fields (pressure, tilt, rotation, tangential pressure) via FFM or JNI | 1 |
| Linux | Compose Desktop (JVM) | Wayland tablet-v2 protocol first; XInput2 valuators on X11/XWayland as fallback | 1 (Wayland), 2 (X11 fallback) |
| Android 9+ phone/tablet | Compose Android | MotionEvent stylus axes (pressure, tilt, orientation, distance), historical batches, MotionPredictor; covers USI and S Pen | 1 |
| ChromeOS | Android build on ChromeOS | Same as Android; USI pens report as stylus MotionEvents | 1 |
| iPadOS | Compose iOS (Kotlin/Native) | UITouch force, altitude, azimuth, estimated properties, coalesced and predicted touches, UIPencilInteraction, hover | 1 |
| iPhone | Compose iOS | Touch and finger painting; no Apple Pencil | 2 |
| Web | Compose Wasm | Pointer Events (pressure, tilt, twist, coalesced and predicted events) | 3, post-1.0 |

**Input capability model.** Each stroke sample carries position, timestamp, pressure, tilt, azimuth, twist, hover distance, tool type (pen, eraser end, finger, mouse) and button state. Fields a device lacks are marked absent, never zero-filled, so brush dynamics can fall back sensibly.

**Device-specific features.** Apple Pencil double-tap and squeeze map to configurable actions and hover previews the brush cursor on supported iPads. S Pen and USI barrel buttons default to eyedropper or pan, configurable per device. Mouse and trackpad pressure comes from an optional simulated-velocity curve. Palm rejection: when a pen is in range, finger touches pan and zoom only; a setting can make them draw.

**Form factors.** Layouts adapt to window size classes (compact, medium, expanded), not device type.

## 3. Architecture summary

See `architecture.md` for the full design. In short: a multi-module Kotlin Multiplatform project where every feature is shared code except four thin platform adapters (stylus input, canvas surface, files and clipboard, display color). Targets: desktop (JVM: Windows, macOS, Linux), Android (phone, tablet, ChromeOS), iOS (Kotlin/Native: iPadOS, iPhone), Web (Wasm) later.

- Engine modules are plain Kotlin with no Compose dependency, so the engine runs headless for tests, batch processing and a CLI.
- Skia is the common renderer behind a `RenderBackend` interface (Skiko on desktop/iOS/Wasm, platform Skia on Android unless Spike S1 says otherwise). GPU filters and blend modes are runtime shaders in the SkSL/AGSL common subset; devices below Android API 33 use CPU kernels. CPU kernels are scalar Kotlin, used for export, tests and fallback.
- One shared text shaping stack across targets so layout is identical everywhere.
- Document mutations go through a single document actor; renderer and UI read immutable snapshots.
- Dependency policy: prefer Kotlin Multiplatform libraries; wrap native libraries behind interfaces only where none exists.

## 4. Document model and file format

An Iris document is a tree of layers on one or more artboards plus a shared resource library, saved as one versioned package (`.iris`) that stores parameters and source data, never baked results. The format is an OPC package with OpenEXR raster layers and SVG vector layers; the full definition is in `file-format.md`.

**Document structure**

- Artboards: one or more canvases with size, ppi, color space and bit depth; layers belong to the document and are placed on artboards.
- Layer tree: unlimited group nesting; every node has a stable UUID, fractional-index position, visibility, opacity, blend mode, transform, optional raster mask, optional vector mask, clipping flag and an ordered effect stack.
- Resources: embedded fonts, ICC profiles, brushes, patterns, gradients, swatches, symbols, styles, linked images, referenced by id.
- Saved selections and alpha channels live in the document, not in a layer.

**Layer types**

| Type | Stores | Notes |
| --- | --- | --- |
| Raster | Sparse 256 x 256 tiles, 8/16-bit int or 16/32-bit float | Empty tiles cost nothing; unbounded extents |
| Vector | Path tree, fills, strokes, variable-width profiles, live shapes | Edited as paths; rasterized only on export or request |
| Text | Rich text runs, paragraph styles, text-on-path, font references | Outlines generated at render time |
| Group | Child layers, optional isolation and knockout | Pass-through or isolated compositing |
| Adjustment | Parametric color operation (curves, levels, etc.) | Affects layers below, or clipped to one layer |
| Fill | Solid, gradient, pattern or mesh gradient | Resolution-independent |
| Smart object | Embedded or linked Iris, SVG, PDF or raster source plus transform | Re-renders when source changes |
| Reference | Image shown but excluded from export | For tracing and mood boards |

**Non-destructive graph.** Effects, masks and filters are nodes holding parameters. Rendered tiles are cached per node and invalidated by dirty-region tracking; the cache is disposable and never required to reopen a file.

**Saving and recovery.** Saves are incremental. Autosave writes the same package format to a recovery slot on a timer and on app backgrounding (mandatory on iOS and Android); a command journal is deferred. **Collaboration readiness:** stable layer ids and fractional ordering from v1, though real-time collaboration is out of scope.

## 5. Raster engine

Tile-based, high-bit-depth and GPU-composited, with a CPU path producing identical results for export, tests and devices without a capable GPU.

**Pixel storage:** 256 x 256 sparse copy-on-write tiles; formats RGBA 8-bit, 16-bit integer, 16-bit float, 32-bit float, grayscale and alpha-only; alpha premultiplied in memory and on disk; mip pyramid per layer; editing in a linear or perceptual RGB working space per document; CMYK, Lab and indexed are import/export and soft-proof modes at launch.

**Brush engine:** dab-based; each stroke stamps dabs along a smoothed path into a stroke buffer, then composites into the layer on stroke end. Parameters: size, hardness, flow, opacity, spacing, angle, roundness, scatter, count, jitter, texture, dual brush, color jitter, smudge with paint mixing. Dynamics: any input (pressure, tilt, azimuth, twist, velocity, direction, distance, fade, random) maps through an editable curve to any parameter. Stabilizers: weighted-average, pulled-string, predictive, with delay shown on the cursor. Symmetry: mirror, radial, tiling; wrap-around painting. Brush import: GIMP GBR and GIH first; MyPaint `.myb` and Photoshop ABR best-effort. Tools sharing the engine: paint, erase, smudge, blur, sharpen, dodge, burn, sponge, clone, heal, pattern.

**Selections and masks:** 8-bit masks with rectangle, ellipse, lasso, polygon, magic wand (contiguous or global, tolerance, anti-aliased), color range, path-to-selection and quick-mask painting. Modifiers: add, subtract, intersect, feather, grow, shrink, border, smooth, invert, save/load as channel. Raster, vector, clipping and luminosity masks attach to any layer type.

**Transforms and retouching:** move, scale, rotate, shear, perspective, mesh warp, liquify, applied live to smart objects and baked only for plain raster layers on commit. Resampling: nearest, bilinear, bicubic, Lanczos. Heal and patch use classical gradient-domain blending; a PatchMatch-style fill is a stretch goal and stays deterministic, not a model.

**Large documents:** operations run per tile on a worker pool, report progress, are cancelable, and show partial results as tiles complete.

## 6. Vector engine

Vector layers keep geometry and appearance as editable data and rasterize at display scale each frame.

**Geometry:** cubic Bézier subpaths with smooth or cusp nodes, nonzero or even-odd fill, open or closed; quadratics and arcs convert on import. Live shapes (rectangle with per-corner radii, ellipse, arc, polygon, star, line, spiral) keep parameters until converted to paths. Compound paths and clipping groups are first-class; boolean results stay live (union, subtract, intersect, exclude, divide, trim, outline) until expanded. Path operations: offset, simplify, smooth, reverse, join, split at node, knife, scissors, outline stroke.

**Appearance:** each object has an ordered appearance stack of several fills, strokes and effects. Strokes: width, caps, joins, miter limit, dashes, inside/center/outside alignment, arrowheads, markers, variable-width profiles (pen and pencil can record pressure into width). Fills: solid, linear, radial, conic, diamond, freeform and mesh gradients, tiling patterns, image fills; gradients support wide-gamut stops and interpolation in a chosen color space. Symbols and instances with per-instance overrides.

**Text:** point, area, path and vertical text; OpenType features, variable fonts, bidirectional text, complex scripts; character and paragraph styles, tab stops, columns, baseline grid; convert to outlines on request; missing fonts substitute with a visible warning. Layout must be identical across platforms.

**Vector tools and extras:** pen, curvature pen, node editor, pencil with stabilizer, shape tools, width tool, blend tool, envelope distort, mesh tool, align/distribute with smart guides. Image trace (classical, potrace-style, no ML). Snapping to nodes, paths, guides, grids, pixels and bounds; pixel-preview mode.

**Boolean implementation:** start from platform path-operation primitives behind a common interface; replace with an in-house robust implementation if quality tests on tangent curves, self-intersection and near-coincident edges fail.

## 7. Compositing and non-destructive editing

The document renders through a render graph; each layer, mask, adjustment, filter and effect is a node computing tiles on demand, so any parameter can change at any time and only affected tiles recompute.

- Nodes declare how far they read beyond their output tile; the scheduler computes exactly the input region required.
- Every node has a GPU implementation and a CPU reference implementation; tests compare them within a per-node tolerance; export can force the CPU path.
- Results are cached per node and tile, bounded by a memory budget, evicted by cost and recency.
- Blending runs in the working space or in linear light per document, with the PDF/SVG and CSS compositing definitions as reference behavior.

**Blend modes**

| Family | Modes |
| --- | --- |
| Normal | Normal, Dissolve, Behind, Clear |
| Darken | Darken, Multiply, Color Burn, Linear Burn, Darker Color |
| Lighten | Lighten, Screen, Color Dodge, Linear Dodge (Add), Lighter Color |
| Contrast | Overlay, Soft Light, Hard Light, Vivid Light, Linear Light, Pin Light, Hard Mix |
| Comparative | Difference, Exclusion, Subtract, Divide, Grain Extract, Grain Merge |
| Component | Hue, Saturation, Color, Luminosity |

Also: fill opacity separate from layer opacity, knockout, isolated groups, channel toggles, and Blend If sliders (own or underlying luminosity/channel, with split sliders).

**Adjustments** (adjustment layers, each with mask and blend). Tone: Brightness/Contrast, Levels, Curves (composite and per-channel), Exposure, Shadows/Highlights, Gradient Map, Posterize, Threshold, Invert. Color: Hue/Saturation, Vibrance, Color Balance, Selective Color, Channel Mixer, Black & White, Photo Filter, Temperature/Tint, Color Lookup (3D LUT, `.cube`).

**Filters** (live on any layer, stacked as smart filters). Blur: Gaussian, box, motion, radial, zoom, lens, surface (bilateral), tilt-shift. Sharpen/detail: unsharp mask, high pass, smart sharpen (deconvolution), median, noise reduction (wavelet or non-local means). Stylize: emboss, find edges, halftone, mosaic, oil paint (Kuwahara), posterize edges, film grain, vignette, chromatic aberration. Distort: displacement map, ripple, wave, twirl, pinch, spherize, lens correction. Render: Perlin and fractal clouds, gradient noise, lighting, lens flare. Every filter is an algorithm with explicit parameters; none uses a trained model.

**Layer effects and styles:** drop shadow, inner shadow, outer and inner glow, bevel and emboss, satin, color/gradient/pattern overlay, stroke; apply to raster, vector, text and group layers; reusable styles; scale with the document.

**Smart objects:** embed or link an Iris document, SVG, PDF or raster image; transforms and filters never touch the source; source edits propagate to all instances; stale-link badge; embed or relink in one action. **Masking everywhere:** every adjustment, filter and effect accepts its own mask.

## 8. Tools catalog

Union of everyday tools in GIMP, Photoshop, Inkscape and Illustrator, delivered across Alpha, Beta, 1.0 and Post-1.0. Each tool is one implementation adapting to mouse, touch and stylus.

| Group | Tools | Reference apps | Release |
| --- | --- | --- | --- |
| Selection | Rectangle, ellipse, lasso, polygonal lasso, magic wand, select by color, quick mask | GIMP, Photoshop | Alpha |
| Selection (edge-aware) | Magnetic lasso, color range, refine edge (feather, smooth, shift) | Photoshop | Beta |
| Paint | Brush, pencil, eraser, gradient, bucket fill, pattern stamp, color replacement | All | Alpha |
| Paint (advanced) | Smudge with mixing, mixer brush, symmetry painting, wrap-around painting | Photoshop, Krita-style | Beta |
| Retouch | Clone, heal, spot heal, patch, dodge, burn, sponge, blur and sharpen brushes, red-eye | GIMP, Photoshop | Beta |
| Transform | Move, free transform, perspective, crop with straighten and perspective crop, canvas and artboard resize | All | Alpha |
| Warp | Mesh warp, liquify, puppet-style pin warp, envelope distort | Photoshop, Illustrator | 1.0 |
| Vector drawing | Pen, curvature pen, node editor, pencil with stabilizer, line, rectangle, ellipse, polygon, star, spiral | Illustrator, Inkscape | Beta |
| Vector editing | Knife, scissors, join, width tool, blend tool, shape builder, path offset and simplify, boolean operations | Illustrator, Inkscape | Beta |
| Gradients and meshes | Gradient on objects, freeform gradient, mesh gradient tool | Illustrator, Inkscape | 1.0 |
| Type | Point, area, path and vertical text, character and paragraph panels | All | Beta |
| Utility | Eyedropper (point, average, all layers), measure, count, hand, zoom, rotate view, notes | All | Alpha |
| Layout | Artboards, slices, guides, grids, rulers, smart guides, align and distribute, snapping | Illustrator, Photoshop | Beta |
| Tracing | Image trace (classical, non-ML) | Illustrator, Inkscape | 1.0 |
| Automation | Action recorder, batch processing, export presets, scripting | Photoshop, GIMP | 1.0 |

**Document-wide features:** linear undo/redo, branching history panel, named snapshots, history persistence across sessions (size-capped); multi-scale, multi-format export from artboards and slices with live preview; libraries (swatches, brushes, gradients, patterns, symbols, styles, fonts) importable and shareable; rulers and units (px, pt, mm, cm, in, pica, ppi-aware). **Parity tracking:** the repository keeps a feature matrix against GIMP, Inkscape, Photoshop and Illustrator (`docs/parity-matrix.md`), each row marked not started, in progress, shipped or intentionally skipped.

## 9. Stylus and pointer input pipeline

Input bypasses the UI thread: a native canvas surface receives raw events, a dedicated input thread feeds the brush engine, and Compose sees only the resulting document state.

1. **Capture:** platform adapters read raw events with hardware timestamps, keeping every coalesced or historical sample.
2. **Normalize:** samples become `StrokeSample` (pressure 0..1, tilt and azimuth in radians, twist, hover distance, tool type, buttons); per-device calibration and a user-editable pressure curve apply.
3. **Predict:** OS predictor where it exists (Android motion prediction, iOS predicted touches), else a constant-acceleration predictor. Predicted samples draw to a temporary overlay and are discarded when real samples arrive.
4. **Stabilize:** selected smoothing filters the stream; delay is shown to the user; never applied to the eraser or straight-line modes.
5. **Render in-progress stroke:** dabs stamp into the stroke buffer on the render thread; low-latency OS paths are used where offered.
6. **Commit:** on pen-up the buffer composites into the layer tiles, one history entry is recorded, the overlay clears.

**Gestures and bindings:** two fingers pan, pinch-zoom and rotate; two-finger tap undo and three-finger tap redo (configurable); with a pen in range, fingers never paint by default; pen buttons, eraser end, Apple Pencil double-tap and squeeze and keyboard modifiers share one editable binding table per device; hover shows the brush outline; rotate and mirror canvas are one-gesture actions. **Diagnostics:** a stylus test pad in Settings plots raw and normalized pressure, tilt and rate per device. **Test strategy:** recorded stylus traces replay in CI through the same pipeline with golden-image comparison.

## 10. Color management

Color-managed from the first release: every document has a working space, imported images keep their profile, and the display transform is applied once, at the end of the render graph.

- Working spaces: sRGB, Display P3, Adobe RGB, Rec. 2020, ProPhoto RGB, ACEScg, linear variants, grayscale, any embedded ICC v2/v4 RGB profile. Depths: 8-bit, 16-bit, 16-bit float, 32-bit float (float documents hold values outside 0 to 1).
- On import a profile is honored, assigned or converted per prompt or saved preference; untagged images are treated as sRGB and flagged. Gradients and blends can interpolate in sRGB, linear, Oklab or Oklch.
- Display: the canvas transforms to the display profile of the monitor the window is on and re-transforms when the window moves. ICC on Windows and macOS, color-space info on iOS and Android, best effort on Linux; otherwise assume sRGB and say so in the status bar. Wide-gamut/HDR output where the OS exposes an extended-range surface; SDR tone mapping is a user setting.
- Print: soft proofing with a chosen CMYK or press profile, rendering intent, black point compensation, gamut warning. CMYK and Lab are import/convert/export targets at 1.0; true CMYK editing is post-1.0. Spot colors and overprint preview are tracked for PDF export.
- Color tools: picker in HSV, HSL, Oklch, Lab, RGB, CMYK; swatches, palettes, harmony, gamut-aware warnings; out-of-gamut handling by perceptual or relative colorimetric clipping.
- Implementation: use a vetted color-management library behind a common interface (for example Little CMS through native bindings, subject to Spike S7) rather than writing ICC handling from scratch.

## 11. UI and UX in Compose

All chrome is Compose Multiplatform; one UI codebase adapts to window size class and input method, with the canvas a native surface embedded in it.

| Window class | Typical devices | Layout |
| --- | --- | --- |
| Compact (under 600 dp) | Phones, small split-screen windows | Full-screen canvas, bottom tool rail, contextual toolbar above it, panels as bottom sheets |
| Medium (600 to 840 dp) | Tablets in portrait, foldables | Canvas with one collapsible side panel, floating tool palette |
| Expanded (over 840 dp) | Tablets in landscape, desktop, ChromeOS | Dockable panel groups left and right, tool rail, menu bar, document tabs |

- Workspaces: presets for Paint, Illustrate, Photo and Layout; custom workspaces stored per form factor.
- Panels: Layers, Properties (contextual), Brushes, Color, Swatches, History, Navigator, Paths, Channels, Character, Paragraph, Effects, Libraries, Artboards, Info. Dock, float, tab-group and collapse on desktop; sheets on compact screens.
- Interaction: contextual task bar near the selection; command palette (Ctrl/Cmd+K, search icon on touch); modifier dock for touch (Shift, Ctrl, Alt, Space); keymap presets for GIMP, Photoshop, Illustrator and Inkscape, all remappable; touch handles at 44 to 48 dp, a loupe under the finger, precision mode with cursor offset; canvas gestures never conflict with pen drawing.
- Desktop: native menu bar (global on macOS), multi-window with tear-off tabs, drag and drop, high-DPI and per-monitor scaling, system clipboard for images and SVG.
- Mobile: system document picker and Files integration, save on background, split-screen and multi-window where offered, persistent progress for large operations.
- Visual design: neutral dark theme by default, light and high-contrast themes so UI color never skews color judgment; three density presets (compact, comfortable, touch) following the input device but overridable.
- Compose performance rules: canvas frames never trigger recomposition; panels read immutable snapshots; thumbnails render off the main thread and are cached; long lists are virtualized.

## 12. Import, export and interoperability

Whatever cannot be preserved is reported, never dropped silently.

| Format | Direction | Fidelity notes | Release |
| --- | --- | --- | --- |
| Iris (`.iris`) | Read, write | Full fidelity; OPC with OpenEXR and SVG | Alpha |
| PNG, JPEG, WebP, GIF, BMP | Read, write | 8-bit; 16-bit PNG with a dedicated codec; animated GIF/WebP import as frames | Alpha |
| OpenRaster (`.ora`) | Read, write | Layers, masks, opacity, common blend modes | Alpha |
| SVG | Read, write | Paths, shapes, gradients, text, clips, masks, filter subset; Iris extensions in a private namespace | Alpha (basic), 1.0 (full) |
| PSD / PSB | Read first, write later | Layers, groups, masks, blend modes, text with fallback, mappable adjustment layers; smart objects keep embedded content | Beta (read), 1.0 (write) |
| GIMP XCF | Read | Layers, masks, channels, paths, text layers as raster fallback | Beta |
| PDF | Write, limited read | Vector export with embedded fonts, CMYK and spot colors, multi-page from artboards; read as smart object | 1.0 |
| Adobe Illustrator (`.ai`) | Read (best effort) | Reads the embedded PDF stream; live effects flatten | 1.0 |
| TIFF | Read, write | 8/16/32-bit, ICC, optional layers | 1.0 |
| OpenEXR (standalone) | Read, write | 16/32-bit float, multi-channel, all common compressions on read | 1.0 |
| HEIC, AVIF, JPEG XL | Read, write where supported | Depends on platform decoders; feature-detected | 1.0 |
| Camera RAW | Read | Develop-style non-destructive import as smart object | Post-1.0 |
| Icons (ICO, ICNS), favicon sets | Write | Multi-resolution from an artboard | 1.0 |
| Palettes and brushes | Read, write | GPL, ASE, ACO swatches; GBR/GIH brushes | Beta |

Metadata: EXIF, XMP, IPTC survive by default with one-click strip on export; GPS stripped by default on mobile export. Import reports list every converted, approximated or dropped feature. Clipboard keeps full fidelity between Iris windows; share sheets and drag and drop export flattened PNG or the selected format. Codec strategy: platform codecs cover common formats; 16-bit PNG, TIFF, OpenEXR, JPEG XL and PDF rendering need shared implementations for identical output on every target (Phase 0 spikes).

## 13. Performance, history, extensibility, accessibility and testing

**Performance and memory:** pixel data lives in native memory behind `TileBuffer`; a global memory budget is derived from device memory (conservative on iOS and Android) with cold tiles spilling to a disk scratch store; a prioritized coroutine scheduler (visible tiles, adjacent tiles, background fill) cancels stale work; budgets are tracked in CI per target.

**Undo and history:** every edit is a command; raster edits record changed tiles by reference (copy-on-write); vector and structure edits record structural diffs; parameter drags coalesce; history has a memory cap and spills to disk; it branches rather than discarding redo; named snapshots.

**Extensibility:** 1.0 ships declarative extension packs (shader-based filters with parameter UI schema, brush packs, styles, swatches, workspaces, recorded actions) stored as data. Post-1.0: Lua scripting, embedded and sandboxed, over a stable headless document API; store rules on downloaded code must be checked first.

**Accessibility and localization:** screen-reader semantics on every panel and dialog (TalkBack, VoiceOver, Narrator, NVDA, Orca); keyboard-navigable layer tree; canvas-state announcements; full keyboard operation on desktop; switch control on mobile; scalable text; high-contrast theme; reduced motion; no information by color alone. Externalized strings from day one; right-to-left layouts; locale-aware units and numbers.

**Testing:** see `testing-tdd.md`. Hardware matrix before each release: Surface Pen, Wacom tablets, Apple Pencil (1st, 2nd, Pro), S Pen, USI pens on ChromeOS and Android, mouse-only desktop. **Privacy:** no telemetry by default; crash reports opt-in, local-first, no document content.

## 14. Milestones and open questions

Phases are gated by exit criteria, not dates: M0 Spikes, Alpha, Beta, 1.0, Post-1.0, with four gates (stack questions decided; latency targets met on a test tablet; parity matrix reviewed against GIMP; success targets met on every Tier 1 OS). The detailed plan is `phase-plan.md`.

| Question | Status / proposed default | Why it matters |
| --- | --- | --- |
| Android renderer: platform Skia (runtime shaders need API 33) or bundled Skiko? | Platform Skia with CPU kernels below API 33, unless the spike shows a gap | GPU filter coverage on older Android devices and ChromeOS |
| Pure Kotlin only, or native libraries for color management, TIFF, PDF and hot kernels? | Native libraries behind interfaces, only where no Kotlin Multiplatform option exists (the EXR codec is pure Kotlin) | Build complexity, licensing, store packaging |
| `.iris` container | Decided: OPC package, OpenEXR raster layers, SVG vector layers; AIF stays an import path | Format lock-in |
| Linux pen input | Decided: native Wayland from the start, X11/XWayland fallback; spike picks the windowing route | Linux stylus users on Wayland |
| Shared text shaping stack | One bundled shaper everywhere, validated on Android first | Layout parity between phone and desktop |
| License | Decided: Apache 2.0; no GPL code; LGPL only if dynamically linkable on every store; GIMP/Inkscape code cannot be copied | Constrains libraries and plugin model |
| Minimum OS | Decided: Android 9 (API 28), Windows 11; iOS and macOS set in Phase 0. API 28 lacks runtime shaders (API 33) and front-buffered rendering (API 29), so those devices use CPU kernels and the standard stroke overlay | Device matrix and test budget |
| Scripting after 1.0 | Decided: Lua, sandboxed; confirm store policy before shipping | App Store rules on downloaded code |
| Real-time collaboration | Out of scope for 1.0; stable ids and fractional ordering keep the door open | Avoids a format redesign |
