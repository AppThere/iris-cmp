# Spike S4: Deflate provider and ZIP64 with raw entry copy

Question (`docs/phase-plan.md`): which multiplatform library or small implementation gives Deflate/Inflate on all targets, and ZIP64 read/write with raw entry copy? Exit criteria: round trip of 1 GB of EXR-like data; raw copy benchmark.

Status: done 2026-10-05 (JVM and Kotlin/Native linuxX64 on the Linux dev machine). **Decided: option A (D-028)**, with Okio and the system zlib on iOS approved by Kevin. Code: `spikes/s4-deflate/` (standalone Gradle build, not part of the product; run with `./gradlew -p spikes/s4-deflate jvmRun --args="deflate 1024"`, `jvmRun --args="zip <dir> huge"`, or the `linuxX64` release executable).

## 1. Candidates

| Candidate | License | How it compresses | Status |
| --- | --- | --- | --- |
| Okio 3.18.2 (Square) | Apache-2.0 | JVM/Android: `java.util.zip` (Okio's `Deflater`/`Inflater` are type aliases). Kotlin/Native: the system zlib through Kotlin/Native's prebuilt `platform.zlib` bindings | Active |
| korlibs-compression 6.0.0 | MIT | Pure Kotlin (`DeflatePortable`) | Last release July 2024. **Its compressor does not compress**: output ratio 1.0 (stored blocks only). Its inflater works |
| Own pure Kotlin (spike `MiniDeflate.kt`) | ours | LZ77 hash chains, lazy matching, dynamic Huffman with package-merge length limiting | Spike quality, unoptimized |
| Own `expect`/`actual` over the platform zlib | ours | Same backends as Okio, without the dependency | Not built; same performance as Okio by construction |

kotlinx-io (0.9.1) has no compression module. Okio's `ZipFileSystem` is read-only (no writing, no raw copy), so no candidate covers the ZIP container.

## 2. Deflate results

Data: 256x256x4 half-float tiles (smooth gradient plus noise, a painted tile rather than white noise) after EXR's ZIP predictor, 512 KiB per tile, raw Deflate per tile as EXR and ZIP entries use it. Every tile round-trips byte for byte. All candidates' output inflates with `java.util.zip`, and each inflates `java.util.zip`'s output.

JVM (JDK 25, 1 GiB per row for Okio and korlibs, 256 MiB for the own compressor):

| Codec | Deflate L1 | Deflate L6 | Inflate | Ratio L1 / L6 |
| --- | --- | --- | --- | --- |
| Okio = `java.util.zip` | 119 MiB/s | 62 MiB/s | 405-445 MiB/s | 2.78 / 2.97 |
| korlibs portable | (stores only) | (stores only) | 500 MiB/s on stored blocks | 1.0 / 1.0 |
| Own compressor + korlibs inflate | 61 MiB/s | 35 MiB/s | 135 MiB/s | 2.84 / 2.91 |

Kotlin/Native, linuxX64 release build (256 MiB per row):

| Codec | Deflate L1 | Deflate L6 | Inflate | Ratio L1 / L6 |
| --- | --- | --- | --- | --- |
| Okio = system zlib | 126 MiB/s | 70 MiB/s | 344-394 MiB/s | 2.78 / 2.97 |
| korlibs portable | (stores only) | (stores only) | 105 MiB/s on stored blocks | 1.0 / 1.0 |
| Own compressor + korlibs inflate | 42 MiB/s | 22 MiB/s | **52 MiB/s** | 2.84 / 2.91 |

Reading:

- Pure Kotlin matches zlib's ratio but not its speed: on Kotlin/Native it compresses at about a third and **inflates at about a seventh** of zlib's speed. Inflation is on the hot path: tiles decode lazily as the viewport needs them (`docs/file-format.md` §10). At 52 MiB/s, 40 visible 512 KiB tiles take about 0.4 s; with zlib about 0.05 s. A tuned own inflater would narrow the gap, not close it, and iPad and Android CPUs are slower than this PC.
- The system zlib performs the same on the JVM and on Kotlin/Native.
- Not measured: macOS/iOS arm64 (needs a macOS CI run) and Android devices. Apple and Android both ship zlib, so the zlib route should behave like the numbers above.

## 3. ZIP64 and raw copy results (JVM, pure Kotlin spike writer and reader)

| Experiment | Result |
| --- | --- |
| Write 1 GiB package: 2048 STORED 512 KiB tile entries + 64 DEFLATED XML parts | 2.2 s (CRC included) |
| **Raw copy** of all 2112 entries into a new package, one tile replaced, no recompression | **0.6 s** (about 1.7 GiB/s; file IO bound). Every CRC checks on reread |
| 70 000 entries (ZIP64 entry count) | writes and rereads |
| 4.5 GiB package, 72 x 64 MiB entries (ZIP64 offsets; last entry at 4 764 732 174) | 10 s; rereads, CRC ok |
| Our archives checked by Info-ZIP `unzip -t` and Python `zipfile.testzip()` | all four pass (1 GiB, copy, 70 000 entries, 4.5 GiB) |
| Archives from Info-ZIP `zip -9` and Python with `force_zip64` read by our reader | pass, all CRCs ok |

Writer properties confirmed: sizes and CRC in local headers (no data descriptors, as `file-format.md` §2 requires), ZIP64 extra fields only for overflowing values, ZIP64 end records only when needed, fixed DOS timestamps (deterministic output). Not covered: a single entry over 4 GiB (Iris entries are chunks and XML parts; the spike holds entries in a `ByteArray`). The product reader also needs data-descriptor reading for archives other tools wrote (the spike does not).

Conclusion for the container: a pure-Kotlin ZIP layer is small and fully adequate; it belongs in `iris-opc` (D-027) with no dependency.

## 4. Options for the Deflate provider

| | A. System zlib through Okio | B. System zlib through our own `expect`/`actual` | C. Pure Kotlin |
| --- | --- | --- | --- |
| Speed | zlib everywhere | zlib everywhere | Kotlin/Native inflate about 7x slower |
| New dependency | Okio (Apache-2.0, active, widely used) | none | none |
| Native library (stop-and-ask) | yes: Apple's system `libz` on iOS (part of the OS; zlib license; nothing bundled), via Kotlin/Native's prebuilt bindings | same, plus our own `kotlinx.cinterop` glue (pinning, `z_stream`) | none |
| Our code | a thin adapter | about 150 lines of platform glue per backend, plus tests | about 600-800 lines (deflate and inflate), fuzzing, tuning |
| Same compressed bytes on every target | no (zlib versions differ; not required, since unchanged entries are raw-copied) | no | yes |
| `ArchitectureTest` impact | none (Okio types stay inside the adapter) | needs an exemption for `platform.zlib`/`kotlinx.cinterop` in the module holding the glue | none |

## 5. Recommendation

**A: system zlib through Okio**, wrapped behind the small `Deflater` interface `docs/file-format.md` §5 asks for, so the provider can be swapped later without touching callers.

Placement, keeping D-027 intact (`iris-opc` depends on no Iris module):

- `iris-opc` and `iris-exr` each declare the two-method codec interface they need (deflate, inflate with an output limit for zip-bomb protection) and take an implementation as a parameter.
- `iris-io`, which already depends on both, provides the Okio-backed implementation and passes it in. No new module, no change to the module table.

This needs two approvals: Okio as a dependency (for `iris-io`), and the system zlib on iOS as a native library.

If you would rather avoid both, **B** gives the same speed with our own glue (and an architecture exemption), and **C** avoids native code entirely at the cost of slow tile loading on mobile.

## 6. Open questions for Kevin

1. A, B or C?
2. If A: approve Okio 3.18.2 (Apache-2.0) as a dependency of `iris-io`?
3. Approve the system zlib on iOS (A or B)? It ships with iOS, is used through Kotlin/Native's own bindings, and adds nothing to the app bundle.
4. Want macOS/iOS arm64 numbers first? A manual CI workflow can run the spike on the macOS runner.
