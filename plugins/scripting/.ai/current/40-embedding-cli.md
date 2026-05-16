# Current — Embedding: CLI (active)

> **When to consult**: `-script` flag, plugin autoload, CLI K2 entry chain.
> **Cache lifetime**: stable
> **Last verified**: 2026-05-16

Legacy daemon + cli-base REPL surface is split out into [`45-embedding-daemon-legacy.md`](45-embedding-daemon-legacy.md). Read that file only when executing migration steps 4, 5, or 6.

## Arguments

`compiler/arguments/src/.../arguments/description/CommonCompilerArguments.kt`:

| Arg | Type | Since | Status |
|---|---|---|---|
| `-script` | Boolean | 1.3.70 | active — evaluates `.kts` files |
| `-Xdisable-default-scripting-plugin` | Boolean | — | active |

(`-Xrepl` is REMOVE — see [`45-embedding-daemon-legacy.md`](45-embedding-daemon-legacy.md).)

## Pipeline integration

`compiler/cli/src/.../pipeline/AbstractConfigurationPhase.kt`:

| Method | Role |
|---|---|
| `setupCommonConfiguration()` | Reads `arguments.script` / `arguments.repl` into `CompilerConfiguration`. |
| `provideCustomScriptingPluginOptions()` | Hook for custom plugin options. |
| `loadCompilerPlugins()` | Discovery + load. |
| `tryLoadScriptingPluginFromCurrentClassLoader()` | Fallback when in distribution. |

## Plugin autoload sequence

1. Check explicit `-Xplugin=` for scripting plugin presence
2. If absent, scan `libPath` for `PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS`
3. If still absent, try current ClassLoader
4. Register plugin classes:
   - `SCRIPT_PLUGIN_REGISTRAR_NAME` → `ScriptingCompilerConfigurationComponentRegistrar` (K1)
   - `SCRIPT_PLUGIN_K2_REGISTRAR_NAME` → `ScriptingK2CompilerPluginRegistrar` (K2)
   - `SCRIPT_PLUGIN_COMMANDLINE_PROCESSOR_NAME`

## Script evaluation paths

- **`-script` flag** → script treated as compilation unit → if also `ScriptEvaluationExtension` is registered, `JvmCliScriptEvaluationExtension` runs the compiled script.
- **Expression evaluation** — internal path on top of `-script`.

## CLI K2 entry chain

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

## Status summary

| Subsystem | Status |
|---|---|
| `-script` flag | KEEP |
| Plugin autoload | KEEP |
| `JvmCliScriptEvaluationExtension` | KEEP |
| `ScriptJvmK2CompilerFromEnvironment` (CLI wrapper) | KEEP |
