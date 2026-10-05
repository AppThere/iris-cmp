# Spike S5: OpenEXR feasibility in pure Kotlin

Question (`docs/phase-plan.md`): can we write and read tiled `HALF`/`FLOAT` ZIP files in pure Kotlin that the reference tools open? Exit criteria: files written by our code pass `exrheader`/`exrstdattr`-style checks and read in a reference viewer; reference files read by our code; throughput numbers.

Status: in progress 2026-10-05. Reference checks through ImageMagick 7.1.2 (built with OpenEXR 3.1.11) done; checks with the OpenEXR command-line tools (`exrheader`, `exrcheck`, `exrmaketiled`) pending their installation. Code: `spikes/s5-exr/` (standalone JVM build; zlib through Okio, D-028).

## 1. What the spike implements

- Writer: single-part, `ONE_LEVEL` tiled files; `HALF` and `FLOAT` channels; `NO_COMPRESSION` and `ZIP` (EXR's even/odd byte split and delta predictor, then zlib); partial edge tiles; raw fallback when compression does not help; standard attributes (`channels`, `compression`, `dataWindow`, `displayWindow`, `lineOrder`, `pixelAspectRatio`, `screenWindowCenter`, `screenWindowWidth`, `tiles`).
- Reader: single-part scanline and `ONE_LEVEL` tiled files; `NO_COMPRESSION`, `ZIPS` and `ZIP`; `HALF`, `FLOAT` and `UINT`. Rejects deep, multi-part and subsampled files with a clear error.

## 2. Results so far

| Check | Result |
| --- | --- |
| Our writer, then our reader: 1000x700 RGBA `HALF`, 640x480 RGB `HALF` + Z `FLOAT`; NONE and ZIP; partial edge tiles | bit-exact |
| ImageMagick (OpenEXR 3.1.11) reads our tiled files: RGBA `HALF` ZIP, RGBA `HALF` NONE, RGB `HALF` + Z `FLOAT` ZIP | opens all three; pixels match our decoding within 7.6e-6 (half a step of ImageMagick's 16-bit quantization) |
| Our reader reads files ImageMagick wrote (scanline; `ZIP` with 16-line blocks, `ZIPS`, `NONE`) | pixels match ImageMagick's own decoding within 7.6e-6 |
| `exrheader`, `exrcheck` on our files; our reader on `exrmaketiled` (tiled) reference files | pending: OpenEXR tools not installed yet |

Throughput, JVM (JDK 25), single thread, 4096x4096 RGBA `HALF` (128 MiB), 256-pixel tiles:

| Compression | Size | Ratio | Write | Read |
| --- | --- | --- | --- | --- |
| NONE | 128 MiB | 1.00 | 790 MiB/s | 883 MiB/s |
| ZIP level 1 | 30 MiB | 4.23 | 157 MiB/s | 205 MiB/s |
| ZIP level 4 | 28 MiB | 4.47 | 92 MiB/s | 236 MiB/s |
| ZIP level 6 | 27 MiB | 4.63 | 42 MiB/s | 242 MiB/s |

Tiles are independent, so both directions parallelize across cores in the product. The level and chunk size are S10's to choose.
