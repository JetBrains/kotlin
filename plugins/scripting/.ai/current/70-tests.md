# Current — Tests

Where the tests live and how to run them.

## Test placement

| Area | Path | Coverage |
|---|---|---|
| Common API | `libraries/scripting/common/test/` | Smoke tests for API surface |
| JVM impls | `libraries/scripting/jvm/test/` + testData | Evaluator, classloader, snippet history |
| Host & REPL | `libraries/scripting/jvm-host-test/` (19 test files) | `ReplTest` (K2), `LegacyReplTest` (K1), `ScriptingHostTest`, `ConfigurationDslTest`, `CachingTest`, `ResolveDependenciesTest` (K2-skipped), `ImplicitsFromScriptResultTest` |
| JSR-223 | `libraries/scripting/jsr223-test/test/` | `KotlinJsr223ScriptEngineIT.kt` — JSR-223 compliance + manual memory tests |
| Dependencies API | `libraries/scripting/dependencies/test/` (4 files) | Resolver API |
| Dependencies Maven | `libraries/scripting/dependencies-maven/test/` (2 files) | Aether integration |
| Dependencies Maven shaded | `libraries/scripting/dependencies-maven-all/test/` | Smoke |
| main-kts | `libraries/tools/kotlin-main-kts-test/` | Script def integration |
| Scripting plugin tests | `plugins/scripting/scripting-tests/` | Compiler-side integration |
| IDE services | `plugins/scripting/scripting-ide-services-test/` | Completion / analysis (K1) |
| Test fixture def | `plugins/scripting/test-script-definition/` | Tiny custom definition used by other tests |

## K1/K2 markers

`libraries/scripting/jvm-host-test/.../commonUtil.kt`:

| Helper | Purpose |
|---|---|
| `isRunningTestOnK2` | Runtime branch |
| `expectTestToFailOnK2` | Marks tests known to fail on K2 (skipped) |

`ResolveDependenciesTest` is the main known-failing-on-K2 entry today.

## Running tests

`./gradlew` (use `-q` for less noise):

```bash
# JVM host (REPL, host, deps)
./gradlew :kotlin-scripting-jvm-host-test:test -q

# JSR-223
./gradlew :kotlin-scripting-jsr223-test:test -q

# main-kts
./gradlew :kotlin-main-kts-test:test -q

# Scripting plugin tests
./gradlew :plugins:scripting:scripting-tests:test -q
```

Update test data (when format changes):
```bash
./gradlew :kotlin-scripting-jvm-host-test:test -Pkotlin.test.update.test.data=true --continue
```

## Generated tests

Scripting test runners follow the general compiler test pattern — generated `*TestGenerated.java` runners produced by `./gradlew generateTests`. Never edit them by hand.

## Test data conventions

- `.kts` fixtures live alongside the test class under `testData/`
- For configuration-aware tests, fixtures often pair `.kts` + a `.config.kt` defining the custom script def

## Compiler-side scripting tests (under `compiler/`)

Tests that live inside the compiler tree but exercise scripting/REPL. Mixed frontends. Disposition tags: REMOVE / KEEP / MOVE = relocate to `plugins/scripting/scripting-tests` / AUDIT.

### Test classes

| Path | Class | Frontend | Disposition |
|---|---|---|---|
| `compiler/tests-integration/tests/.../codegen/ScriptGenTest.kt` | `ScriptGenTest` | K1 | REMOVE |
| `compiler/tests-integration/tests/.../codegen/` | `CustomScriptCodegenTest` (abstract) | both | KEEP (test infra) |
| `compiler/tests-integration/tests/.../codegen/` | `FirLightTreeCustomScriptCodegenTest` | K2 LT | KEEP, MOVE → `plugins/scripting/scripting-tests` |
| `compiler/tests-integration/tests/.../codegen/` | `FirPsiCustomScriptCodegenTest` | K2 (PSI variant) | KEEP, MOVE → `plugins/scripting/scripting-tests` |
| `compiler/tests-integration/tests/.../cli/jvm/repl/GenericReplTest.kt` | `GenericReplTest` | K1 | REMOVE (with `cli-base/repl/*`) |
| `compiler/tests-integration/tests/.../cli/LauncherScriptTest.kt` | `LauncherScriptTest` | both | KEEP (CLI launcher integration) |
| `compiler/psi/psi-impl/tests/.../psi/CustomPsiTest.kt` (`testScriptFunctionDeclaration` etc.) | K1 PSI script parse cases | K1 | REMOVE (with K1) |
| `compiler/daemon/daemon-tests/test/.../CompilerDaemonTest`, `CompilerApiTest` | mixed daemon tests | K1 (mostly) | AUDIT — drop REPL-specific; keep general daemon |
| `compiler/daemon/daemon-client/src/test/kotlin/` | listener tests | both | KEEP (not script-specific) |
| `compiler/build-tools/kotlin-build-tools-api-tests/` | `ScriptingTest`, `ScriptExtensionDiscoveryTest`, `DiscoverScriptExtensionsOperationDefaultsTest`, `ScriptResolverEnvironmentSpecialCharsTest` | both | KEEP (BTA-owned) |

### Test infrastructure (abstract runners + helpers)

| Path | Class | Frontend | Disposition |
|---|---|---|---|
| `compiler/tests-common-new/testFixtures/.../runners/codegen/AbstractFirScriptCodegenTest.kt` | abstract runner | K2 | KEEP |
| `compiler/tests-common-new/testFixtures/.../runners/codegen/AbstractFirScriptAndReplCodegenTest.kt` | abstract runner | K2 | KEEP |
| `compiler/tests-common-new/testFixtures/.../runners/codegen/AbstractFirCustomScriptCodegenTestBase.kt` | abstract runner | K2 | KEEP |
| `compiler/tests-common-new/testFixtures/.../runners/codegen/AbstractFirLightTreeCustomScriptCodegenTest.kt` | abstract runner | K2 LT | KEEP |
| `compiler/tests-common-new/testFixtures/.../runners/codegen/AbstractFirPsiCustomScriptCodegenTest.kt` | abstract runner | K2 PSI | KEEP |
| `compiler/tests-common-new/testFixtures/.../frontend/fir/FirReplFrontendFacade.kt` | K2 REPL test facade | K2 | KEEP |
| `compiler/tests-common-new/testFixtures/.../services/configuration/ScriptingEnvironmentConfigurator.kt` | plugin config | both | KEEP |
| `compiler/test-infrastructure-utils/testFixtures/.../script/scriptTestUtil.kt` | `loadScriptingPlugin` | both | KEEP |

### Generated runners

| Path | Notes | Disposition |
|---|---|---|
| `compiler/fir/fir2ir/build/tests-gen/.../FirScriptCodegenTestGenerated.java` | 40+ K2 script codegen tests | KEEP (regenerated via `./gradlew generateTests`) |
| `compiler/psi/psi-impl/tests-gen/.../PsiParsingTestGenerated.java` | includes K1 script/REPL parse cases | REMOVE corresponding script/REPL entries with K1 |

### Test data

| Path | Content | Frontend | Disposition |
|---|---|---|---|
| `compiler/testData/codegen/script/` (50+ files) | script codegen fixtures | K2 | KEEP |
| `compiler/testData/codegen/script/scriptInstanceCapturing/` (15+ files) | capture | K2 | KEEP |
| `compiler/testData/codegen/scriptCustom/` (10+ files) | custom script def codegen | mixed | AUDIT per file; MOVE K2-ones to `plugins/scripting/scripting-tests` candidate |
| `compiler/testData/codegen/boxJvm/script/` (5+ files) | box JVM script | mixed | AUDIT, likely KEEP |
| `compiler/testData/diagnostics/tests/script/` (30+ files) | FIR diagnostics | K2 | KEEP |
| `compiler/testData/diagnostics/tests/scripts/` | potential duplicate path | check | VERIFY |
| `compiler/tests-integration/testData/repl/` (~30 dirs) | K1 REPL fixtures | K1 | REMOVE |
| `compiler/tests-integration/testData/integration/smoke/script*` (scriptException, scriptDashedArgs, scriptFlushBeforeShutdown) | CLI smoke | both | KEEP |
| `compiler/psi/psi-impl/testData/psi/script/` (~17 files) | K1 PSI parse fixtures | K1 | REMOVE |
| `compiler/psi/psi-impl/testData/psi/repl/` (1 file) | K1 PSI REPL fixture | K1 | REMOVE |

### Notes

- **REPL test data tail** (`tests-integration/testData/repl/`, `GenericReplTest`) is the biggest single removal block.
- **MOVE candidates**: `FirLightTreeCustomScriptCodegenTest`, `FirPsiCustomScriptCodegenTest`, and parts of `testData/codegen/scriptCustom/` exercise the scripting plugin's public surface — natural home is `plugins/scripting/scripting-tests`, not `compiler/tests-integration`.
- After K1 retires, `psi/psi-impl/testData/psi/script,repl/` can go alongside the `KtScript` parser test cases (if the PSI `KtScript` itself goes — open).
