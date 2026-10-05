# iris-render

Render graph, scheduler, tile cache, CPU kernels, `RenderBackend` interface.

- Package: `org.appthere.iris.render`
- Convention plugin: `iris.kmp.library`
- May depend on: `iris-model`, `iris-pixels`, `iris-color`, `iris-vector` (`docs/architecture.md` §1; checked by `ArchitectureTest` from A4)
- Engine module: no Compose and no platform API (`android.*`, `java.awt.*`, `platform.UIKit.*`, Skia and others). Plain Kotlin in `commonMain`.

## Public entry points

None yet.
