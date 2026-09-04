# Target — Compiler Representation

> **When to consult**: designing or executing compiler-layer cleanup (parsing/FIR/IR/lowerings/registrars).
> **Cache lifetime**: stable
> **Last verified**: 2026-05-16

What stays, what dies, what gets refactored. Read alongside [current/10-compiler-representation.md](../current/10-compiler-representation.md).

## Layer-by-layer target

### Parsing

| Element | Target |
|---|---|
| `KtScript`, `KotlinScriptStubImpl`, `KtScriptElementType` (PSI) | KEEP while K1 still alive (the REPL `KtFileScriptSource` branch is gone — KT-83498 landed 2026-09-04). Re-audit when the K1 frontend retires. |
| `LightTreeRawFirDeclarationBuilder.buildScript()` (LT) | KEEP — **sole K2 script parsing path** in production |
| LightTree for `FirReplSnippet` | **Landed 2026-09-04 (KT-83498 / KT-77583)** — `LightTreeRawFirDeclarationBuilder.convertReplSnippet` over the shared `AbstractRawFirBuilder.convertReplSnippetImpl`; `K2ReplCompiler` has no PSI branch |

### K1 frontend

Everything **REMOVE**. List in [current/90-legacy-inventory.md](../current/90-legacy-inventory.md#k1-frontend-bindings-to-scripting).

### K2 FIR — script

| Element | Target |
|---|---|
| `FirScript` + builder + symbol + receivers | KEEP |
| `FirScriptConfiguratorExtension(Impl)` | KEEP |
| `FirScriptResolutionConfigurationExtension(Impl)` | KEEP |
| `FirScriptDeclarationsScope` | KEEP |
| `Fir2IrScriptConfiguratorExtension(Impl)` | KEEP |

No structural changes expected. Possibly remove the `accepts()` extension method on configurator if no longer needed once K1 is gone.

### K2 FIR — REPL snippet

| Element | Target |
|---|---|
| `FirReplSnippet` (embedded class + eval fn) | KEEP — current shape |
| `FirReplSnippetConfiguratorExtension(Impl)` | KEEP, **extend for bindings** (see [40-jsr223-target.md](40-jsr223-target.md)) |
| `FirReplSnippetResolveExtension(Impl)` | KEEP |
| `FirReplHistoryProvider`, `FirReplHistoryScope` | KEEP |
| `FirReplDeclarationReference` / `FirReplExpressionReference` / `FirReplPropertyDelegate` / `FirReplPropertyInitializer` | KEEP |
| `Fir2IrReplSnippetConfiguratorExtension(Impl)` | KEEP |

**Refactor target**: tighten the configurator API so that binding/provided-property injection has a clean path. Today injection happens implicitly via mutation; documented hook would help.

### IR

| Element | Target |
|---|---|
| `IrScript` | KEEP; **drop K1-only fields** `providedProperties`, `providedPropertiesParameters` from generator schema |
| `IrReplSnippet` | KEEP |
| `ScriptsToClassesLowering` | KEEP; trim K1 branches |
| `ReplSnippetsToClassesLowering` | KEEP |

### Orchestrators

| Element | Target |
|---|---|
| `ScriptJvmK2CompilerImpl` (parser-agnostic core) | KEEP — model REPL pipeline on this shape |
| `ScriptJvmK2CompilerIsolated` (host wrapper) | KEEP |
| `ScriptJvmK2CompilerFromEnvironment` (CLI wrapper) | KEEP — sole K2 CLI entry |
| `convertToFirViaLightTree` | KEEP — only converter wired today |
| `K2ReplCompiler` + `K2ReplCompilationState` | KEEP. Aligned with `ScriptJvmK2CompilerImpl` (`convertToFir` seam, LT default) since KT-83498. Next: route annotation refinement through `refineAllForK2` instead of `CliScriptConfigurationsProvider` (PSI inside), and narrow `isReplSnippetSource` (G15). |
| `GenericReplCompiler` | REMOVE |
| `ScriptJvmCompilerIsolated` (K1) | REMOVE |

### Plugin registrars

| Element | Target |
|---|---|
| `ScriptingK2CompilerPluginRegistrar` | KEEP — single registrar |
| `FirScriptingCompilerExtensionRegistrar`, `ReplCompilerPluginRegistrar` | KEEP — split is fine |
| `ScriptingCompilerConfigurationComponentRegistrar` (K1) | REMOVE |

## LightTree status

`FirScript`: **done** — `LightTreeRawFirDeclarationBuilder.buildScript()` produces `FirScript`; `ScriptJvmK2CompilerImpl` wires it via `convertToFirViaLightTree`; CLI uses LT exclusively.

`FirReplSnippet`: **done (KT-83498, 2026-09-04)** — `LightTreeRawFirDeclarationBuilder.convertReplSnippet` produces it (golden parity with PSI on all `*.repl.kts` raw-builder fixtures); `K2ReplCompiler` converts every `SourceCode` through `convertToFir` (LT default). See [`50-migration-plan.md`](50-migration-plan.md#2-land-kt-83498--full-lighttree-path-for-k2replcompiler) for the design record and [`../current/10-compiler-representation.md`](../current/10-compiler-representation.md) for line anchors.

## Net effect after cleanup

- One compiler representation pipeline per concept (script, snippet) — modelled on `ScriptJvmK2CompilerImpl`
- No K1 descriptors in scripting modules
- No PSI dependency in API/host modules (already true today)
- Scripts: zero PSI on K2 path (already true today)
- Snippets: zero PSI in `K2ReplCompiler` / FIR configurators (KT-83498 landed); residual PSI only in the `CliScriptConfigurationsProvider` refinement path shared with K1 (goes with step 8)
