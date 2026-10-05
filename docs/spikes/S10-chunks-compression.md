# Spike S10: chunk size and EXR compression

Question (`docs/phase-plan.md`, P-004): which chunk size (1024, 2048, 4096) and which EXR compression (ZIP, ZIPS, PIZ)? Exit criteria: save/load time and size for the reference documents at the three chunk sizes.

Status: desktop measurements done 2026-10-05 (12-core Linux PC, NVMe, JDK 25). Tablet numbers come later (hardware matrix). **Decision pending (Kevin)**: the recommended chunk size changes `docs/file-format.md` §5.1, and one finding concerns §10. Code: `spikes/s10-chunks/` (reuses the S5 EXR codec and the S4 ZIP writer).

## 1. Reference documents

No reference documents were defined before this spike; these follow the budgets in `docs/testing-tdd.md` §9:

| Document | Size | Layers | Depth (stored type) | Content |
| --- | --- | --- | --- | --- |
| R1 illustration | 4096 x 4096 | 30 | `8i` (`HALF`) | painted: smooth gradients, little noise; a full background, 4 layers at about 60 %, 10 at 25 %, 15 small details |
| R2 large 16-bit | 10000 x 10000 (100 MP) | 3 | `16i` (`FLOAT`) | photo-like: textured and noisy; full coverage |

Each non-empty chunk is one EXR file (tiled 256, the tile-aligned bounding box of its covered tiles, empty tiles inside as zeros), stored in the package as a STORED ZIP entry. Save and load run in parallel on 12 cores; pixel generation is excluded from the timings. "Incremental" is a single-tile edit: re-encode the one affected chunk (serially) and raw-copy every other entry into a new package (`docs/file-format.md` §10).

## 2. Results

R1 (4096 x 4096, 30 layers, 8-bit):

| Chunk | Compression | Entries | Package | Save | Load | Incremental (chunk encode) |
| --- | --- | --- | --- | --- | --- | --- |
| 1024 | ZIP L4 | 179 | 85 MiB | 1.4 s | 0.6 s | **132 ms** (83 ms) |
| 1024 | ZIP L1 | 179 | 105 MiB | 0.8 s | 0.5 s | 102 ms (46 ms) |
| 1024 | NONE | 179 | 790 MiB | 1.9 s | 0.4 s | 622 ms (12 ms) |
| 2048 | ZIP L4 | 78 | 85 MiB | 1.3 s | 0.5 s | **383 ms** (333 ms) |
| 2048 | ZIP L1 | 78 | 105 MiB | 0.8 s | 0.5 s | 247 ms (182 ms) |
| 2048 | NONE | 78 | 790 MiB | 1.8 s | 0.3 s | 582 ms (25 ms) |
| 4096 | ZIP L4 | 30 | 85 MiB | 1.1 s | 0.6 s | **1371 ms** (1301 ms) |
| 4096 | ZIP L1 | 30 | 105 MiB | 0.7 s | 0.6 s | 804 ms (712 ms) |
| 4096 | NONE | 30 | 790 MiB | 1.8 s | 0.3 s | 796 ms (98 ms) |

R2 (10000 x 10000, 3 layers, 16-bit):

| Chunk | Compression | Entries | Package | Save | Load | Incremental (chunk encode) |
| --- | --- | --- | --- | --- | --- | --- |
| 1024 | ZIP L4 | 300 | 2550 MiB | 17.8 s | 3.7 s | 3195 ms (318 ms) |
| 1024 | ZIP L1 | 300 | 2576 MiB | 14.2 s | 3.6 s | 3106 ms (230 ms) |
| 2048 | ZIP L4 | 75 | 2550 MiB | 17.3 s | 3.4 s | 4401 ms (1267 ms) |
| 2048 | ZIP L1 | 75 | 2576 MiB | 14.0 s | 3.3 s | 2901 ms (895 ms) |
| 4096 | ZIP L4 | 27 | 2550 MiB | 16.6 s | 3.7 s | 6931 ms (5003 ms) |
| 4096 | ZIP L1 | 27 | 2576 MiB | 13.6 s | 3.8 s | 5545 ms (3600 ms) |

Compression methods on one 2048 chunk, through the OpenEXR 3.1 reference tool (`exrmaketiled`, same tool for both, so the times compare fairly):

| Sample | ZIP | PIZ |
| --- | --- | --- |
| R1 painted `HALF`, 32 MiB raw | 3526 KiB (9.3:1), 335 ms | 2532 KiB (12.9:1), 199 ms |
| R2 photo `FLOAT`, 64 MiB raw | 36505 KiB (1.8:1), 1339 ms | 42476 KiB (1.5:1), 953 ms |

The reference tool's ZIP chunk (3526 KiB) matches our ZIP level 4 chunk (3527 KiB). `exrmaketiled` does not offer ZIPS for tiled output: in tiled files each tile is one compression block, so ZIPS and ZIP coincide.

## 3. Findings

1. **Package size does not depend on chunk size** (85 MiB and 2550 MiB at every chunk size). Per-file EXR overhead is negligible.
2. **Chunk size decides the incremental save's encode cost.** The changed chunk is re-encoded whole: 1024 costs 83-318 ms serially, 2048 333-1267 ms, 4096 1.3-5 s. Encoding a chunk's tiles in parallel divides these by the core count, but tablets have fewer and slower cores than this PC.
3. **The package copy is a floor that grows with the document.** Writing a new package and raw-copying unchanged entries (§10) moved about 50 ms of data for R1 (85 MiB) but about 2.9 s for R2 (2.5 GiB), whatever the chunk size. The §1 goal "incremental saves cost time proportional to what changed" holds for encoding, not for IO. The `testing-tdd.md` budget (under 500 ms for a single-tile edit) is met for R1-sized documents and not for R2.
4. **ZIP level**: on painted content level 4 is 19 % smaller than level 1 for about twice the encode CPU; on noisy photo content the levels differ by 1 %. Level 4 is OpenEXR's own default.
5. **PIZ** is 28 % smaller than ZIP on painted `HALF` data but 16 % larger on `FLOAT` photo data, and it needs a substantial pure-Kotlin implementation (wavelet transform plus OpenEXR's Huffman coder).

## 4. Recommendation (P-004)

- **Compression: ZIP, level 4**, for every depth class. Revisit PIZ later as an optional size optimization for `HALF` documents; it is not worth its implementation cost now and loses on `FLOAT`.
- **Chunk size: 1024.** Same package size, the smallest incremental encode (fits the budget even serially on this PC), and four times as many, smaller entries (300 for R2), which the ZIP layer handles easily. This changes `docs/file-format.md` §5.1 (2048 today; the constant `RasterLayout.CHUNK_SIZE` and `layer.xml` already record the value) and §5.2 (`displayWindow` is the chunk's full area). The product should still encode a chunk's tiles in parallel.
- **The package-copy floor** (finding 3) needs a decision of its own. Options:
  - a. Accept it for 1.0: state in §1 that encoding is proportional to the change and IO is one sequential copy of the package (about 1 s per GiB on this PC), and scope the 500 ms budget to R1-sized documents. Measure on the tablet.
  - b. Use copy-on-write file clones where the file system has them (APFS on iOS and macOS, btrfs and XFS reflinks on Linux, ReFS block cloning on Windows) to make the copy near-free, falling back to a plain copy (Android, ext4).
  - c. Append changed entries and a new central directory to the existing file and compact occasionally. The fastest, but it gives up the atomic replace in §10, needs crash recovery for a half-written tail, and may not be possible through Android and iOS document providers.

  Recommended: a now, with b as a later optimization; c is not worth its risks.

## 5. Open questions for Kevin

1. P-004: ZIP level 4 and chunk size 1024 (approve the `file-format.md` §5.1/§5.2 change)?
2. The package-copy floor: a, b or c (and reword `file-format.md` §1 and the `testing-tdd.md` budget accordingly)?
3. Are R1 and R2 the right reference documents for the budgets? They can become fixtures for the Phase 1 performance tests.
