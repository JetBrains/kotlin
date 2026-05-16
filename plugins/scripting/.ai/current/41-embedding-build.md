# Current — Embedding: Build Systems

> **When to consult**: Gradle subplugin or Build Tools API integration changes.
> **Cache lifetime**: stable
> **Last verified**: 2026-05-16

## Gradle integration

Subplugin auto-applies when the Kotlin Gradle Plugin is applied.

| Element | File | Role |
|---|---|---|
| `ScriptingGradleSubplugin` | `libraries/tools/kotlin-gradle-plugin/src/common/kotlin/.../scripting/ScriptingGradleSubplugin.kt` | Entry point; per-sourceset config via `configureForSourceSet()`. Creates discovery classpath configs (`kotlinScriptDef`, `kotlinScriptDefExtensions`). |
| `ScriptingExtension` | same dir | DSL extension for user-facing scripting config in Gradle scripts. |
| `ScriptingKotlinGradleSubplugin` | same dir | Internal variant supporting configuration refinement. |

### Discovery flow

1. Gradle resolves `kotlinScriptDef` config (contains script definition jars).
2. Artifact transform applied to discovery classpath — invokes `DiscoverScriptExtensionsOperation` from Build Tools API.
3. Discovered extensions (`.kts`, `.main.kts`, `.gradle.kts`, etc.) feed into `task.scriptDefinitions` for `KotlinCompile` tasks.

## Build Tools API

`compiler/build-tools/`:

| Element | File | Role |
|---|---|---|
| `DiscoverScriptExtensionsOperation` | `kotlin-build-tools-api/src/main/kotlin/.../jvm/operations/DiscoverScriptExtensionsOperation.kt` | API: `BuildOperation<Collection<String>>`. Since v2.4.0 (experimental). Input: classpath. Output: discovered script file extensions. |
| `DiscoverScriptExtensionsOperationImpl` | `kotlin-build-tools-impl/src/main/kotlin/.../jvm/operations/DiscoverScriptExtensionsOperationImpl.kt` | Impl wrapping classpath discovery via `ScriptDefinitionsFromClasspathDiscoverySource`. |
| `COMPILER_MESSAGE_RENDERER` option | same | Diagnostic format selector. |

## Status

| Subsystem | Status |
|---|---|
| Gradle scripting subplugin | KEEP, active |
| Build Tools API script-extension discovery | KEEP, active (stable target) |

Both are decoupled from frontend choice — work with K1 and K2. No PSI / IntelliJ-platform leakage.
