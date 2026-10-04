# The `.iris` file format, version 1

An Iris document is an **Open Packaging Conventions (OPC)** package (the ZIP-based container defined in ISO/IEC 29500-2 and ECMA-376 Part 2). Raster layers are **OpenEXR** images. Vector layers are **SVG** files. Document structure is XML. Nothing in the package is a baked result: effects, masks and adjustments are stored as parameters.

Status: design, not yet implemented. Changes to this file require Kevin's approval (see `CLAUDE.md`).

Placeholders: namespace and relationship-type URIs below use `appthere.org`. Kevin must confirm the real domain before the first public release; until then, keep every URI in one Kotlin object (`IrisUris`) so it can be changed in one place.

## 1. Goals and constraints

- Open and documented: another developer can read a file with a ZIP tool, an XML parser, an EXR reader and an SVG renderer.
- Lossless for pixel data at every supported bit depth.
- Incremental saves cost time proportional to what changed, not to document size.
- Forward compatible: a reader preserves parts and relationships it does not understand when it rewrites a file.
- Safe to open untrusted files (see section 11).

## 2. Package layout

```
/[Content_Types].xml
/_rels/.rels
/docProps/core.xml                       Dublin Core + OPC core properties
/docProps/thumbnail.png                  flattened preview (PNG, max 512 px long edge)
/iris/document.xml                       artboards, layer tree, document settings
/iris/_rels/document.xml.rels            relationships to layers, resources, profiles
/iris/layers/{layerId}/layer.xml         one per layer that owns parts (raster, vector, smart object)
/iris/layers/{layerId}/_rels/layer.xml.rels
/iris/layers/{layerId}/c_{cx}_{cy}.exr   raster chunks (pixel data)
/iris/layers/{layerId}/m_{cx}_{cy}.exr   raster mask chunks (single channel)
/iris/layers/{layerId}/vector.svg        vector content
/iris/layers/{layerId}/vmask.svg         vector mask (optional)
/iris/resources/profiles/{id}.icc        embedded ICC profiles
/iris/resources/fonts/{id}.ttf|otf|woff2 embedded font subsets
/iris/resources/brushes/{id}.xml         brush definitions (and tip images as .exr or .png)
/iris/resources/patterns/{id}.exr
/iris/resources/linked/{id}.*            embedded sources of smart objects
```

Rules:

- Part names are ASCII, begin with `/`, never end with `/`, and are unique ignoring case. `{layerId}` and other ids are lowercase UUIDs (no braces). `{cx}` and `{cy}` are signed decimal integers (`c_-1_0.exr` is valid).
- Every part is reachable through a relationship chain from `/_rels/.rels`. The writer must not produce orphan parts.
- ZIP entries: XML and SVG are Deflate-compressed. `.exr` and `.png` entries are STORED (they are already compressed). ZIP64 is used when needed. No encryption. No data descriptors in files we write (sizes are known). No multi-disk archives.
- Relationship types (all under `http://schemas.appthere.org/iris/2026/relationships/`): `document`, `layer`, `chunk`, `maskchunk`, `vector`, `vectormask`, `profile`, `font`, `brush`, `pattern`, `linked`. The standard OPC types are used for core properties and the thumbnail.
- Content types: `.xml` parts use `application/vnd.appthere.iris.document+xml` (document.xml) and `application/vnd.appthere.iris.layer+xml` (layer.xml) as Override entries. Defaults: `exr` is `image/x-exr`, `svg` is `image/svg+xml`, `png` is `image/png`, `icc` is `application/vnd.iccprofile`, `rels` is `application/vnd.openxmlformats-package.relationships+xml`, `ttf`, `otf`, `woff2` use their IANA font types.
- The package MIME type for the file itself is `application/vnd.appthere.iris`, extension `.iris`.

## 3. `document.xml`

XML, UTF-8, namespace `urn:appthere:iris:1` (prefix `iris`, placeholder URI). The root element carries the format version. Minimal example:

```xml
<iris:document xmlns:iris="urn:appthere:iris:1"
               xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
               formatVersion="1.0" id="3b1f2c0e-...">
  <iris:settings ppi="300" depth="16i" workingSpace="srgb" blendInLinear="false">
    <iris:profile r:id="rIdProfile1"/>      <!-- optional embedded ICC -->
  </iris:settings>
  <iris:artboards>
    <iris:artboard id="a1" name="Main" x="0" y="0" width="4096" height="2304" background="transparent"/>
  </iris:artboards>
  <iris:layers>
    <iris:group id="g1" name="Group 1" index="a0" visible="true" opacity="1" blend="passThrough">
      <iris:raster id="l1" name="Paint" index="a0" visible="true" opacity="0.8" blend="multiply"
                   clip="false" r:id="rIdL1">
        <iris:transform m="1 0 0 1 0 0"/>
        <iris:mask kind="raster" r:id="rIdL1"/>   <!-- mask chunks listed in layer.xml -->
        <iris:effects>
          <iris:effect type="dropShadow" enabled="true">
            <iris:param name="radius" value="8"/>
          </iris:effect>
        </iris:effects>
      </iris:raster>
      <iris:adjustment id="l2" index="a1" type="curves" ...>
        <iris:param name="points.rgb" value="0,0 0.5,0.4 1,1"/>
      </iris:adjustment>
    </iris:group>
  </iris:layers>
  <iris:resources> ... <iris:swatches/> <iris:gradients/> <iris:styles/> </iris:resources>
  <iris:channels> saved selections and alpha channels, by reference </iris:channels>
</iris:document>
```

Rules:

- Layer types: `raster`, `vector`, `text`, `group`, `adjustment`, `fill`, `smartObject`, `reference`.
- `id` is a UUID string that never changes. `index` is a **fractional index** string that orders siblings (see `docs/architecture.md`, section 3). Order in the file is not significant; readers sort by `index`.
- Enumerated values (`blend`, effect types, adjustment types) are lowerCamelCase strings. Unknown values are preserved on rewrite and rendered as `normal` / skipped with a warning.
- Parameters are `iris:param` elements with string values in a documented micro-syntax per parameter (numbers use `.` as decimal separator and no thousands separator, lists are space separated).
- Transforms are six numbers `a b c d e f` of a 2D affine matrix in document pixels.
- Unknown elements and attributes in the `iris:` namespace or in other namespaces are preserved on rewrite.

## 4. `layer.xml` (per layer parts manifest)

```xml
<iris:layerParts xmlns:iris="urn:appthere:iris:1" xmlns:r="..." layerId="l1" kind="raster"
                 chunkSize="2048" tileSize="256" pixelType="half" channels="RGBA">
  <iris:chunk cx="0" cy="0" r:id="rId1" bytes="1834221"/>
  <iris:chunk cx="1" cy="0" r:id="rId2" bytes="402811"/>
  <iris:maskChunk cx="0" cy="0" r:id="rId3"/>
</iris:layerParts>
```

Vector layers instead have `<iris:vector r:id="..."/>` and optional `<iris:vectorMask r:id="..."/>`. Smart objects have `<iris:source r:id="..." kind="iris|svg|raster|pdf" link="optional/path"/>`.

## 5. Raster layers: OpenEXR

### 5.1 Chunks

A raster layer's pixels are split into **chunks** of 2048 x 2048 layer pixels aligned to the layer origin (chunk `(cx, cy)` covers `x` in `[2048*cx, 2048*(cx+1))`). Each non-empty chunk is one EXR file. A chunk with no non-transparent tiles is not written. The chunk size is a named constant (`RasterLayout.CHUNK_SIZE`) so Phase 0 benchmarks can tune it; the value in use is recorded in `layer.xml`.

Why chunks: a save rewrites only the chunks whose tiles changed, and a layer can be unbounded and sparse without one huge file.

### 5.2 EXR requirements for files we write

| Property | Value |
| --- | --- |
| Storage | Tiled, `ONE_LEVEL`, tile size 256 x 256 |
| Compression | `ZIP` by default. Phase 0 benchmarks `PIZ` and `ZIPS`; the choice is logged in `docs/decisions.md`. Scratch/recovery files may use `NO_COMPRESSION` |
| Channels | Color: `R`, `G`, `B`, `A`. Grayscale layers: `Y`, `A`. Masks: `Y` only |
| Pixel type | See 5.3 |
| Alpha | **Premultiplied** (the OpenEXR convention). Memory and disk agree |
| `dataWindow` | Tile-aligned bounding box of non-empty tiles within the chunk, in layer pixel coordinates (can be negative) |
| `displayWindow` | The chunk's full 2048 x 2048 area |
| Empty tiles inside `dataWindow` | Written as all-zero tiles (they compress to a few bytes) so the file never has missing tiles |
| `lineOrder` | `INCREASING_Y` |
| Deep data, multi-part | Not written |

### 5.3 Pixel types by document depth

OpenEXR has no 8-bit or 16-bit integer channels, so the document depth class maps to a stored type:

| Document depth (`iris:settings/@depth`) | EXR pixel type | Notes |
| --- | --- | --- |
| `8i` | `HALF` | Values are `code / 255`. Lossless: half has more than 8 bits of precision across [0, 1]. A property test must prove every code 0..255 round-trips |
| `16i` | `FLOAT` | Values are `code / 65535`. Lossless: float32 round-trips every 16-bit code. Costs 4 bytes per channel before compression |
| `16f` | `HALF` | Native |
| `32f` | `FLOAT` | Native |

Values are stored **in the document's working-space encoding**, not converted to linear. A document in sRGB stores sRGB-encoded values. This keeps edits lossless and cheap. The encoding is recorded in the header (5.4) and in `document.xml`. Consequence: a third-party EXR viewer will show encoded values as if linear. Export to a standalone EXR (Phase 6) converts to linear on request.

### 5.4 Header attributes

Standard: `chromaticities` (working-space primaries and white point, so tools can see the gamut), `compression`, `dataWindow`, `displayWindow`, `tiles`, `channels`.

Custom attributes (name, EXR attribute type), all optional on read, all written by Iris:

| Name | Type | Meaning |
| --- | --- | --- |
| `iris:formatVersion` | string | `1.0` |
| `iris:layerId` | string | Owning layer UUID |
| `iris:chunk` | v2i | `(cx, cy)` |
| `iris:depthClass` | string | `8i`, `16i`, `16f`, `32f` |
| `iris:encoding` | string | `srgb`, `linear`, `gamma22`, `custom` (`custom` means see the embedded ICC profile) |
| `iris:premultiplied` | int | `1` |

### 5.5 Codec (`iris-exr`)

The codec is pure Kotlin, no native code, so it behaves identically on every target. It exposes a streaming tile API so a chunk is never fully decoded to the heap:

- `ExrReader.open(source)` gives header and tile index. `readTile(tx, ty, into: TileBuffer)` decodes one tile.
- `ExrWriter.begin(header)`, then `writeTile(tx, ty, from: TileBuffer)` in any order, then `finish()` (writes the offset table).
- Supported to write: tiled, `HALF`/`FLOAT`, `NO_COMPRESSION`, `ZIP` (and `PIZ`/`ZIPS` if adopted). Supported to read in Phase 1: the same, plus scanline files and `UINT`. Reading `RLE`, `PXR24`, `B44`, `B44A`, `DWAA`, `DWAB` and multi-part files is Phase 6 (import of third-party EXR). Deep files are rejected with a clear error.
- Compression needs Deflate. Phase 0 spike S4 decides the provider (a multiplatform library or a small pure-Kotlin implementation) behind `interface Deflater`. It must be Apache-compatible.
- Conformance is proven with reference files: fixtures produced by the OpenEXR reference implementation (the public `openexr-images` set is BSD-3-Clause; confirm license before copying files into `testdata/`) plus files written by our codec and read back by a reference tool in the nightly CI job.

## 6. Vector layers: SVG

Each vector layer is one SVG file. Goals, in priority order: (1) lossless round trip of everything Iris can express, (2) the file renders sensibly in a browser or Inkscape with no Iris code, (3) other tools can edit it and Iris still opens it.

Rules:

- Root: `<svg xmlns="http://www.w3.org/2000/svg" xmlns:iris="urn:appthere:iris:1" viewBox="x y w h">`. One user unit equals one document pixel, so `viewBox` is in layer pixel coordinates.
- Allowed content: SVG static features only. No scripts, no `foreignObject`, no external references (`href` to `http:` or `file:`), no event attributes, no `<style>` with `@import`. Embedded images are `data:` URIs only, and only when a vector layer contains a pasted image (otherwise use a raster or smart-object layer).
- Every Iris object is an element with an `id` (UUID) and Iris metadata in `iris:` attributes. Plain SVG elements without metadata are accepted on read and become plain objects.
- Live information that plain SVG cannot express is stored as `iris:` attributes next to a **fallback** that plain SVG can render:

| Iris feature | Plain-SVG rendering (fallback) | Live data |
| --- | --- | --- |
| Multiple fills and strokes (appearance stack) | A `<g iris:appearance="...">` containing one element per paint, bottom to top | `iris:appearance` lists the paints and effects |
| Variable-width stroke | The stroke expanded to a filled outline path | `iris:widthProfile` and `iris:centerline` |
| Live shapes (rounded rect, star, polygon, spiral) | `<rect>`, `<ellipse>` or a plain `<path>` | `iris:shape` and `iris:params` |
| Live booleans / compound shapes | The computed result path | `iris:booleanOp` and child operands in `<iris:operands>` |
| Symbols and instances | `<symbol>` and `<use>` | `iris:overrides` |
| Mesh gradient | A linear/radial approximation | `iris:mesh` |
| Text (point, area, on path) | `<text>`/`<tspan>` with font family; embedded font subset in resources | `iris:textStyle`, `iris:areaBox` |
| Blend modes, effects | `mix-blend-mode` and CSS filter equivalents where they exist | `iris:effects` |

- Colors outside sRGB are written as `color(display-p3 ...)` or via `iris:color` with the profile reference, and the plain fallback is the nearest sRGB.
- Writers produce stable output: attributes in a fixed order, numbers with at most 6 fractional digits (path coordinates) and no exponent for ordinary magnitudes, so saves are diff-friendly and tests can compare text.
- The reader has two modes: **strict** for files Iris wrote (validates `iris:` attributes) and **tolerant** for foreign SVG (ignores what it does not understand and reports it in an import report).
- If a foreign tool edited an element that has both a fallback and live data and the two disagree (detected by a stored hash of the fallback in `iris:fb`), the fallback wins and the live data is dropped with a warning.

## 7. Resources

- ICC profiles: stored whole, referenced by id. Working-space and display decisions stay in `document.xml`.
- Fonts: only the glyphs used, as WOFF2 or TTF/OTF, with the license field preserved. Missing fonts fall back with a visible warning.
- Brushes: `brush.xml` (parameters and dynamics curves) plus tip images (EXR or PNG).
- Linked smart-object sources: embedded copies in `resources/linked/`. A `link` attribute may hold a relative path; embedding is the default.

## 8. Metadata

- `docProps/core.xml`: title, creator, created, modified, keywords, description (standard OPC core properties).
- EXIF, XMP and IPTC of imported images are kept as parts reachable from `document.xml.rels` (type `metadata`). Export strips them on request; GPS is stripped by default on mobile export.
- Thumbnail: a PNG of the flattened first artboard, regenerated on every save.

## 9. Versioning and compatibility

- `formatVersion` is `major.minor`. Same major, higher minor: open, preserve unknown content, warn. Higher major: refuse to open writable, offer read-only flattened preview if the thumbnail exists.
- Migrations live in `iris-io` as pure functions `v1.0 -> v1.1 ...` with a test per migration using frozen fixtures in `testdata/format/v1.0/`.
- Fixtures for every released minor version stay in the repo forever.

## 10. Save and load

Save (all targets):

1. Collect dirty items from the document: changed structure, and the set of (layer, chunk) pairs whose tiles changed since the last save.
2. Write a new package to a temporary location next to the target (or in the app cache where the platform forbids sibling files).
3. Stream-copy every unchanged entry from the old package **without recompressing** (raw copy of compressed bytes).
4. Encode dirty chunks to EXR, regenerate `document.xml`, changed `layer.xml`, relationships, `[Content_Types].xml` and the thumbnail.
5. Verify: reopen the central directory, check every relationship target exists, check a CRC on the new entries.
6. Replace the target atomically (rename where the platform supports it; on Android document providers and iOS file coordination, write through the platform adapter's `replaceContents`).

Load:

1. Read the central directory and relationships only. Parse `document.xml`. Show the canvas as soon as the structure and the thumbnail are available.
2. Decode chunks lazily when the viewport (or an export) needs them. Never decode a whole layer eagerly.

Recovery (Phase 3 scope): the autosave timer and app backgrounding write the same package format to a recovery slot in app data, keyed by document id, using the same incremental algorithm. A write-ahead command journal on top of that is optional and deferred; record the decision in `docs/decisions.md` when made.

## 11. Security and robustness limits

All limits are named constants in `iris-io` with tests, and the defaults below are starting points to tune.

- ZIP: reject entries whose names are absolute, contain `..`, or normalize to a different part name; cap entry count, per-entry uncompressed size, and total uncompressed size; reject compression ratios above a threshold (zip bombs).
- XML: disable DTDs and external entities entirely. Cap depth and attribute counts.
- EXR: validate `dataWindow`, tile counts and the offset table before allocating; cap decoded tile bytes against the declared tile size; never trust a declared size to allocate.
- SVG: cap path length and element count; refuse external references.
- Decoding runs in cancelable coroutines with memory accounting; failure produces a typed error and leaves the document usable.
- Fuzz tests exist for the ZIP, XML, EXR and SVG readers (see `docs/testing-tdd.md`).

## 12. Tests required before the format is considered done

- Round trip: build a document in memory with every layer type and parameter kind, save, load, compare structurally and by rendering.
- Pixel exactness: for each depth class, random and edge-case tiles round-trip bit-exactly through EXR.
- Incremental save: change one tile; assert only that chunk's entry bytes differ and all other entries are byte-identical.
- Unknown content: inject unknown parts, relationships, elements and attributes; save; assert they survive.
- Foreign opens: SVGs produced by Inkscape/Illustrator open in tolerant mode; Iris SVGs render in a reference renderer within a tolerance.
- Corrupt files: truncated, bad CRC, bad offsets, missing relationship targets all produce typed errors, never crashes.
