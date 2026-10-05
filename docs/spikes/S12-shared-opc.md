# Spike S12: shared OPC layer

Question (`docs/phase-plan.md`, P-003): should `iris-opc` be the same library as the OPC layer of the planned KMP Office library?

Status: report and interface sketch done 2026-10-05. **Decision pending (Kevin).** No repositories were created or merged.

## 1. What each side needs from OPC

| Capability | Iris (`docs/file-format.md`) | OOXML (ECMA-376 Part 2) |
| --- | --- | --- |
| Part names: validation, normalization, case-insensitive uniqueness | yes (§2) | yes |
| `[Content_Types].xml`: defaults and overrides | yes (§2) | yes |
| Relationships (`.rels`), internal targets | yes (§2) | yes |
| Relationships with `TargetMode="External"` | no (refused for safety, §11) | yes (hyperlinks, linked data) |
| Core properties (`docProps/core.xml`), thumbnail | yes (§2, §8) | yes |
| ZIP64 | yes | yes |
| Per-entry compression choice (STORED vs Deflate) | yes, STORED for `.exr`/`.png` | yes |
| Raw copy of unchanged entries (no recompression) | **core of incremental save** (§10) | useful (fast re-save) |
| Lazy, random access to many parts | **yes**: thousands of chunk parts, decoded on demand (§10) | yes, smaller scale |
| Reading data descriptors | yes (other tools may repack `.iris` files) | yes (common in the wild) |
| Writing data descriptors / streaming parts of unknown size | no (Iris knows sizes) | yes (large worksheets) |
| Interleaved parts (`/[0].piece`, ...) | read only, rare | read required by consumers |
| Digital signatures (OPC/XML-DSig) | no | optional |
| Orphan and missing-target validation | yes, before replacing the file (§10) | yes |
| Preserving unknown parts and relationships | yes (§1, §9) | yes |
| Untrusted-input limits (zip bombs, names with `..`, XML DTDs) | yes (§11) | yes |
| Encrypted documents | no | not OPC (MS-OFFCRYPTO uses a compound file) |

Overlap is nearly complete. Iris needs nothing OOXML lacks; OOXML adds external relationships, streaming writes, interleaving and signatures, all of which can be optional layers.

## 2. Interface sketch

Shape only, not compiled; Phase 1 tests define the final API. The point is what the API does **not** contain: no Iris types, no `iris-core`, no Iris URIs, so the code can live in either repository.

```kotlin
// Package: org.appthere.opc (neutral, not org.appthere.iris).

// --- Byte IO the library owns; platform file access adapts to it outside the library ---
public interface RandomAccessSource : AutoCloseable {
    public val size: Long
    public fun read(position: Long, into: ByteArray, offset: Int, length: Int): Int
}
public interface ByteSink : AutoCloseable {
    public val position: Long
    public fun write(bytes: ByteArray, offset: Int, length: Int)
}

// --- Names and metadata ---
@JvmInline public value class PartName private constructor(public val value: String) {
    public val normalized: String            // for case-insensitive comparison (ECMA-376-2 §6.2.2.3)
    public fun relationshipsPart(): PartName // /a/b.xml -> /a/_rels/b.xml.rels
    public companion object { public fun parse(text: String): OpcResult<PartName> }
}
public data class ContentType(val type: String, val subtype: String, val parameters: Map<String, String> = emptyMap())
public enum class Compression { STORED, DEFLATED }
public enum class TargetMode { INTERNAL, EXTERNAL }
public data class Relationship(val id: String, val type: String, val target: String, val targetMode: TargetMode) {
    public fun resolve(source: PartName?): PartName?   // null for external targets
}
public data class PartInfo(
    val name: PartName, val contentType: ContentType, val compression: Compression,
    val compressedSize: Long, val uncompressedSize: Long, val crc32: Int,
)

// --- Errors: the library's own; Iris wraps them in an IrisError hierarchy in iris-io ---
public sealed interface OpcError { /* BadZip, BadPartName, MissingContentType, LimitExceeded, CrcMismatch, ... */ }
public sealed interface OpcResult<out T> { /* Success / Failure(OpcError) */ }
public data class OpcLimits(
    val maxEntries: Int, val maxEntryUncompressed: Long, val maxTotalUncompressed: Long,
    val maxCompressionRatio: Int, val maxXmlDepth: Int, val allowExternalRelationships: Boolean,
)

// --- Reading: central directory, content types and relationships up front; part data lazily ---
public interface OpcPackageReader : AutoCloseable {
    public val parts: Collection<PartInfo>
    public fun relationships(source: PartName?): OpcResult<List<Relationship>>   // null = package level
    public fun open(part: PartName): OpcResult<PartInput>                        // inflating, CRC checked at end
    public fun raw(part: PartName): OpcResult<RawEntry>                          // compressed bytes for raw copy
    public companion object {
        public fun open(source: RandomAccessSource, limits: OpcLimits): OpcResult<OpcPackageReader>
    }
}

// --- Writing: raw copies plus new parts; [Content_Types].xml, .rels and the central directory on finish ---
public interface OpcPackageWriter : AutoCloseable {
    public fun copyRaw(from: OpcPackageReader, part: PartName): OpcResult<Unit>
    public fun writePart(part: PartName, type: ContentType, compression: Compression, content: (ByteSink) -> Unit): OpcResult<Unit>
    public fun setRelationships(source: PartName?, relationships: List<Relationship>)
    public fun finish(): OpcResult<Unit>
}

// --- Validation before an atomic replace (Iris §10 step 5) ---
public fun validatePackage(reader: OpcPackageReader): List<OpcProblem>   // orphans, missing targets, bad types

// Optional layers, not needed by Iris: streaming writes with data descriptors, interleaved-part reading,
// signatures. They sit on top of the same reader/writer.
```

`IrisResult`/`IrisError` stay in Iris: `iris-io` maps `OpcError` to its own sealed hierarchy (the pattern from A9).

## 3. Dependencies a shared library would have to settle

- **Deflate/inflate in common code (S4).** `iris-exr` needs zlib streams too, but the module table does not let `iris-exr` depend on `iris-opc`. Whatever S4 chooses (pure Kotlin, platform zlib, a library) must live where both can use it: inside `iris-core`, a new small module, or a dependency. A shared OPC library would carry its own copy or depend on the same artifact.
- **XML in common code.** OPC needs to read and write `[Content_Types].xml`, `.rels` and `core.xml`; Iris also needs XML for `document.xml` and SVG, and the Office library for everything. There is no XML parser in the Kotlin standard library. Options: a small in-house reader and writer (namespaces, attributes, no DTDs), or a library such as xmlutil (needs a license and target check, and Kevin's approval). This is a separate decision that affects all three.
- **No coroutines in the API.** Plain blocking calls on `RandomAccessSource` keep the library usable from any threading model; Iris calls it on `DispatcherProvider.io`.

## 4. Options

| | A. Shared library now, own repository | B. Build in Iris, extraction-ready | C. Separate implementations |
| --- | --- | --- | --- |
| Where | new repo (or included build), published or consumed as a composite build | `iris-opc` in this repo, package `org.appthere.opc`, no Iris dependencies | each project writes its own |
| Iris Phase 1 | waits on a second repo's setup, release and CI | starts immediately | starts immediately |
| Office library | consumes it from day one | copies the module (both Apache-2.0, same owner) or waits for extraction | starts from scratch |
| API stability pressure | high before the API has met real use | low; the API settles against Iris's tests first | none |
| Duplicate work | none | none until the Office library starts | the whole OPC layer, twice |
| Cost for a solo developer | two release cycles and version coordination now | one rule to keep: no Iris types in `iris-opc` | two code bases to fix bugs in |

## 5. Recommendation

**Option B.** Build `iris-opc` in this repository against the sketch above, under three rules that keep it extractable:

1. Package `org.appthere.opc`, not `org.appthere.iris.opc`.
2. No dependency on `iris-core` or any other Iris module, and no Iris URIs or relationship types (those live in `iris-io`). `ArchitectureTest` can enforce this: `iris-opc` becomes a module that may depend on nothing.
3. Its own error and result types, mapped to `IrisError` in `iris-io`.

Extraction into a shared library then becomes a move, not a rewrite, and can happen when the Office library starts, once the API has held up against Iris's incremental-save and fuzz tests.

What B changes elsewhere, for approval with the decision:

- `docs/architecture.md` §1: `iris-opc` "may depend on" becomes "none" (today: core), and its package root becomes `org.appthere.opc`.
- `ArchitectureTableSyncTest` and the rule table follow the doc.
- S4 must place Deflate where both `iris-opc` (now dependency-free) and `iris-exr` can use it; a tiny dependency-free module (for example `iris-deflate`, also extraction-ready) is the obvious candidate.

## 6. Open questions for Kevin

1. Option A, B or C? (P-003)
2. Does the KMP Office library exist yet, and if so, where? If it already has an OPC layer, comparing APIs before Phase 1 changes the picture.
3. Package root for neutral code: `org.appthere.opc` (still under the P-001 placeholder domain)?
