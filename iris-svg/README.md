# iris-svg

SVG reader/writer with `iris:` extensions.

- Package: `org.appthere.iris.svg`
- Convention plugin: `iris.kmp.library`
- May depend on: `iris-core`, `iris-vector`, `iris-color` (`docs/architecture.md` §1; checked by `ArchitectureTest` from A4)
- Engine module: no Compose and no platform API (`android.*`, `java.awt.*`, `platform.UIKit.*`, Skia and others). Plain Kotlin in `commonMain`.

## Public entry points

None yet.
