# Decision log

Append new entries at the bottom. Each entry: id, date, status (`accepted`, `provisional`, `superseded by D-xxx`), decision, reason. Claude Code does not change an accepted entry; it proposes a new one and asks Kevin.

| Id | Date | Status | Decision | Reason |
| --- | --- | --- | --- | --- |
| D-001 | 2026-10 | accepted | Iris is a Kotlin Multiplatform app with Compose Multiplatform UI on all targets | One codebase for desktop, tablet and phone |
| D-002 | 2026-10 | accepted | License: Apache 2.0. Dependencies must be Apache-compatible; no GPL code; LGPL only if dynamically linkable on every store | App store distribution |
| D-003 | 2026-10 | accepted | No AI features: no inference of any kind, local or remote | Product principle |
| D-004 | 2026-10 | accepted | Minimum OS: Android 9 (API 28), Windows 11. iOS and macOS minimums set in Phase 0 | Windows 10 end of support; Android reach. API 28 lacks runtime shaders (API 33) and front-buffered rendering (API 29), so those devices use CPU kernels and the standard stroke overlay |
| D-005 | 2026-10 | accepted | Linux targets native Wayland from the start (tablet protocol), X11/XWayland as fallback. Windowing route decided by Spike S3 | Wayland is the long-term Linux display system. Standard JVM windows run through XWayland, so this is a known technical risk |
| D-006 | 2026-10 | accepted | Post-1.0 scripting language: Lua, sandboxed, over the headless document API | Small, embeddable, MIT licensed. Store policy on user-installed scripts must be checked before shipping |
| D-007 | 2026-10 | accepted | `.iris` is an OPC package: OpenEXR for raster layer chunks, SVG for vector layers, XML for structure (`docs/file-format.md`). AIF from the earlier Rust Iris work is an import path only | Open, inspectable formats using established standards |
| D-008 | 2026-10 | accepted | The OpenEXR codec is implemented in pure Kotlin (`iris-exr`), validated against reference files and tools | No Kotlin Multiplatform EXR library is assumed; avoids native packaging; identical behavior on all targets. Spike S5 confirms feasibility; if it fails, propose a wrapped native library as a new decision |
| D-009 | 2026-10 | accepted | Raster layers are stored in 2048 x 2048 chunks (constant, tunable by Spike S10), each a tiled EXR with 256 x 256 tiles; 8-bit documents store `HALF`, 16-bit integer documents store `FLOAT`; values are stored in working-space encoding; alpha premultiplied | Incremental save granularity, lossless depth mapping, one alpha convention in memory and on disk |
| D-010 | 2026-10 | accepted | Real-time collaboration is out of scope for 1.0; stable layer ids and fractional ordering are required from v1 | Keeps the door open without a format redesign |
| D-011 | 2026-10 | accepted | Autosave/recovery in v1 writes the same package format to a recovery slot; a command journal is deferred | Simpler and reuses the tested save path |
| D-012 | 2026-10 | accepted | Document mutations are serialized through a single document actor; render and UI read immutable snapshots with structural sharing | Predictable concurrency, cheap undo, safe rendering |
| D-013 | 2026-10 | accepted | Pixel data lives in native memory behind `TileBuffer`, never in JVM heap object graphs | Avoids heap limits and GC pressure on desktop and Android |
| D-014 | 2026-10 | accepted | One shared text shaping stack across targets for identical layout | Documents must lay out identically on phone and desktop. Spike S6 validates; Android is the likely trouble spot |
| D-015 | 2026-10 | accepted | Skia is the common renderer behind a `RenderBackend` interface; Android renderer choice (platform Skia vs bundled Skiko) is decided by Spike S1 | Compose's common drawing API is not assumed sufficient for tile compositing, shaders and float surfaces |
| D-016 | 2026-10 | accepted | Extension model for 1.0 is declarative packs (shader filters with parameter schema, brushes, styles, workspaces, actions) | Safe on every store; no executable code |
| D-017 | 2026-10 | accepted | Source files up to 400 lines, functions up to 50 lines, complexity up to 12, enforced in CI | Keeps agent-written code reviewable |
| D-018 | 2026-10 | accepted | Windows uses the JVM target; there is no Kotlin/Native Windows target | Avoids MinGW; matches Kevin's other KMP work |
| D-019 | 2026-10-04 | accepted | `ArchitectureTest` and its rule table live in `build-logic` (JVM unit tests plus a `verifyArchitecture` task wired into `check`), not in `iris-testing` | The check reads the Gradle dependency graph, which only build logic can see |
| D-020 | 2026-10-04 | accepted | `licenseCheck` allow-list: Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, Zlib for shipped code; EPL-1.0/2.0 allowed in test-only configurations | Apache-compatible licenses only in the app (D-002); common test tools such as JUnit are EPL |
| D-021 | 2026-10-04 | accepted | Experimental Kotlin stdlib APIs (for example `kotlin.uuid.Uuid`) may be used if each opt-in sits at a single wrapper site with a comment | Avoids a third-party UUID dependency; keeps the experimental surface in one place |
| D-022 | 2026-10-04 | accepted | Gate 1 accepts a documented partial result for spikes S3 and S9 when pen hardware is not available; completion carries into Phase 3 and is tracked in `STATUS.md` | Stylus hardware may not be available during Phase 0; Phase 3 (painting) is where adapters are built |

## Provisional items and placeholders

| Id | Item | Needed by |
| --- | --- | --- |
| P-001 | Package root `org.appthere.iris`, and the namespace/relationship URIs in `docs/file-format.md` (`appthere.org`, `urn:appthere:iris:1`) are placeholders. Confirm real domain/URN before first public release. All URIs are centralized in `IrisUris` | Before Phase 6 |
| P-002 | iOS and macOS minimum versions | Phase 0 (S1, S2, S9 results) |
| P-003 | Whether `iris-opc` is shared with the planned KMP Office library (OOXML also uses OPC) | Spike S12, Phase 0 |
| P-004 | EXR compression default (ZIP, ZIPS, PIZ) and chunk size | Spike S10, Phase 0 |
| P-005 | Windowing route for native Wayland on JVM | Spike S3, Phase 0 |
| P-006 | Persistent collections library | Spike S8, Phase 0 |
| P-007 | Reference devices for performance budgets | End of Phase 0 |
| P-008 | Store policy for user-installed Lua scripts | Before Phase 8 |
| P-009 | Real license details of fixtures (for example `openexr-images`) before copying into `testdata/` | Phase 1 |

## Entry template

```
### D-0xx: <title>
Date: YYYY-MM-DD   Status: proposed | accepted | superseded by D-0yy
Context: what problem, what constraints, what was measured
Options: the alternatives considered, with the main trade-off of each
Decision: what we chose
Consequences: what becomes easier, what becomes harder, what to revisit and when
```
