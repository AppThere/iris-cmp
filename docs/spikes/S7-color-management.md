# Spike S7: color management

Question (`docs/phase-plan.md`): an in-house ICC subset or a wrapped Little CMS (MIT)? Exit criteria: ICC matrix/TRC and LUT transforms compared against a reference; packaging notes for each store.

Status: measurements done 2026-10-05. **Decision pending (Kevin)**: the recommendation deviates from `docs/product-spec.md` §10 ("use a vetted color-management library ... rather than writing ICC handling from scratch"), and the alternative is a native library. Code: `spikes/s7-color/` (pure-Kotlin JVM prototype, about 350 lines). Reference: Little CMS 2.17's `transicc` (already installed with `liblcms2-utils`), relative colorimetric intent, double precision.

## 1. Prototype

Reads the ICC header and tag table; `XYZ`, `curv` (identity, gamma, table), `para` (function types 0-4), `lut16` (`mft2`) and `lut8` (`mft1`) with matrix, input curves, CLUT and output curves; the v2 Lab and XYZ PCS encodings. Transforms: matrix/TRC RGB to and from PCS XYZ (inverse curves by bisection), XYZ to Lab (D50), CMYK to Lab through `A2B1`, Lab to CMYK through `B2A0`. CLUT interpolation follows Little CMS: tetrahedral for 3-input device-space tables, two tetrahedral slices blended linearly for 4 inputs, trilinear for Lab input.

Profiles (installed with colord and Ghostscript; used locally, not copied into the repo): v4 sRGB, Adobe RGB (1998), ProPhoto (parametric curves), Rec. 709 (table curve); v2 Ghostscript sRGB and ROMM RGB; v2 FOGRA39 and GRACoL TR006 (CMYK, `lut16`); Ghostscript `default_cmyk` (`lut8` B2A0).

## 2. Agreement with Little CMS

| Transform | Samples | Max difference | Mean |
| --- | --- | --- | --- |
| sRGB (v4) -> ProPhoto | 729 (9^3) | 0.0001 / 255 | 0.00004 |
| sRGB (v4) -> Adobe RGB | 729 | 0.0068 / 255 | 0.0004 |
| Rec. 709 (table curves) -> sRGB | 729 | 0.0051 / 255 | 0.003 |
| sRGB (v2) -> ROMM RGB (v2) | 729 | 0.0314 / 255 | 0.002 |
| FOGRA39 CMYK -> Lab (`lut16` A2B1) | 1296 (6^4) | dE76 0.0060 | 0.0023 |
| GRACoL CMYK -> Lab (`lut16` A2B1) | 1296 | dE76 0.0056 | 0.0023 |
| Lab -> FOGRA39 CMYK (`lut16` B2A0) | 343 | 0.012 % ink | 0.002 |
| Lab -> Ghostscript CMYK (`lut8` B2A0) | 343 | 0.004 % ink | 0.001 |

Two details mattered and are now known: the v2 16-bit Lab encoding (a\*, b\* = value / 256 - 128; a first attempt with /257 gave mean dE 0.69), and Little CMS's interpolation choices (plain multilinear gave CMYK -> Lab dE up to 0.56; tetrahedral everywhere broke Lab -> CMYK by up to 9.5 % ink).

Not covered here: v4 `lutAtoBType`/`lutBtoAType` (`mAB`/`mBA`; no such profile installed), black point compensation, absolute colorimetric (white point scaling), gray `kTRC` (trivial), multiProcessElements (rare). These reuse the same parts (curves, matrices, CLUTs).

## 3. The two routes

| | A. In-house ICC subset (pure Kotlin, `iris-color`) | B. Little CMS through native bindings |
| --- | --- | --- |
| Accuracy | matches Little CMS to rounding on matrix/TRC and v2 LUTs (above) | the reference itself |
| Remaining work | v4 `mAB`/`mBA`, black point compensation, absolute intent, gray, profile writing (for export), a fixture set; estimate 1500-2500 lines with tests | binding layers: Kotlin/Native cinterop (iOS), JNI + C glue (Android), Panama (desktop JVM); a lean Kotlin API over them |
| Native code | none; identical results on every target | yes, on all targets (stop-and-ask) |
| Builds | none | Little CMS built for iOS (device + simulator, as an XCFramework; needs macOS CI), Android (NDK, per ABI: arm64-v8a, armeabi-v7a, x86_64), Windows x64/arm64, macOS universal, Linux x64/arm64 |
| Speed | not critical: the canvas display transform is baked into a GPU 3D LUT or shader (architecture §6), and image conversions can use a baked 33^3 LUT with tetrahedral lookup | fast C, but the same baking applies |
| Maintenance | ours; Little CMS stays a dev-only reference to generate test fixtures | follow upstream releases on 7 native targets |

Packaging notes for route B, per store:

- **App Store (iOS/iPadOS)**: static library inside an XCFramework, linked through cinterop; MIT notice in the app's acknowledgements. Building needs macOS (CI or Kevin's Mac).
- **Google Play (Android, ChromeOS)**: one `.so` per ABI in the APK/AAB (a few hundred KB each), loaded by a JNI wrapper; NDK in the build.
- **Microsoft Store (MSIX)** and direct download: a bundled DLL next to the JVM runtime, called through Panama (JDK 22+, bundled).
- **Mac App Store / notarized macOS**: a signed universal dylib inside the app bundle.
- **Linux (Flatpak, packages)**: Little CMS is in the freedesktop runtime and every distribution; bundle or depend.

Route A needs none of this.

## 4. Recommendation

**Route A, the in-house subset**, with Little CMS as a dev-only reference: its `transicc` generates expected values for a checked-in fixture table, so tests never need it at run time. The prototype shows the subset reaches reference agreement with a few hundred lines; the rest is the same building blocks. It keeps `iris-color` free of platform code (architecture §12) and avoids seven native builds.

This needs Kevin's approval because `docs/product-spec.md` §10 prefers a vetted library over writing ICC handling from scratch. With approval, that line changes to: in-house ICC subset in pure Kotlin, verified against Little CMS fixtures.

## 5. Open questions for Kevin

1. Route A or B?
2. If A: approve changing `product-spec.md` §10 accordingly, and checking in a fixture table generated by `transicc` (values only, no profiles from other projects)?
3. ICC profiles for tests: generate our own working-space profiles (sRGB, Display P3, Rec. 2020 and so on are defined by published primaries and curves) rather than copying third-party profile files; CMYK test profiles need a license check before any is copied into `testdata/`.
