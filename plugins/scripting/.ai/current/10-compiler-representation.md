# Current — Compiler Representation

> **When to consult**: any compiler-side edit (PSI/LT/FIR/IR/lowerings/EPs). Canonical home for the (landed) KT-83498 line anchors (`K2ReplCompiler.kt:71-73` seam, `:412` conversion, `:341-345` refined-config pre-seeding; `FirReplSnippetConfiguratorExtensionImpl.kt:173` `currentLineId`; `LightTreeRawFirDeclarationBuilder.kt:2978` / `:3001`; `AbstractRawFirBuilder.kt:1480` shared impl) and the 6 configurator EPs enumeration.
> **Cache lifetime**: stable
> **Last verified**: 2026-05-16

How a script (or REPL snippet) flows from source text to bytecode.

## 1. Parsing

### PSI (used by K1 and REPL `KtFileScriptSource` branch)

| Element | File | Notes |
|---|---|---|
| `KtScript` | `compiler/psi/psi-api/src/.../psi/KtScript.kt` | Extends `KtNamedDeclarationStub<KotlinScriptStub>`. Holds `blockExpression`, `fqName`. |
| `KtScript.isReplSnippet` | same | Marker via copyable user data key — distinguishes REPL snippets from regular scripts. |
| `KtScript.replSnippetClassId` | same | Experimental — REPL snippet class id (set externally). |
| `KotlinScriptStubImpl` | `compiler/psi/psi-impl/src/.../stubs/impl/KotlinScriptStubImpl.kt` | Stores `fqName` + `isReplSnippet` flag for indexing. |
| `KtScriptElementType` | `compiler/psi/psi-impl/src/.../stubs/elements/KtScriptElementType.kt` | Stub serialization. |

### LightTree (sole K2 script path; partial for REPL)

| Element | File | Notes |
|---|---|---|
| `LightTree2Fir` | `compiler/fir/raw-fir/light-tree2fir/src/.../lightTree/LightTree2Fir.kt` | Parses source via `KotlinLightParser.buildLightTree()`; delegates to `LightTreeRawFirDeclarationBuilder.convertFile()`. |
| `LightTreeRawFirDeclarationBuilder.buildScript()` | `compiler/fir/raw-fir/light-tree2fir/src/.../converter/LightTreeRawFirDeclarationBuilder.kt:2884-2920` | Detects `SCRIPT` token at file level (line 108: `SCRIPT -> scriptNodes += child`), collects script declarations (`SCRIPT_DECLARATION_TOKENS`: `CLASS`, `FUN`, `PROPERTY`, `TYPEALIAS`, `OBJECT_DECLARATION`, `CLASS_INITIALIZER`, `MODIFIER_LIST`, `SCRIPT_INITIALIZER`, `DESTRUCTURING_DECLARATION`), converts `SCRIPT_INITIALIZER` → `FirAnonymousInitializer`, builds `FirScript` with `FirScriptSymbol(packageFqName.child(scriptName))`. |
| `convertToFirViaLightTree` (extension) | `plugins/scripting/scripting-compiler/src/.../impl/ScriptJvmK2CompilerImpl.kt:317-327` | `SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile`. Wraps `LightTree2Fir` for `SourceCode`. Used by every K2 script entry. |

**Note on `FirReplSnippet`** (KT-83498 / KT-77583 landed 2026-09-04): both builders produce it. The parser-independent part (snippet `object`, `$$eval`, primary constructor, `FirReplDeclarationReference` / `FirReplPropertyInitializer` / `FirReplPropertyDelegate` rewriting, `replSnippetDelegatedPropertyCopies`) lives in `AbstractRawFirBuilder.convertReplSnippetImpl` (`raw-fir.common/.../AbstractRawFirBuilder.kt:1480`); `PsiRawFirBuilder.Visitor.convertReplSnippet` (`:1480`) and `LightTreeRawFirDeclarationBuilder.convertReplSnippet` (`:2978`) only supply the parser-specific `extractReplElements`. The `bindFunctionTarget` / `replSnippetDeclarationSymbol` hooks overridden by the Analysis API (`RawFirNonLocalDeclarationBuilder`) moved to the common base class. Script-vs-snippet decision: LT — `FirReplSnippetConfiguratorExtension.isReplSnippetsSource` (EP); PSI — `KtScript.isReplSnippet` marker (KT-84387). `*.repl.kts` raw-builder fixtures run under both builders (`LightTree2FirConverterTestCaseGenerated`, no `.lt.txt` overrides).

## 2. K1 frontend (legacy)

| Element | File | Notes |
|---|---|---|
| `LazyScriptDescriptor` | `plugins/scripting/scripting-compiler-impl/src/.../resolve/LazyScriptDescriptor.kt` | Extends `ScriptDescriptor` + `LazyClassDescriptor`. Reads config from `ScriptConfigurationsProvider`. Builds ctor params in 4 categories (earlierScripts, baseClass, implicit receivers, provided properties). |
| `LazyScriptClassMemberScope` | `plugins/scripting/scripting-compiler-impl/src/.../resolve/LazyScriptClassMemberScope.kt` | Member scope for K1 script class. |
| `ScriptProvidedPropertyDescriptor` | `plugins/scripting/scripting-compiler-impl/.../resolve/` | Synthetic property for provided values. |
| `ReplResultPropertyDescriptor` | same | K1 REPL result-field descriptor. |
| `ScriptingResolveExtension` | scripting-compiler | K1 `SyntheticResolveExtension` for scripts. |
| `ScriptExtraImportsProviderExtension` | scripting-compiler | K1 imports injection. |
| `ScriptingCollectAdditionalSourcesExtension` | scripting-compiler | K1 source discovery. |
| `ScriptingProcessSourcesBeforeCompilingExtension` | scripting-compiler | K1 pre-compile hook. |

All K1, all PSI-tied, all going away with K1 frontend retirement.

## 3. K2 / FIR — script

| Element | File | Notes |
|---|---|---|
| `FirScript` | `compiler/fir/tree/gen/.../declarations/FirScript.kt` (generated) | `name`, `declarations`, `parameters: List<FirProperty>`, `receivers: List<FirScriptReceiverParameter>`, `resultPropertyName`, `symbol`. |
| `FirScriptImpl` | `compiler/fir/tree/gen/.../declarations/impl/FirScriptImpl.kt` | Generated impl. |
| `FirScriptBuilder` | generated, same dir | DSL builder used during raw FIR construction. |
| `FirScriptSymbol` | generated | Identified by `FqName`. |
| `FirScriptReceiverParameter` | generated, same dir | Implicit receiver param. |
| `FirScriptConfiguratorExtension` | `compiler/fir/raw-fir/raw-fir.common/src/.../builder/FirScriptConfiguratorExtension.kt` | EP. Methods: `accepts(sourceFile, scriptSource)`, `FirScriptBuilder.configureContainingFile`, `FirScriptBuilder.configure(sourceFile, context)`. |
| `FirScriptConfiguratorExtensionImpl` | `plugins/scripting/scripting-compiler/src/.../services/FirScriptConfigurationExtensionImpl.kt` | Resolves base class, adds receivers + provided props, reports config errors. |
| `FirScriptResolutionConfigurationExtension` | `compiler/fir/resolve/src/.../extensions/FirScriptResolutionConfigurationExtension.kt` | EP. Loads compilation config during resolve. |
| `FirScriptResolutionConfigurationExtensionImpl` | `plugins/scripting/scripting-compiler/src/.../services/FirScriptResolutionConfigurationExtensionImpl.kt` | Pulls from `ScriptConfigurationsProvider`. |
| `FirScriptDeclarationsScope` | `compiler/fir/providers/src/.../scopes/impl/FirScriptDeclarationsScope.kt` | Scope over script-level declarations. |
| `TowerElementsForScript` | `compiler/fir/semantics/src/.../declarations/ScriptScopes.kt` | Tower for resolution. |
| `Fir2IrScriptConfiguratorExtension` | `compiler/fir/fir2ir/src/.../backend/Fir2IrScriptConfiguratorExtension.kt` | EP. Customizes FIR→IR for scripts. |
| `Fir2IrScriptConfiguratorExtensionImpl` | `plugins/scripting/scripting-compiler/src/.../services/Fir2IrScriptConfiguratorExtensionImpl.kt` | Converts FIR script to IR script. |

## 4. K2 / FIR — REPL snippet

**Different shape from `FirScript`:** snippet wraps statements in a `FirRegularClass` + `$$eval` function. This is the source of the bindings-port difficulty.

| Element | File | Notes |
|---|---|---|
| `FirReplSnippet` | `compiler/fir/tree/gen/.../declarations/FirReplSnippet.kt` (generated) | `name`, `receivers`, `snippetClass: FirRegularClass`, `evalFunctionSymbol`. |
| `FirReplSnippetImpl` | generated, same dir | |
| `FirReplSnippetBuilder` | generated | |
| `FirReplSnippetSymbol` | generated | Wraps `FirRegularClassSymbol`. |
| `FirReplSnippetConfiguratorExtension` | `compiler/fir/raw-fir/raw-fir.common/src/.../builder/FirReplSnippetConfiguratorExtension.kt` | EP. Methods include `configureEvalBody` and `MutableList<FirElement>.configure` for the eval body. |
| `FirReplSnippetConfiguratorExtensionImpl` | `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetConfiguratorExtensionImpl.kt` | Builds snippet class, eval fn signature, last-expression → result property. PSI-free since KT-83498: the result field name is `repl.resultFieldPrefix` + `configuration[repl.currentLineId].no` (line 173), read from the refined configuration pre-seeded by `K2ReplCompiler`; `resultField` is used when no line id is present (non-REPL / test-infra sessions). |
| `FirReplSnippetResolveExtension` | `compiler/fir/providers/src/.../extensions/FirReplSnippetResolveExtension.kt` | EP. `getSnippetDefaultImports`, `getSnippetScope`, `updateResolved`. |
| `FirReplSnippetResolveExtensionImpl` | `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetResolveExtensionImpl.kt` | Owns history provider + scope. |
| `FirReplHistoryProvider` | scripting-compiler-impl | Tracks snippet symbols in order. |
| `FirReplHistoryScope` | `plugins/scripting/scripting-compiler-impl/src/.../resolve/FirReplHistoryScope.kt` | Reverse-order access to prior snippet declarations. |
| `FirReplDeclarationReference` / `FirReplExpressionReference` / `FirReplPropertyDelegate` / `FirReplPropertyInitializer` | `compiler/fir/tree/gen/.../expressions/` | Expression types for cross-snippet references. |
| `Fir2IrReplSnippetConfiguratorExtension` | `compiler/fir/fir2ir/src/.../backend/Fir2IrReplSnippetConfiguratorExtension.kt` | EP. |
| `Fir2IrReplSnippetConfiguratorExtensionImpl` | `plugins/scripting/scripting-compiler/src/.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt` | Converts snippet class + eval fn + state object to IR. |

## 5. IR

| Element | File | Notes |
|---|---|---|
| `IrScript` | `compiler/ir/ir.tree/gen/.../declarations/IrScript.kt` | Used by both K1 and K2. **K1-only fields**: `providedProperties`, `providedPropertiesParameters`. **Shared**: `explicitCallParameters`, `implicitReceiversParameters`, `resultProperty`, `earlierScriptsParameter`, `thisReceiver`, `baseClass`, `targetClass`. |
| `IrScriptImpl` | gen, same dir | |
| `IrReplSnippet` | `compiler/ir/ir.tree/gen/.../declarations/IrReplSnippet.kt` | K2 only. `stateObject`, `evalFunction`, `targetClass`. |
| `IrReplSnippetImpl` | gen, same dir | |

## 6. IR lowering

| Element | File | Constants / notes |
|---|---|---|
| `ScriptsToClassesLowering` | `plugins/scripting/scripting-compiler/src/.../irLowerings/ScriptLowering.kt` | Topological sort by import deps; `prepareScriptClass()` + `finalizeScriptClass()`. Used by K1 + K2. |
| `ReplSnippetsToClassesLowering` | `plugins/scripting/scripting-compiler/src/.../irLowerings/ReplSnippetLowering.kt` | `REPL_SNIPPET_EVAL_FUN_NAME = "$$eval"`, `REPL_SNIPPET_RESULT_PROP_NAME = "$$result"`. K2 only. |

## 7. REPL orchestrators

| Class | File | Frontend / parsing |
|---|---|---|
| `K2ReplCompiler` (+ `K2ReplCompilationState`) | `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` | K2. **Parser-agnostic** (KT-83498): constructor takes `convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile = SourceCode::convertToFirViaLightTree` (lines 71-73, same seam as `ScriptJvmK2CompilerImpl`); every `SourceCode` (snippet + imports) goes through it (line 412). No `KtFile`/`KtScript`/`KtFileScriptSource` handling; snippet class FQ name comes from the produced `FirReplSnippet.snippetClass`. The per-snippet refined configuration (with `repl.currentLineId`) is pre-seeded into `scriptRefinedCompilationConfigurationsCache` (lines 341-345) so FIR configurators see it. Incomplete-code detection (`ScriptDiagnostic.incompleteCode`) = all errors are `SYNTAX` at the end of the snippet text (`isIncompleteSnippet`). |
| `GenericReplCompiler` | `plugins/scripting/scripting-compiler/src/.../repl/GenericReplCompiler.kt` | K1 (legacy) |

## 8. Compilation orchestrators (non-REPL)

`plugins/scripting/scripting-compiler/src/.../impl/ScriptJvmK2CompilerImpl.kt`:

| Class / fn | Line | Role |
|---|---|---|
| `ScriptJvmK2CompilerImpl` | 109 | **Parser-agnostic core.** Ctor takes `convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile`. Drives refine → annotation-resolve → session creation → FIR build (via the injected converter) → `resolveAndCheckFir` → `convertAnalyzedFirToIr` → `generateCodeFromIr` → `makeCompiledScript`. |
| `ScriptJvmK2CompilerIsolated` | 60 | Thin wrapper. Calls `withK2ScriptCompilerWithLightTree`. Used by `JvmScriptCompiler` (libraries/scripting/jvm-host) for embedded/host scenarios. |
| `ScriptJvmK2CompilerFromEnvironment` | 80 | Thin wrapper around a pre-built `KotlinCoreEnvironment`. Used from CLI via `JvmCliScriptEvaluationExtension`. |
| `withK2ScriptCompilerWithLightTree<T>` | 295 | Helper: builds isolated state + `ScriptJvmK2CompilerImpl` with `convertToFirViaLightTree`. |
| `convertToFirViaLightTree` (top-level) | 317 | Default converter. Wraps `LightTree2Fir`. |
| `ScriptJvmCompilerIsolated` (K1 legacy) | — | Separate file. K1 path; goes with K1 retirement. |

**No PSI converter is wired today** — every production caller uses LT. The seam exists so a PSI variant could be added if needed; not in scope.

## Cheat sheet: script vs snippet at each layer

| Layer | Script | Snippet |
|---|---|---|
| Source repr (K1) | `KtScript` (PSI) | `KtScript` (PSI, marked as snippet) |
| Source repr (K2) | `SourceCode` → LightTree | `SourceCode` → LightTree (via injectable `convertToFir`; PSI only if a PSI converter is injected) |
| FIR | `FirScript` (statements + params + receivers) | `FirReplSnippet` (embedded `FirRegularClass` + `$$eval`) |
| FIR scope | `FirScriptDeclarationsScope` | `FirReplHistoryScope` |
| IR | `IrScript` | `IrReplSnippet` |
| Lowered to | `IrClass` | `IrClass` with `$$eval(...)` |
