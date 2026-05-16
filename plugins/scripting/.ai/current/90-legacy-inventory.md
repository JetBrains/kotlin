# Current — Legacy Inventory

> **When to consult**: K1 audit / deletion task; before executing any REMOVE step from `target/50-migration-plan.md`.
> **Cache lifetime**: mutable-per-iteration (shrinks as steps land)
> **Last verified**: 2026-05-16

Itemized list of every K1 / PSI-tied / IDE-coupled / duplicated piece with disposition tag.

**Disposition**:
- `REMOVE` — drop entirely; nothing depends on it after upstream cleanup
- `MIGRATE` — re-implement on K2 / PSI-free path then remove the old
- `KEEP-FOR-NOW` — still needed in transition; remove once upstream allows
- `KEEP` — stays as-is

## K1 frontend bindings to scripting

| Artifact | Path | Why legacy | Disposition |
|---|---|---|---|
| `LazyScriptDescriptor` | `plugins/scripting/scripting-compiler-impl/src/.../resolve/LazyScriptDescriptor.kt` | K1 descriptor | REMOVE (with K1 frontend) |
| `LazyScriptClassMemberScope` | same dir | K1 scope | REMOVE |
| `ScriptProvidedPropertyDescriptor` | same dir | K1 synthetic prop | REMOVE |
| `ReplResultPropertyDescriptor` | same dir | K1 REPL result | REMOVE |
| `ScriptingResolveExtension` | scripting-compiler | K1 `SyntheticResolveExtension` | REMOVE |
| `ScriptExtraImportsProviderExtension` | scripting-compiler | K1 imports injection | REMOVE |
| `ScriptingCollectAdditionalSourcesExtension` | scripting-compiler | K1 source discovery | REMOVE |
| `ScriptingProcessSourcesBeforeCompilingExtension` | scripting-compiler | K1 pre-compile hook | REMOVE |
| `ScriptDefinitionProvider` / `ScriptConfigurationsProvider` K1 services | scripting-compiler | K1 registration in `ScriptingCompilerConfigurationComponentRegistrar` | REMOVE (registrations) |
| `IrScript.providedProperties`, `providedPropertiesParameters` | `compiler/ir/ir.tree/gen/.../IrScript.kt` | K1-only IR fields | REMOVE (regen schema) |

## K1 REPL

| Artifact | Path | Disposition |
|---|---|---|
| `GenericReplCompiler` | `plugins/scripting/scripting-compiler/src/.../repl/GenericReplCompiler.kt` | REMOVE |
| `cli-base` REPL helpers — see [45-embedding-daemon-legacy.md](45-embedding-daemon-legacy.md) | `compiler/cli/cli-base/src/.../cli/common/repl/*` (~13 files) | REMOVE |
| `legacyReplCompilation.kt` (`JvmReplCompiler`) | `libraries/scripting/jvm-host/` | REMOVE |
| `legacyReplEvaluation.kt` (`JvmReplEvaluator`) | `libraries/scripting/jvm-host/` | REMOVE |
| `obsoleteJvmScriptEvaluation.kt` (deprecated aliases) | `libraries/scripting/jvm-host/` | REMOVE |
| `JvmScriptCompiler.createLegacy()` + `ScriptJvmCompilerIsolated` (K1) | jvm-host + scripting-compiler | REMOVE |
| `JvmCliReplShellExtension` | scripting-compiler | REMOVE |
| `JvmStandardReplFactoryExtension` | scripting-compiler | REMOVE |
| `ReplFactoryExtension` EP | scripting-compiler | REMOVE |

## CLI

| Artifact | Path | Disposition |
|---|---|---|
| `-Xrepl` flag | `compiler/arguments/.../CommonCompilerArguments.kt` | REMOVE |
| `replMode` field plumbing | `compiler/cli/.../AbstractConfigurationPhase.kt` | REMOVE |

## Daemon REPL

| Artifact | Path | Disposition |
|---|---|---|
| `CompileService.leaseReplSession` / `releaseReplSession` / `replCreateState` / `replCheck` / `replCompile` | `compiler/daemon/daemon-common/src/.../CompileService.kt` | REMOVE |
| `ReplStateFacade` | daemon-common | REMOVE |
| `KotlinRemoteReplService` (`KotlinJvmReplServiceBase`) | `compiler/daemon/src/.../KotlinRemoteReplService.kt` | REMOVE |
| `KotlinRemoteReplCompilerClient` | `compiler/daemon/daemon-client/src/main/kotlin/.../KotlinRemoteReplCompilerClient.kt` | REMOVE |
| `RemoteReplCompilerState` | same dir | REMOVE |

## IDE-coupled

| Artifact | Path | Why | Disposition |
|---|---|---|---|
| `scripting-ide-services` | `plugins/scripting/scripting-ide-services/` | K1 PSI-based REPL completion / analyzer (`KJvmReplCompleter`, `IdeLikeReplCodeAnalyzer`, `KJvmReplCompilerWithIdeServices`) | REMOVE (user-flagged) |
| `scripting-ide-services-embeddable` | same level | Packaging of above | REMOVE |
| `scripting-ide-services-test` | same level | Tests for above | REMOVE |
| `scripting-ide-common` | `plugins/scripting/scripting-ide-common/` | **Copied from IntelliJ Community plugin** (per its README). K1+PSI. | REMOVE (future reimplementation possible in different form, definitely without K1) |

## Script definition discovery

| Artifact | Path | Why | Disposition |
|---|---|---|---|
| `ScriptDefinitionsFromClasspathDiscoverySource` | `plugins/scripting/scripting-compiler-impl/src/.../definitions/` | Deprecated KT-82551, suppressed deprecation in tree | KEEP-FOR-NOW (open question) |
| `CliScriptDefinitionProvider` | `plugins/scripting/scripting-compiler/src/.../definitions/` | Transitional | MIGRATE (collapse into FIR provider) |

## Duplicated implementations

| Concern | K1 path | K2 path | Resolution |
|---|---|---|---|
| Script compiler | `ScriptJvmCompilerIsolated` | `ScriptJvmK2CompilerIsolated` | Keep K2 only |
| REPL compiler | `GenericReplCompiler` + cli-base/repl/* | `K2ReplCompiler` | Keep K2 only |
| REPL evaluator | wrappers in `legacyRepl*.kt` | `K2ReplEvaluator` | Keep K2 only |
| Script evaluator | `BasicJvmScriptEvaluator` (K1 branches) | same file (K2 branches via `isCompiledWithK2`) | Collapse to K2-only branches |
| JSR-223 engine | K1 path via `legacyRepl*` (not exposed via factory anymore) | `KotlinJsr223ScriptEngineImpl` | K2 only |
| Plugin registrar | `ScriptingCompilerConfigurationComponentRegistrar` (K1) | `ScriptingK2CompilerPluginRegistrar` | Keep K2 only (after K1 frontend dies) |

## Stays

| Artifact | Why |
|---|---|
| `libraries/scripting/intellij` | Public API surface (`IdeScriptConfigurationControlFacade` EP) used by IntelliJ plugin authors wiring custom-scripts support |
| `KtScript` (PSI) + stub | K1 fallback; REPL `KtFileScriptSource` branch (until KT-83498) |
| `FirScript`, `FirReplSnippet`, configurator extensions | Active K2 path |
| `IrScript`, `IrReplSnippet`, lowerings | Active |
| `ScriptJvmK2CompilerImpl` + `convertToFirViaLightTree` | Parser-agnostic core; sole production K2 script path |
| `K2ReplCompiler`, `K2ReplCompilationState` | Active (hybrid parsing — KT-83498 to remove PSI branch) |
| `JvmScriptCompiler` (K2 path), `KotlinJsr223ScriptEngineImpl`, `KotlinJsr223DefaultScriptEngineFactory` | Active |
| `libraries/scripting/common`, `dependencies(-maven)(-all)` | Active |
| `kotlin-main-kts` | Active canonical def |
| `Gradle scripting subplugin`, `DiscoverScriptExtensionsOperation` (BTA) | Active |
| `-script` flag, plugin autoload | Active |

## Compiler-side tests (REMOVE / MOVE)

See [70-tests.md](70-tests.md#compiler-side-scripting-tests-under-compiler) for the full table. Headline items:

| Artifact | Path | Disposition |
|---|---|---|
| `ScriptGenTest` (K1) | `compiler/tests-integration/tests/.../codegen/ScriptGenTest.kt` | REMOVE |
| `GenericReplTest` (K1 REPL) | `compiler/tests-integration/tests/.../cli/jvm/repl/GenericReplTest.kt` | REMOVE |
| K1 REPL test data | `compiler/tests-integration/testData/repl/*` (~30 dirs) | REMOVE |
| K1 PSI script parse tests + fixtures | `compiler/psi/psi-impl/tests/.../CustomPsiTest` (script cases) + `compiler/psi/psi-impl/testData/psi/{script,repl}/` | REMOVE (with K1 PSI retirement) |
| Custom-script codegen tests | `compiler/tests-integration/tests/.../codegen/Fir{LightTree,Psi}CustomScriptCodegenTest` | MOVE → `plugins/scripting/scripting-tests` |
| `compiler/testData/codegen/scriptCustom/` | mixed K1/K2 fixtures | AUDIT — segregate, MOVE K2 ones |

## Pending work (not legacy, but on roadmap)

| Item | Issue | Notes |
|---|---|---|
| Full LightTree path for `K2ReplCompiler` | **KT-83498** | Removes PSI from snippet parsing — line anchors in [`10-compiler-representation.md`](10-compiler-representation.md); design in [`../target/50-migration-plan.md`](../target/50-migration-plan.md) step 2 |
| Eliminate residual PSI touch (`scriptSource.psi as? KtScript`) in `FirReplSnippetConfiguratorExtensionImpl` | (KT-83498 related) | Last PSI touch in snippet configurator impl |
