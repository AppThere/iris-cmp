# iris-input

`StrokeSample`, normalizer, calibration, predictor (common).

- Package: `org.appthere.iris.input`
- Convention plugin: `iris.kmp.library`
- May depend on: `iris-core` (`docs/architecture.md` §1; checked by `ArchitectureTest` from A4)
- Engine module: no Compose and no platform API (`android.*`, `java.awt.*`, `platform.UIKit.*`, Skia and others). Plain Kotlin in `commonMain`.

## Public entry points

- `StylusInputSource`, `StylusCapabilities` (docs/architecture.md section 14).
- `RawPenEvent`, `PenAction`, `PenTool`, `PenAxes` (which optional axes are present). Minimal Phase 0 shape; Phase 3 extends it (buttons, normalizer, predictor).

Depends on `kotlinx-coroutines-core` (API, for `Flow`).
