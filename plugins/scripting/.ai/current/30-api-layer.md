# Current — API Layer

> **When to consult**: edits in `libraries/scripting/*` (modules, K2 core wrappers, K1/K2 gap audit).
> **Cache lifetime**: stable
> **Last verified**: 2026-05-16

Module catalog for `libraries/scripting/*`.

## Module table

| Module | Path | Role | Frontend | Status | Tests |
|---|---|---|---|---|---|
| common | `libraries/scripting/common/` | API contracts: `ScriptCompiler`, `ScriptEvaluator`, `ReplCompiler/Evaluator`, `ScriptCompilationConfiguration`, etc. | agnostic | active | `common/test/` |
| jvm | `libraries/scripting/jvm/` | JVM evaluator (`BasicJvmScriptEvaluator`, `BasicJvmReplEvaluator`), `JvmDependency`, `KJvmCompiledScript`. K2-aware via `isCompiledWithK2` flag. | K1-biased | active | `jvm/test/` |
| jvm-host | `libraries/scripting/jvm-host/` | Compilation entry: `JvmScriptCompiler` (K2 default) + `createLegacy()` (K1). `KotlinJsr223ScriptEngineImpl` (K2 REPL engine). Hosts: `BasicJvmScriptingHost`. | K2 default, K1 fallback | active | `jvm-host-test/` |
| jvm-host-embeddable | `libraries/scripting/jvm-host-embeddable/` | Packaging only | — | active | — |
| jsr223 | `libraries/scripting/jsr223/` | Public JSR-223 factory: `KotlinJsr223DefaultScriptEngineFactory` + `KotlinJsr223DefaultScript` template. | K2 | active | `jsr223-test/` |
| jsr223-embeddable | `libraries/scripting/jsr223-embeddable/` | Packaging only | — | active | — |
| dependencies | `libraries/scripting/dependencies/` | API: `ExternalDependenciesResolver`, `RepositoryCoordinates`, `CompoundDependenciesResolver`, `FileSystemDependenciesResolver`. | agnostic | active | `dependencies/test/` |
| dependencies-maven | `libraries/scripting/dependencies-maven/` | Aether impl (`MavenDependenciesResolver`). Maven 3.8.8 / Resolver 1.9.22. | agnostic | active | `dependencies-maven/test/` |
| dependencies-maven-all | `libraries/scripting/dependencies-maven-all/` | Shaded distribution with relocated Aether. Includes CVE pins (Guava CVE-2023-2976, commons-codec WS-2019-0379). | agnostic | active | smoke test |
| intellij | `libraries/scripting/intellij/` | `IdeScriptConfigurationControlFacade` EP. Public API surface for IntelliJ plugin authors wiring custom-scripts support. | agnostic | active (KEEP) | — |
| jvm-host-test | `libraries/scripting/jvm-host-test/` | Integration tests (REPL, host, caching, deps). `expectTestToFailOnK2` markers. | both | active | — |
| jsr223-test | `libraries/scripting/jsr223-test/` | JSR-223 compliance + manual memory tests. | K2 | active | — |

## Legacy / obsolete files inside `jvm-host`

| File | What | Status |
|---|---|---|
| `legacyReplCompilation.kt` | `JvmReplCompiler` wrapping K1 cli-base REPL classes | **legacy** |
| `legacyReplEvaluation.kt` | `JvmReplEvaluator` wrapping K1 cli-base REPL | **legacy** |
| `obsoleteJvmScriptEvaluation.kt` | Deprecated aliases (some `DeprecationLevel.ERROR`) | **legacy** |
| `JvmScriptCompiler.createLegacy()` | K1 entry path | **legacy** |

## Dependency graph

```
common (base API)
  ↑
  ├── jvm (JVM impls)
  │     ↑
  │     └── jvm-host (compilation + JSR-223 engine)
  │           ↑
  │           └── jsr223 (public factory)
  │
  └── dependencies (resolver API)
        ↑
        └── dependencies-maven (Aether impl)
              ↑
              └── dependencies-maven-all (shaded)

intellij (independent, IDE EP)
```

## K2 compilation core (lives in `plugins/scripting/scripting-compiler`)

| Layer | Class / fn | File |
|---|---|---|
| Parser-agnostic core | `ScriptJvmK2CompilerImpl` | `plugins/scripting/scripting-compiler/src/.../impl/ScriptJvmK2CompilerImpl.kt:109` |
| Host-side wrapper | `ScriptJvmK2CompilerIsolated` (calls `withK2ScriptCompilerWithLightTree`) | same file:60 |
| CLI-side wrapper | `ScriptJvmK2CompilerFromEnvironment` (takes prepared `KotlinCoreEnvironment`) | same file:80 |
| LT converter | `convertToFirViaLightTree` (top-level extension) | same file:317 |

The core is **parser-agnostic** (`convertToFir` lambda in ctor). Every production wrapper passes `SourceCode::convertToFirViaLightTree`. PSI converter does not exist.

## K1 vs K2 in jvm-host

| Concern | K2 path | K1 fallback |
|---|---|---|
| Script compiler | `JvmScriptCompiler()` → `ScriptJvmK2CompilerIsolated` (LT) | `JvmScriptCompiler.createLegacy()` → `ScriptJvmCompilerIsolated` (PSI/K1) |
| REPL compiler | `K2ReplCompiler` (from scripting-compiler) via `KotlinJsr223ScriptEngineImpl` | `JvmReplCompiler` in `legacyReplCompilation.kt` |
| REPL evaluator | `K2ReplEvaluator` | `JvmReplEvaluator` in `legacyReplEvaluation.kt` |
| JSR-223 engine | `KotlinJsr223ScriptEngineImpl` (single class — `K2ReplState`) | None — only K2 engine is exposed via factory |
| Script eval | `BasicJvmScriptEvaluator` w/ `isCompiledWithK2` branches | same evaluator, K1 branches |

## Known K2 gaps (per tests)

- `ResolveDependenciesTest` — K2 lacks classloader-reflection-based dep extraction (`KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY`). Marked `expectTestToFailOnK2`.
- JSR-223 bindings — partial K2 port (`04ecbd1f8a7f`); bindings not fully ported due to different `FirReplSnippet` shape vs `FirScript`.
- Remote (out-of-process) compilation — no K2 scenario.
