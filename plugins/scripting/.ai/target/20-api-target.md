# Target — API Layer

Target shape for `libraries/scripting/*`.

## Module dispositions

| Module | Target |
|---|---|
| `common` | KEEP unchanged — public API surface |
| `jvm` | KEEP; remove K1 branches inside `BasicJvmScriptEvaluator` (drop `isCompiledWithK2` checks once K1 is dead) |
| `jvm-host` | KEEP; collapse K2 default + K1 fallback → K2 only |
| `jvm-host-embeddable` | KEEP — packaging |
| `jsr223` | KEEP; finish K2 port (see [40-jsr223-target.md](40-jsr223-target.md)) |
| `jsr223-embeddable` | KEEP — packaging |
| `dependencies`, `dependencies-maven`, `dependencies-maven-all` | KEEP unchanged |
| `intellij` | **KEEP** — public API surface (`IdeScriptConfigurationControlFacade` EP) used by IntelliJ plugin authors to wire custom-scripts support |

## `jvm-host` collapse

Files to delete:

| File | Reason |
|---|---|
| `legacyReplCompilation.kt` (`JvmReplCompiler`) | K1 REPL wrapper |
| `legacyReplEvaluation.kt` (`JvmReplEvaluator`) | K1 REPL wrapper |
| `obsoleteJvmScriptEvaluation.kt` | Deprecated aliases (some already `DeprecationLevel.ERROR`) |

API surface changes:

| Before | After |
|---|---|
| `JvmScriptCompiler()` → K2; `JvmScriptCompiler.createLegacy()` → K1 | Single ctor; remove `createLegacy()` |
| Two `JvmRepl*` classes (legacy) + K2 engine | Only `KotlinJsr223ScriptEngineImpl` (K2) |

## `BasicJvmScriptEvaluator` cleanup

Today:
- Checks `isCompiledWithK2` flag (line ~75) to branch eval logic
- Reflective attribute access differs K1 vs K2 (lines ~92, ~106)

After: drop K1 branches. Single eval path.

## `intellij` module

`libraries/scripting/intellij/` stays. Small, stable surface used by IntelliJ plugin authors. No K1 / PSI / IntelliJ-platform leakage.

## Dependency graph (after)

Same as current — no module added, none removed (except possibly `intellij`).

```
common (API)
  ↑
  ├── jvm (JVM impls — K2 branches only)
  │     ↑
  │     └── jvm-host (single JvmScriptCompiler, single REPL engine)
  │           ↑
  │           └── jsr223 (public factory, complete K2 bindings)
  │
  └── dependencies
        ↑
        └── dependencies-maven → dependencies-maven-all (shaded)
```

## Tests

After cleanup, in `libraries/scripting/jvm-host-test/`:
- Remove `LegacyReplTest`
- Remove `expectTestToFailOnK2` markers (every K2-failing test must pass or be deleted)
- `ResolveDependenciesTest` must work on K2 (see [40-jsr223-target.md](40-jsr223-target.md))

## What does NOT change

- `ScriptCompilationConfiguration` / `ScriptEvaluationConfiguration` API
- Refinement DSL
- `ScriptCompiler` / `ScriptEvaluator` / `ReplCompiler` / `ReplEvaluator` interfaces
- `ExternalDependenciesResolver` SPI
- main-kts (canonical example)
