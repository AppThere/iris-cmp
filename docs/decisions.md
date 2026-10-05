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
| D-023 | 2026-10-04 | accepted | Static analysis uses detekt 2.0.0-alpha.6, pinned; `DetektLimitsFixtureTest` guards rule behavior on upgrade. Revisit when detekt 2.0.0 is stable | The stable detekt 1.23.8 does not support Kotlin 2.4 or Gradle 9; 2.0 alphas are built against our stack |
| D-024 | 2026-10-04 | superseded by D-026 | Minimum iOS (and iPadOS) version is 14.0 (Kevin). Pinned as `ios-minVersion` in `libs.versions.toml`; every Kotlin/Native iOS binary gets `-Xoverride-konan-properties=minVersion.ios=14.0` because the Kotlin/Native 2.4.20 default is 15.0. Completes the iOS part of D-004 | Compose Multiplatform 1.12.1 supports iOS 14; the override is documented by Kotlin (native-target-support). Linking and the resulting Mach-O minimum need macOS to verify |
| D-025 | 2026-10-05 | accepted | Android compileSdk is 37 (was 36; Kevin). min SDK stays 28 (D-004) and target SDK stays 36 | Compose Multiplatform 1.12.1's Android artifacts (`androidx.compose.runtime:runtime-saveable-android:1.12.1`) fail AGP's AAR metadata check below 37. AGP 9.3.1 recommends compileSdk up to 37.1 |
| D-026 | 2026-10-05 | accepted | Minimum iOS (and iPadOS) version is 15.0 (Kevin), replacing D-024. `ios-minVersion` stays pinned explicitly on every Kotlin/Native binary. CI's minos check fails if any object needs a newer iOS than 15.0, except archive members listed in `.github/scripts/minos-exempt.txt` (Skiko's code-free ICU data blob, minos 18.5). Older objects are fine (Skiko's prebuilt Skia objects are 14.0) | The first iOS app build (CI run 37316725553) showed Kotlin/Native's prebuilt platform-library caches at minos 15.0 in debug builds, so an iOS 14 debug app linked 15.0 objects. 15.0 is the Kotlin/Native 2.4.20 default; Compose Multiplatform 1.12.1 supports it. Xcode 26 ships no iOS 14 or 15 simulator, so the oldest supported runtime is not exercised in CI |
| D-027 | 2026-10-05 | accepted | The OPC layer is built in this repository as `iris-opc`, ready to be extracted into a library shared with the planned KMP Office library: package `org.appthere.opc`, no dependency on any Iris module, no Iris URIs or relationship types (those live in `iris-io`), its own error and result types (mapped to `IrisError` in `iris-io`). Resolves P-003 | Spike S12 (`docs/spikes/S12-shared-opc.md`): Iris and OOXML need nearly the same OPC core; the Office library does not exist yet, so building it here lets the API settle against Iris's tests first, and extraction becomes a move rather than a rewrite |
| D-028 | 2026-10-05 | accepted | Deflate comes from the platform zlib through Okio (3.18.2, Apache-2.0): `java.util.zip` on JVM and Android, the system zlib on iOS through Kotlin/Native's prebuilt `platform.zlib` bindings (native library approved by Kevin; part of iOS, nothing bundled). `iris-opc` and `iris-exr` each declare the small codec interface they need (deflate; inflate with an output limit) and take an implementation as a parameter; `iris-io` provides the Okio-backed one, so `iris-opc` stays dependency-free (D-027). The ZIP container (ZIP64, raw entry copy) is pure Kotlin in `iris-opc` | Spike S4 (`docs/spikes/S4-deflate-zip64.md`): zlib compresses at 62-126 MiB/s and inflates at 340-445 MiB/s on JVM and Kotlin/Native; pure Kotlin matched the ratio but inflated about 7x slower on Kotlin/Native, which would slow lazy tile loading on mobile. korlibs-compression's compressor does not compress. A pure-Kotlin ZIP64 writer/reader passed Info-ZIP and Python checks, and raw copy of a 1 GiB package took 0.6 s |

## Provisional items and placeholders

| Id | Item | Needed by |
| --- | --- | --- |
| P-001 | Package root `org.appthere.iris`, and the namespace/relationship URIs in `docs/file-format.md` (`appthere.org`, `urn:appthere:iris:1`) are placeholders. Confirm real domain/URN before first public release. All URIs are centralized in `IrisUris` | Before Phase 6 |
| P-002 | iOS and macOS minimum versions | Phase 0 (S1, S2, S9 results) |
| P-003 | Whether `iris-opc` is shared with the planned KMP Office library (OOXML also uses OPC) | Resolved by D-027 (2026-10-05) |
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
