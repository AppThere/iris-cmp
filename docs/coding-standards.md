# Coding standards

Applies to all Kotlin in this repository. Tooling enforces what it can; reviews and the end-of-phase quality pass cover the rest. Rules marked **[CI]** fail the build.

## 1. Language and style

- Kotlin official code style. Formatting by Spotless with ktlint **[CI]**; static analysis by detekt with the repository config **[CI]**.
- Names: types `PascalCase`, functions and properties `camelCase`, constants `SCREAMING_SNAKE_CASE`, packages lowercase. Test names are backtick sentences: `` `stroke with zero pressure paints nothing` ``.
- Package root `org.appthere.iris.<module>` (placeholder; see `docs/decisions.md`). One top-level public type per file unless the types are a closed set (sealed hierarchy members, small value types).
- No wildcard imports. No `typealias` to hide a primitive meaning; use a value class instead.
- Prefer expression bodies for one-liners and named arguments when a call has more than three arguments or any boolean argument.

## 2. Size and complexity limits **[CI]**

| Limit | Value |
| --- | --- |
| File length | 400 lines (tests 600) |
| Function length | 50 lines |
| Cyclomatic complexity per function | 12 |
| Nesting depth | 4 |
| Parameters per function | 7 (use a parameter object beyond that) |
| Public members per class | 20 |

`verifySizeLimits` reports every offender. Generated code and test fixtures in `testdata/` are exempt. Do not add suppressions; split the code. A suppression requires a decision-log entry.

## 3. Module boundaries and visibility

- Engine modules use `explicitApi()`; every public declaration states its visibility and type. Default to `internal`. Public API changes are checked by the binary-compatibility validator **[CI]** and need Kevin's approval once a module's API is declared stable.
- Dependency direction follows `docs/architecture.md` section 1 and is verified by `ArchitectureTest` **[CI]**.
- `expect`/`actual` is for things that cannot be an interface (`TileBuffer`, a few constants). Everything else is an interface in common code with implementations injected by the app shell. Keep `actual` implementations tiny and covered by contract tests.
- No platform types in common signatures (`android.graphics.*`, `NSObject`, `java.awt.*`). Wrap them in the adapter modules.
- No global mutable state, no singletons, no service locators. Pass dependencies through constructors. Top-level `val`s are constants only.

## 4. Types and data

- Model meaning with types: `value class LayerId(val uuid: Uuid)`, `value class Radians(val v: Float)`, `PixelCoord`, `DocCoord`. No `Int` that is sometimes a layer index and sometimes a tile coordinate.
- Prefer immutable data classes and sealed hierarchies. `when` over sealed types has no `else`.
- No `!!` in production code **[CI]**. Use `requireNotNull` with a message at boundaries, or restructure.
- Nullability means absence, not error. Do not return `null` to mean "failed".
- Collections exposed from APIs are read-only interfaces. Use persistent collections inside `DocumentSnapshot`.
- Enums that are persisted in files have stable string names defined in one place; never use `ordinal`.

## 5. Errors

- Expected failures (invalid file, unsupported feature, over memory budget, cancelled) return `IrisResult<T>` / sealed errors. Programmer errors (violated preconditions) use `require`, `check`, `error`.
- Never catch `Throwable` or `Exception` broadly. Catch the specific exception at the boundary that translates it to an `IrisError`, and always rethrow `CancellationException`.
- Never swallow an exception. Log with context and either translate or propagate.
- Error messages are for developers and contain the data needed to diagnose (counts, ids), but never document content or user file paths in release logs.

## 6. Concurrency **[review]**

- Structured concurrency only. No `GlobalScope`. Every launched coroutine belongs to a scope with an owner and a lifetime.
- Dispatchers come from `DispatcherProvider`; no hard-coded `Dispatchers.*` in engine code.
- Long loops (tiles, pixels per row, import decoding) call `ensureActive()` at least once per tile.
- No blocking calls (file IO, `runBlocking`, `Thread.sleep`) on the main or input threads. `runBlocking` is allowed only in tests and in `main` of `iris-cli`.
- Shared mutable state is confined to one actor (document actor, editor actor). If you need a lock, explain why in a comment and add a concurrency test.
- Published snapshots and frozen tiles are immutable; mutable buffers are owned by exactly one coroutine at a time.

## 7. Performance rules for pixel code **[review + benchmarks]**

- Inner loops use primitive arrays or `TileBuffer` views. No boxing, no `List<Float>`, no lambdas that capture and allocate per pixel, no `Pair`/`Triple`, no `String` in hot paths.
- Zero allocations in steady-state brush stamping, tile compositing and filter kernels. Allocation checks run in benchmarks (JVM allocation profiler or counters around pools) and gate merges for those modules.
- Process by tile, row-major, with a fixed stride. Hoist invariants. Prefer a small number of well-named kernels over generic frameworks.
- Do not optimize without a benchmark. Add the benchmark first (`kotlinx-benchmark`), record the baseline in `benchmarks/baselines.json`, and keep budgets from the phase plan.
- Use `Float` for color math. Use `Double` only where precision demands it (accumulated geometry, ICC matrix inversion) and say why in a comment.
- Pools and reuse for `TileBuffer`s; every acquired buffer is released in a `use {}` block or by an owner with a documented lifetime.

## 8. Numerical and determinism rules

- Float comparisons in tests use helpers (`assertClose(expected, actual, ulps | epsilon)`). Never `==` on floats outside bit-exactness tests.
- CPU results must be deterministic across targets within a documented tolerance. Avoid `Math` functions with platform-dependent last-bit differences in code on the bit-exact path (export, golden tests): use `iris-core` implementations (`fastExp`, `fastPow`, table-based sRGB transfer) for those.
- No `Random()` without a seed. No `Clock.System` in engine code; inject `Clock`.
- Premultiplied alpha is the only in-memory representation. Convert at IO boundaries and in color pickers only; every function states in KDoc whether it expects premultiplied input.

## 9. Documentation

- KDoc on every public declaration: what it does, units, ranges, thread-safety, ownership of buffers.
- Comments explain why, not what. Remove commented-out code.
- Each module has a short `README.md`: purpose, public entry points, dependency rule reminder.
- Architecture-level changes update `docs/architecture.md` in the same commit. Format changes update `docs/file-format.md` (with approval).

## 10. Dependencies

- Add a dependency only if it saves substantial work and is maintained. Check: Apache-2.0-compatible license, supports every target the module needs, no transitive GPL, recent releases.
- Record each dependency in `docs/dependencies.md` (name, version, license, why, date checked) and keep versions in `gradle/libs.versions.toml` only.
- `licenseCheck` compares the resolved dependency graph to an allow-list **[CI]**.
- Native libraries (cinterop, JNI, Panama) need Kevin's approval, an interface in common code, a pure-Kotlin fallback or a feature flag per target, and a packaging note for each store.
- Compose: use stable, documented APIs only. Experimental APIs require an opt-in annotation at a single wrapper site with a comment and a decision-log entry.

## 11. Compose rules

- State hoisting: composables take state and callbacks; no business logic in composables.
- No work in composition. No allocation of large objects, no IO. Use `remember`/`derivedStateOf` deliberately.
- Stable parameters (immutable data classes, `@Immutable`/`@Stable` only when true). Pass lambdas that do not capture changing state needlessly.
- The canvas host must not trigger recomposition per frame. Measure with a recomposition counter in debug builds; the performance test fails on regressions.
- Every interactive element has a `contentDescription`/semantics role, a minimum 44 dp touch target on touch, and keyboard focus behavior on desktop.
- No string literals shown to users in composables; use resources.

## 12. Code-smell checklist for the end-of-phase quality pass

Review the whole phase's code for each item. File a fix or record why it is acceptable.

1. Long methods, long files, deep nesting (beyond the CI limits, anything that is hard to read).
2. God classes and "manager" or "helper" types that own unrelated things.
3. Feature envy: a function using another class's data more than its own.
4. Primitive obsession: raw `Int`/`Float` where a value class belongs.
5. Duplicate logic across modules or across platforms.
6. Platform leakage: platform types, threads or assumptions in common code.
7. Hidden global state, time or randomness.
8. Swallowed or over-broad exception handling; missing `CancellationException` rethrow.
9. Blocking calls on the wrong dispatcher; missing `ensureActive()` in long loops.
10. Allocation in hot loops; buffers acquired without a release path.
11. Boolean flag parameters that switch behavior; split the function.
12. Dead code, unused parameters, commented-out code, stale TODOs.
13. Tests that assert implementation details, depend on timing, or hide failures with large tolerances.
14. Public API that exposes internals or mutable collections.
15. Missing KDoc or docs out of date with the code.

## 13. Version control

- Trunk-based: short-lived branches, small pull requests, squash merge with a Conventional Commit title. One logical change per commit.
- Commit types: `feat`, `fix`, `test`, `refactor`, `perf`, `docs`, `build`, `ci`, `chore`. A breaking API or format change is marked `!` and needs a decision-log entry.
- No large binaries in git other than `testdata/` fixtures (use Git LFS for goldens above 1 MB if the repository grows; ask Kevin).
- Never commit secrets, signing keys, local paths or IDE files.

## 14. Security and privacy

- Treat every file as hostile (limits in `docs/file-format.md` section 11).
- No network permissions in manifests or entitlements. No analytics SDKs. Crash reporting, if added, is opt-in and sends no document content.
- Logs never contain pixel data, text content or user file paths in release builds.
- Embedded fonts and linked sources keep their license metadata and are never extracted without user action.
