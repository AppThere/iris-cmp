# iris-opc

OPC package reader/writer, ZIP, relationships, content types.

- Package: `org.appthere.opc` (neutral, not `org.appthere.iris`; D-027)
- Convention plugin: `iris.kmp.library`
- May depend on: nothing (`docs/architecture.md` §1; checked by `ArchitectureTest`)
- Engine module: no Compose and no platform API (`android.*`, `java.awt.*`, `platform.UIKit.*`, Skia and others). Plain Kotlin in `commonMain`.

## Extraction rules (D-027)

This module is meant to become a library shared with the planned KMP Office library, so:

- No dependency on any Iris module, and no Iris types, URIs or relationship types (those live in `iris-io`).
- Its own error and result types; `iris-io` maps them to `IrisError`.
- API shape: `docs/spikes/S12-shared-opc.md` section 2.

## Public entry points

None yet.
