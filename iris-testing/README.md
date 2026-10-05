# iris-testing

Fixtures, golden harness, stroke replay, fakes.

- Package: `org.appthere.iris.testing`
- Convention plugin: `iris.kmp.library`
- May depend on: any module, in tests only (`docs/architecture.md` §1; checked by `ArchitectureTest` from A4)
- Test-only module: never a dependency of production code. No Compose and no platform API.

## Public entry points

- `assertClose` (Double and Float; absolute and/or ulp tolerance; NaN and infinities exact).
- `FakeClock`, `FakeDispatcherProvider` (one virtual-time `StandardTestDispatcher`; pass `dispatcher` to `runTest`), `RecordingLogger`.
- Golden harness: `GoldenImage` (read interface, no pixel copies), `compareGoldenImages`, `goldenDiff`, `GoldenTolerance`, `assertGolden`, `GoldenStore`, `GoldenCodec`; `FileGoldenStore` on the JVM writes `build/golden-candidates/` and `build/golden-failures/<name>/` (docs/testing-tdd.md section 4).
- `ScriptedStylusSource` for `StylusInputSource` consumers.

Use from other modules' test source sets only.
