# Spike S8: `TileBuffer` native memory and persistent collections

Question (`docs/phase-plan.md`, P-006): how does `TileBuffer` hold pixels in native memory on each target, and is a persistent-collections library available? Exit criteria: allocation, view access, release and a leak test on each target; benchmark of a 256 x 256 `Rgba16F` blend loop.

Status: done 2026-10-05 (D-032, D-033). Code: `spikes/s8-tilebuffer/` (KMP: JVM and linuxX64; the JVM classes minus Panama are dexed for ART). Measured on the 12-core Linux PC: HotSpot JDK 25, ART on the API 36 x86_64 emulator (`app_process`), Kotlin/Native linuxX64 release build. iOS and real Android devices are not measured; iOS uses the same Kotlin/Native code generation and `nativeHeap`.

## 1. Blend benchmark

Premultiplied src-over of one 256 x 256 `Rgba16F` tile onto another (half to float through a 64K table, float to half with round-to-nearest-even). The loop is one `inline` function with `inline` accessors, so each backend gets its own specialized loop; all backends produce the same checksum.

| Backend | HotSpot (JVM) | ART (emulator) | Kotlin/Native |
| --- | --- | --- | --- |
| Heap `ShortArray` (baseline only; D-013 forbids heap pixels) | 779 us | 1226 us | 497 us |
| Direct `ByteBuffer`, one `getShort`/`putShort` per sample | 2403 us | 3841 us | n/a |
| Direct `ByteBuffer`, `ShortBuffer` view | 2023 us | 3984 us | n/a |
| Direct `ByteBuffer`, **one 64-bit read/write per pixel** | **1242 us** | **1969 us** | n/a |
| Panama `MemorySegment`, per sample | 1354 us | n/a (no Panama on Android) | n/a |
| Panama `MemorySegment`, **per pixel** | **1150 us** | n/a | n/a |
| `nativeHeap` `ShortVar`, per sample | n/a | n/a | 1065 us |
| `nativeHeap`, **per pixel** (`LongVar`) | n/a | n/a | **649 us** |

Per-sample access to native memory costs 2-3x the heap baseline (bounds and order checks per access). Reading and writing a whole RGBA16F pixel as one 64-bit word brings every native backend to 1.3-1.6x the baseline.

## 2. Allocation, release and leaks

100 000 tiles of 512 KiB (about 50 GB cumulative), every 4 KiB page written, released after use (20 000 on ART):

| Backend | Release | RSS |
| --- | --- | --- |
| HotSpot, Panama confined `Arena` per tile | `close()`, immediate | 301 -> 312 MiB, flat |
| HotSpot, direct `ByteBuffer` through a 64-buffer pool | reuse | flat |
| HotSpot, direct `ByteBuffer` dropped for the GC | GC only | **peak 6.2 GiB**, still about 6 GiB at the end |
| ART, direct `ByteBuffer` through a pool | reuse | flat |
| ART, direct `ByteBuffer` dropped for the GC | GC (native-allocation aware) | peak 152 MiB, then back to 131 MiB |
| Kotlin/Native, `nativeHeap.allocArray` / `free` | explicit | 27 -> 29 MiB, flat |

HotSpot only collects direct buffers when its direct-memory limit (default: the maximum heap) is reached, so unpooled direct buffers on the desktop can hold gigabytes. ART tracks native allocations and reclaims promptly. A first leak test that wrote only one byte per buffer looked clean on ART because untouched pages are mapped lazily; touching every page fixed the measurement.

## 3. Persistent collections

kotlinx.collections.immutable 0.5.2 (JetBrains, Apache-2.0, released 2026-08; JVM, Android via the JVM artifact, iOS, Linux): a persistent hash map of 100 000 tile keys (packed `Long`).

| Runtime | Build 100 000 | Update one entry | Lookup |
| --- | --- | --- | --- |
| HotSpot | 108 ms | 0.7 us | 0.19 us |
| ART (emulator) | 174 ms | 1.8 us | 0.27 us |
| Kotlin/Native | 152 ms | 2.7 us | 0.37 us |

The old version stays unchanged after updates (structural sharing). Fast enough for a document snapshot per command; tile maps change a handful of entries per stroke.

## 4. Decisions

- **D-032 `TileBuffer` backends**: desktop JVM uses Panama `MemorySegment`s with explicit release (needs a JDK 22+ runtime; the desktop app bundles JDK 25); Android uses direct `ByteBuffer`s through a pool bounded by `MemoryBudget`; Kotlin/Native uses `nativeHeap` with explicit `free`. Hot loops access `Rgba16F` (and other 64-bit pixel formats) a pixel at a time; per-sample views remain for convenience. `TileBuffer` stays reference counted and `AutoCloseable` (`docs/architecture.md` §5), and every backend recycles buffers through a pool.
- **D-033 persistent collections**: kotlinx.collections.immutable 0.5.2 for `DocumentSnapshot` and tile maps. Resolves P-006.
- `iris-pixels` gets its planned `ArchitectureTest` exemption: `kotlinx.cinterop.` imports outside `commonMain` (the `nativeHeap` actual). `platform.posix` is not needed.
