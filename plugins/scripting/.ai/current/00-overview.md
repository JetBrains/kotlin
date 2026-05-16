# Current — Overview

## Layer map

| Layer | Modules / Path | Frontend | Parser input | Status |
|---|---|---|---|---|
| Parsing — PSI | `compiler/psi/psi-api/.../KtScript.kt` | shared | PSI | active (used by K1 + REPL `KtFileScriptSource` branch) |
| Parsing — LightTree | `compiler/fir/raw-fir/light-tree2fir/.../LightTreeRawFirDeclarationBuilder.kt::buildScript()` | K2 | LightTree | active (sole K2 script path) |
| K1 frontend | `plugins/scripting/scripting-compiler-impl/.../resolve/*` (`LazyScriptDescriptor`, scope) | K1 | PSI | legacy |
| K2 FIR — script | `compiler/fir/tree/gen/.../FirScript.kt` + raw-fir + providers + fir2ir | K2 | **LightTree** | active |
| K2 FIR — snippet | `compiler/fir/tree/gen/.../FirReplSnippet.kt` + raw-fir + providers + fir2ir | K2 | hybrid (PSI for `KtFileScriptSource`, LT otherwise — KT-83498) | active |
| IR | `compiler/ir/ir.tree/gen/.../IrScript.kt`, `IrReplSnippet.kt` | both | n/a | active (K1 fields legacy) |
| IR lowering | `plugins/scripting/scripting-compiler/.../irLowerings/*` | both | n/a | active |
| Compiler plugin | `plugins/scripting/scripting-compiler/` | both | n/a | active (K1 path being retired) |
| K1 REPL | `plugins/scripting/scripting-compiler/.../repl/GenericReplCompiler.kt` + `compiler/cli/cli-base/.../cli/common/repl/*` | K1 | PSI | **REMOVE** |
| K2 script orchestrator (parser-agnostic) | `plugins/scripting/scripting-compiler/.../impl/ScriptJvmK2CompilerImpl.kt::ScriptJvmK2CompilerImpl` | K2 | parser-agnostic (LT by default) | active |
| K2 script entries | same file: `ScriptJvmK2CompilerIsolated`, `ScriptJvmK2CompilerFromEnvironment`, `withK2ScriptCompilerWithLightTree` | K2 | LightTree | active |
| K2 REPL orchestrator | `plugins/scripting/scripting-compiler/.../impl/K2ReplCompiler.kt` | K2 | hybrid PSI/LT (KT-83498) | active |
| Customization API | `libraries/scripting/common` | agnostic | n/a | active |
| JVM impl | `libraries/scripting/jvm` | K1-biased, K2-aware | n/a | active |
| Host / JSR-223 | `libraries/scripting/jvm-host`, `libraries/scripting/jsr223` | K2 default, K1 fallback | n/a | active (legacy files inside) |
| Dependencies | `libraries/scripting/dependencies(-maven)(-all)` | agnostic | n/a | active |
| IDE EP | `libraries/scripting/intellij` | agnostic | n/a | **REMOVAL CANDIDATE** |
| IDE services | `plugins/scripting/scripting-ide-{common,services}` | K1 | PSI | **REMOVAL CANDIDATE** |
| CLI integration | `compiler/cli/src/.../pipeline/AbstractConfigurationPhase.kt` | both | n/a | active (REPL flag deprecated) |
| Daemon REPL | `compiler/daemon/.../KotlinRemoteReplService.kt` + `daemon-common/CompileService.kt` REPL methods + client | K1 | PSI | **REMOVE** |
| Gradle | `libraries/tools/kotlin-gradle-plugin/.../scripting/*` | n/a | n/a | active |
| Build Tools API | `compiler/build-tools/.../DiscoverScriptExtensionsOperation*` | n/a | n/a | active (v2.4.0+ exp) |
| Canonical def | `libraries/tools/kotlin-main-kts/` | agnostic | n/a | active |

## Pipeline (K2 path)

```
.kts source
  │
  ▼
Parse:
  - script:  LightTree  →  LightTree2Fir / LightTreeRawFirDeclarationBuilder.buildScript()
  - snippet: hybrid     →  KtFileScriptSource → PSI (KtFile); other SourceCode → LightTree   (KT-83498 = unify on LT)
  │
  ▼
Raw FIR build
  │  FirScript builder    ◄── FirScriptConfiguratorExtensionImpl     (PSI-agnostic via KtSourceFile / KtSourceElement)
  │  FirReplSnippet build ◄── FirReplSnippetConfiguratorExtensionImpl (one PSI touch left at line 173)
  ▼
FIR resolve phases
  │  Script config        ◄── FirScriptResolutionConfigurationExtensionImpl
  │  REPL history scope   ◄── FirReplSnippetResolveExtensionImpl
  ▼
FIR → IR
  │  IrScript             ◄── Fir2IrScriptConfiguratorExtensionImpl
  │  IrReplSnippet        ◄── Fir2IrReplSnippetConfiguratorExtensionImpl
  ▼
IR lowering
  │  ScriptsToClassesLowering            (IrScript → IrClass)
  │  ReplSnippetsToClassesLowering       (IrReplSnippet → IrClass + $$eval fn)
  ▼
JVM bytecode
```

Scripts and snippets **diverge in FIR shape** (snippet wraps body in class + eval fn) but **converge to IrClass** at codegen.

## Key invariants

- **Scripts**: K2 path is **LightTree** end-to-end (`LightTreeRawFirDeclarationBuilder.buildScript()` produces `FirScript`). PSI parsing only via K1 fallback.
- **Snippets**: K2 path is **hybrid** today — see `K2ReplCompiler.kt:351-359`. PSI when `SourceCode` is `KtFileScriptSource`; LightTree otherwise. Tracking issue: **KT-83498**.
- Configurator extension SPIs (`FirScriptConfiguratorExtension`, `FirReplSnippetConfiguratorExtension`) take abstract `KtSourceFile`/`KtSourceElement` — parser-agnostic by contract. One residual PSI touch: `FirReplSnippetConfiguratorExtensionImpl.kt:173` (`scriptSource.psi as? KtScript`).
- `ScriptJvmK2CompilerImpl` is **parser-agnostic** — its ctor takes `convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile`. In practice only `convertToFirViaLightTree` is wired.

## Plugin registration entry points

| Class | File | Purpose |
|---|---|---|
| `ScriptingCompilerConfigurationComponentRegistrar` | `plugins/scripting/scripting-compiler/.../pluginRegisrar.kt` | K1 services + extensions |
| `ScriptingK2CompilerPluginRegistrar` | same file | K2 FIR extensions + script eval extension |
| `FirScriptingCompilerExtensionRegistrar` | `plugins/scripting/scripting-compiler/.../FirScriptingCompilerExtensionRegistrar.kt` | Wires FIR script extensions |
| `ReplCompilerPluginRegistrar` | `plugins/scripting/scripting-compiler/.../ReplCompilerPluginRegistrar.kt` | Wires FIR REPL snippet extensions |
