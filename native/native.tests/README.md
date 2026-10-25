# Kotlin/Native compiler integration tests

Black-box tests: compile test data with the Kotlin/Native compiler from the distribution under test, run the binaries, and check the
results.

| Location                                   | Contents                                                                                      |
|--------------------------------------------|-----------------------------------------------------------------------------------------------|
| `testFixtures/.../konan/test/blackbox`     | Infrastructure: base test classes, settings, test grouping, compilation, execution            |
| `testFixtures/.../generators/tests`        | Test class generators (`GenerateNativeTests.kt`)                                              |
| `tests/`                                   | Hand-written tests                                                                            |
| `testData/`                                | Native-specific test data (`codegen`, `standalone`, `gc`, `CInterop`, `ObjCExport`, `CExport`, `lldb`, `klib`, …) |
| `codegen-box/`                             | Runs the shared `compiler/testData/codegen/{box,boxInline}` tests                             |
| `cli-tests/`, `driver/`, `stress/`, `litmus-tests/`, `gc-fuzzing-tests/` | Other test groups                                               |
| `klib-ir-inliner/`, `klib-compatibility/`  | klib inlining and klib backward/forward compatibility tests                                   |

Generated test classes go to `<project>/build/tests-gen/` and are regenerated automatically by the test tasks.
To refresh them for the IDE, use `./gradlew :native:native.tests:generateTests` (or the same task in a subproject).

For running tests, test properties, tags and directives, see [Testing](../../kotlin-native/HACKING.md#testing).
For the infrastructure overview, see [Test infrastructure overview](../../kotlin-native/HACKING.md#test-infrastructure-overview).
