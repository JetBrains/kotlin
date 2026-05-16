# Current — Embedding: CLI & Daemon

## CLI integration

### Arguments

`compiler/arguments/src/.../arguments/description/CommonCompilerArguments.kt`:

| Arg | Type | Since | Status |
|---|---|---|---|
| `-script` | Boolean | 1.3.70 | active — evaluates `.kts` files |
| `-Xrepl` | Boolean | 2.2.0 | **deprecated, REMOVE** |
| `-Xdisable-default-scripting-plugin` | Boolean | — | active |

### Pipeline integration

`compiler/cli/src/.../pipeline/AbstractConfigurationPhase.kt`:

| Method | Role |
|---|---|
| `setupCommonConfiguration()` | Reads `arguments.script` / `arguments.repl` into `CompilerConfiguration`. |
| `provideCustomScriptingPluginOptions()` | Hook for custom plugin options. |
| `loadCompilerPlugins()` | Discovery + load. |
| `tryLoadScriptingPluginFromCurrentClassLoader()` | Fallback when in distribution. |

### Plugin autoload sequence

1. Check explicit `-Xplugin=` for scripting plugin presence
2. If absent, scan `libPath` for `PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS`
3. If still absent, try current ClassLoader
4. Register plugin classes:
   - `SCRIPT_PLUGIN_REGISTRAR_NAME` → `ScriptingCompilerConfigurationComponentRegistrar` (K1)
   - `SCRIPT_PLUGIN_K2_REGISTRAR_NAME` → `ScriptingK2CompilerPluginRegistrar` (K2)
   - `SCRIPT_PLUGIN_COMMANDLINE_PROCESSOR_NAME`

### Script evaluation paths

- **`-script` flag** → script treated as compilation unit → if also `ScriptEvaluationExtension` is registered, `JvmCliScriptEvaluationExtension` runs the compiled script.
- **Expression evaluation** — internal path on top of `-script`.

### CLI K2 entry chain

```
JvmScriptPipelinePhase (compiler/cli/cli-jvm/src/.../pipeline/jvm/JvmScriptPipelinePhase.kt)
  detects scriptMode / expressionToEvaluate (lines 46-59)
  → getCompilerExtensions(ScriptEvaluationExtension)
  → scriptingEvaluator.eval(configuration, projectEnvironment)
JvmCliScriptEvaluationExtension (plugins/scripting/scripting-compiler/src/.../JvmCliScriptEvaluationExtension.kt)
  createScriptCompiler() (lines 64-74)
  if (configuration.useFir && configuration.useLightTree)
    → return ScriptJvmK2CompilerFromEnvironment(environment, hostConfig)   // LightTree path
  else
    → K1 legacy path
ScriptJvmK2CompilerFromEnvironment.compile (ScriptJvmK2CompilerImpl.kt:80)
  → ScriptJvmK2CompilerImpl(state, SourceCode::convertToFirViaLightTree)
```

No CLI route uses PSI for K2 scripts.

## Daemon integration

### `CompileService` REPL methods — ALL **REMOVE**

`compiler/daemon/daemon-common/src/.../daemon/common/CompileService.kt` (lines 151–179):

| Method | Purpose |
|---|---|
| `leaseReplSession` | Allocate session |
| `releaseReplSession` | Free session |
| `replCreateState` | Create state facade |
| `replCheck` | Pre-compile check |
| `replCompile` | Compile snippet |

Supporting type: `ReplStateFacade`.

### Server-side — **REMOVE**

| File | Class | Notes |
|---|---|---|
| `compiler/daemon/src/.../daemon/KotlinRemoteReplService.kt` | `KotlinJvmReplServiceBase` | Extends `ReplCompileAction`, `ReplCheckAction`. Lazy `ReplCompiler` via `ReplFactoryExtension`. |

### Client-side — **REMOVE**

| File | Class |
|---|---|
| `compiler/daemon/daemon-client/src/main/kotlin/.../KotlinRemoteReplCompilerClient.kt` | `KotlinRemoteReplCompilerClient` |
| same dir | `RemoteReplCompilerState.kt` (serializable state for RMI) |

### CLI-base REPL helpers — **REMOVE**

`compiler/cli/cli-base/src/.../cli/common/repl/` (~13 files):

| File | Purpose |
|---|---|
| `GenericReplEvaluator.kt` | K1 REPL evaluator base |
| `GenericReplCompilingEvaluator.kt` | Compile + eval combo |
| `BasicReplState.kt`, `AggregatedReplState.kt`, `ReplState.kt`, `ReplHistory.kt` | K1 REPL state |
| `KotlinJsr223Jvm*` | Legacy JSR-223 wrappers (pre-K2) |
| plus others — full enumeration in [90-legacy-inventory.md](90-legacy-inventory.md) |

These are referenced by:
- The K1 daemon REPL service
- `JvmReplCompiler` / `JvmReplEvaluator` in `libraries/scripting/jvm-host/legacy*.kt`
- `GenericReplCompiler` in `plugins/scripting/scripting-compiler/.../repl/`

## CLI REPL shell extension

`JvmCliReplShellExtension` registered by `ScriptingCompilerConfigurationComponentRegistrar` (K1) — entry point for the in-process REPL when `-Xrepl` is used. Goes with `-Xrepl` removal.

## Status summary

| Subsystem | Status |
|---|---|
| `-script` flag | KEEP |
| Plugin autoload | KEEP |
| `-Xrepl` flag | REMOVE |
| `JvmCliReplShellExtension` | REMOVE |
| Daemon `CompileService` REPL methods | REMOVE |
| `KotlinRemoteReplService` | REMOVE |
| `KotlinRemoteReplCompilerClient` + `RemoteReplCompilerState` | REMOVE |
| `cli-base/.../cli/common/repl/*` | REMOVE |
