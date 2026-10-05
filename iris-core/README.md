# iris-core

Ids, units, geometry (`Rect`, `Matrix`, `Point`), `Result` types, `Clock`, `DispatcherProvider`, `Logger` interface.

- Package: `org.appthere.iris.core`
- Convention plugin: `iris.kmp.library`
- May depend on: nothing (`docs/architecture.md` §1; checked by `ArchitectureTest` from A4)
- Engine module: no Compose and no platform API (`android.*`, `java.awt.*`, `platform.UIKit.*`, Skia and others). Plain Kotlin in `commonMain`.

## Public entry points

- Ids: `DocumentId`, `LayerId`, `ArtboardId` (value classes over `Uuid`; `parseOrNull` accepts the lowercase canonical form only), created through an injected `IdSource` (`RandomIdSource` in apps).
- Geometry in document space: `Point`, `Rect` (half-open), `Matrix` (affine, SVG component order; `m * n` applies `n` first), `Radians`. `IntRect` for pixels and tiles.
- `IrisResult<T>` (`Success`/`Failure`) with `map`, `flatMap`, `fold`, `getOrNull`, `getOrElse`, `errorOrNull`, `onSuccess`, `onFailure`. `IrisError` is an open interface; each module defines its own sealed hierarchy.
- `Clock` (wall `now()` and `monotonic()`), `SystemClock` for apps.
- `DispatcherProvider` (`compute`, `io`, `render`), `DefaultDispatcherProvider` for apps.
- `Logger`, `LogLevel`, `NoOpLogger`, and the lazy `debug`/`info`/`warn`/`error` helpers.
- `FractionalIndex` (sibling order keys) and `SiteId`.

Depends on `kotlinx-coroutines-core` (API, for `DispatcherProvider`).
