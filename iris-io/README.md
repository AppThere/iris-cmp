# iris-io

`.iris` reader/writer, import/export codecs, import reports.

- Package: `org.appthere.iris.io`
- Convention plugin: `iris.kmp.library`
- May depend on: `iris-model`, `iris-exr`, `iris-opc`, `iris-svg` (`docs/architecture.md` §1; checked by `ArchitectureTest` from A4)
- Engine module: no Compose and no platform API (`android.*`, `java.awt.*`, `platform.UIKit.*`, Skia and others). Plain Kotlin in `commonMain`.

## Public entry points

None yet.
