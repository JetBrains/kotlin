# Target — Embedding: CLI, Daemon, Gradle, BTA

## CLI compiler

| Concern | Target |
|---|---|
| `-script` flag + script-as-source pipeline | KEEP unchanged |
| Plugin autoload (`AbstractConfigurationPhase.loadCompilerPlugins`) | KEEP unchanged |
| `-Xrepl` flag | **REMOVE** |
| `replMode` plumbing in `CompilerConfiguration` | REMOVE |
| `JvmCliReplShellExtension` | REMOVE |
| `JvmCliScriptEvaluationExtension` (`ScriptEvaluationExtension`) | KEEP — backs `-script`; entry chain: `JvmScriptPipelinePhase` → `createScriptCompiler()` → `ScriptJvmK2CompilerFromEnvironment` (LT path) |
| K1 branch in `JvmCliScriptEvaluationExtension.createScriptCompiler()` | REMOVE — drop the `useFir/useLightTree` conditional once K1 retires; keep only the K2/LT path |

After: `kotlinc -script foo.kts` works; `kotlinc -Xrepl` is gone.

## Daemon

**All REPL state on the daemon goes.**

| Concern | Target |
|---|---|
| `CompileService.leaseReplSession` / `releaseReplSession` / `replCreateState` / `replCheck` / `replCompile` | REMOVE |
| `ReplStateFacade` interface | REMOVE |
| `KotlinRemoteReplService` / `KotlinJvmReplServiceBase` | REMOVE |
| `KotlinRemoteReplCompilerClient` | REMOVE |
| `RemoteReplCompilerState` | REMOVE |

Daemon still compiles regular `.kt` and `.kts` files — only the REPL session machinery goes.

## CLI-base REPL helpers — REMOVE

All of `compiler/cli/cli-base/src/.../cli/common/repl/*` (~13 files). Inventory:

| File | What |
|---|---|
| `GenericReplEvaluator.kt` | K1 REPL evaluator |
| `GenericReplCompilingEvaluator.kt` | Combo |
| `BasicReplState.kt` | State |
| `AggregatedReplState.kt` | State |
| `ReplState.kt` | Base state |
| `ReplHistory.kt` | History |
| `KotlinJsr223Jvm*.kt` | Legacy JSR-223 wrappers |
| + remaining | Various K1 REPL helpers |

(Run `find compiler/cli/cli-base/src -path '*cli/common/repl/*'` to get the precise current list.)

Removal order: stop callers first (daemon REPL → jvm-host `legacyRepl*` → scripting-compiler `GenericReplCompiler`), then delete.

## Gradle

| Concern | Target |
|---|---|
| `ScriptingGradleSubplugin`, `ScriptingExtension`, `ScriptingKotlinGradleSubplugin` | KEEP unchanged |
| Discovery flow via BTA `DiscoverScriptExtensionsOperation` | KEEP unchanged |

## Build Tools API

| Concern | Target |
|---|---|
| `DiscoverScriptExtensionsOperation` (v2.4.0+, experimental) | Stabilize when ready |
| New operations for remote/sandboxed script compilation | **OPEN** — if we keep JSR-223 remote scenario, this is where it lives, not daemon |

## Plugin registrars

| Concern | Target |
|---|---|
| `ScriptingK2CompilerPluginRegistrar` | KEEP — sole registrar |
| `ScriptingCompilerConfigurationComponentRegistrar` (K1) | REMOVE |

## Script definition discovery

| Concern | Target |
|---|---|
| `FirScriptDefinitionProviderService` (K2, FIR component) | KEEP — primary path |
| `CliScriptDefinitionProvider` | MIGRATE — collapse responsibilities into FIR provider |
| `ScriptDefinitionsFromClasspathDiscoverySource` (deprecated KT-82551) | **OPEN** — un-deprecate or supersede (see [90-open-questions.md](90-open-questions.md)) |
| `META-INF/kotlin/script/templates/*.classname` markers | KEEP for compatibility; document the SPI |

## Net effect

| Subsystem | Before | After |
|---|---|---|
| CLI | `-script` + `-Xrepl` + autoload | `-script` + autoload |
| Daemon | Regular compile + REPL methods + remote REPL | Regular compile only |
| Gradle | KGP subplugin + BTA discovery | Unchanged |
| BTA | Discovery op (experimental) | Discovery op stable; possibly new remote-compile op |
| Embedded REPL | Via JSR-223 K2 engine + K1 legacy wrappers + daemon | JSR-223 K2 engine only |
