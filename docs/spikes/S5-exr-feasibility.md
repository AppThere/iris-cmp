# Spike S5: OpenEXR feasibility in pure Kotlin

Question (`docs/phase-plan.md`): can we write and read tiled `HALF`/`FLOAT` ZIP files in pure Kotlin that the reference tools open? Exit criteria: files written by our code pass `exrheader`/`exrstdattr`-style checks and read in a reference viewer; reference files read by our code; throughput numbers.

Status: done 2026-10-05 (D-029). Feasible: every reference check passes, bit-exact where the reference preserves bits. Code: `spikes/s5-exr/` (standalone JVM build; zlib through Okio, D-028).

## 1. What the spike implements

- Writer: single-part, `ONE_LEVEL` tiled files; `HALF` and `FLOAT` channels; `NO_COMPRESSION` and `ZIP` (EXR's even/odd byte split and delta predictor, then zlib); partial edge tiles; raw fallback when compression does not help; standard attributes (`channels`, `compression`, `dataWindow`, `displayWindow`, `lineOrder`, `pixelAspectRatio`, `screenWindowCenter`, `screenWindowWidth`, `tiles`).
- Reader: single-part scanline and `ONE_LEVEL` tiled files; `NO_COMPRESSION`, `ZIPS` and `ZIP`; `HALF`, `FLOAT` and `UINT`. Rejects deep, multi-part and subsampled files with a clear error.

## 2. Results so far

| Check | Result |
| --- | --- |
| Our writer, then our reader: 1000x700 RGBA `HALF`, 640x480 RGB `HALF` + Z `FLOAT`; NONE and ZIP; partial edge tiles | bit-exact |
| ImageMagick (OpenEXR 3.1.11) reads our tiled files: RGBA `HALF` ZIP, RGBA `HALF` NONE, RGB `HALF` + Z `FLOAT` ZIP | opens all three; pixels match our decoding within 7.6e-6 (half a step of ImageMagick's 16-bit quantization) |
| Our reader reads files ImageMagick wrote (scanline; `ZIP` with 16-line blocks, `ZIPS`, `NONE`) | pixels match ImageMagick's own decoding within 7.6e-6 |
| `exrheader` and `exrinfo -v` (OpenEXR 3.1.13) on our three files | parse cleanly; report the expected channels, tile sizes and compression |
| `exrmaketiled` decodes every tile of our files with the reference library and re-tiles them (100x60 ZIP; 64x64 NONE); our reader decodes the result | **bit-identical** to our originals (0 differing samples), including the `FLOAT` channel and partial edge tiles |
| `exrmaketiled` re-tiles a file ImageMagick wrote (64x64 ZIP); our reader decodes it | **bit-identical** to our decoding of ImageMagick's scanline file |

Throughput, JVM (JDK 25), single thread, 4096x4096 RGBA `HALF` (128 MiB), 256-pixel tiles:

| Compression | Size | Ratio | Write | Read |
| --- | --- | --- | --- | --- |
| NONE | 128 MiB | 1.00 | 790 MiB/s | 883 MiB/s |
| ZIP level 1 | 30 MiB | 4.23 | 157 MiB/s | 205 MiB/s |
| ZIP level 4 | 28 MiB | 4.47 | 92 MiB/s | 236 MiB/s |
| ZIP level 6 | 27 MiB | 4.63 | 42 MiB/s | 242 MiB/s |

Tiles are independent, so both directions parallelize across cores in the product. The level and chunk size are S10's to choose.

## 3. Conclusion

A pure-Kotlin EXR codec for what Iris writes (tiled `HALF`/`FLOAT`, NONE and ZIP) and for the Phase 1 read set (plus scanline and `UINT`) is small and correct against the reference implementation. `iris-exr` takes this route, with Deflate supplied through the codec interface from D-028. `PIZ`/`ZIPS` adoption is S10's question; reading the other compressions stays Phase 6, as `docs/file-format.md` §5 says.

Carried into Phase 1 as permanent tests: the bit-exact round trip, reference files produced by `exrmaketiled` (checked into `testdata/`, small), and structural checks of our output.

## 4. Reference tools without root

The OpenEXR command-line tools come from Ubuntu's `openexr` package (3.1.13; OpenEXR's BSD-3-Clause license). Installing it needs `sudo`; instead, `apt-get download openexr` and `dpkg -x` unpack the binaries into a scratch directory. They link against the OpenEXR 3.1 libraries already installed with ImageMagick. Dev-only; nothing ships. The package has no `exrcheck`; `exrheader`, `exrinfo`, `exrstdattr` and `exrmaketiled` covered the checks.
