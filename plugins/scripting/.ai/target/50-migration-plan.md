# Target — Migration Plan

Ordered, each step independently mergeable. Each step is a small set of commits, not a single mega-MR.

## Sequence

### 1. K2 JSR-223 bindings — via refinement-DSL "implicit snippets" callback

**Goal**: close the feature gap from commit `04ecbd1f8a7f` ("jsr223: k2 impl without bindings support (yet)"). Recommended approach: **Option D** — add a refinement-DSL callback returning implicit snippets to compile + eval before the user's snippet; binding-diff logic lives in a definition-side configurator. See [40-jsr223-target.md](40-jsr223-target.md) Option D.

**Touch**:
- `libraries/scripting/common/api/scriptCompilation.kt` — new public config key + DSL helper (`inferImplicitSnippetsBefore` or similar).
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — harness invokes the handler chain; recursively compiles implicit snippets; depth/cycle guard.
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplEvaluator.kt` — eval implicit snippets in order before user snippet; hide them from `Invocable` enumeration.
- `libraries/scripting/jvm-host/.../KotlinJsr223ScriptEngineImpl.kt` — install `Jsr223BindingsConfigurator` into the engine's `ScriptCompilationConfiguration`.
- New file `libraries/scripting/jvm-host/.../Jsr223BindingsConfigurator.kt` (or similar location) — bindings diff + delegating-property snippet generation + bootstrap of canonical `bindings` accessor.
- No changes needed in `FirReplSnippetConfiguratorExtensionImpl` or `Fir2IrReplSnippetConfiguratorExtensionImpl` — implicit snippets are just snippets.

**Done when**: binding tests in `KotlinJsr223ScriptEngineIT` pass on K2 path; cross-snippet binding tests (bind → use → rebind → use) pass; implicit snippets visible to subsequent user snippets via the normal history scope.

### 2. Land KT-83498 — full LightTree path for `K2ReplCompiler`

**Goal**: drop the PSI branch from snippet parsing; align `K2ReplCompiler` with `ScriptJvmK2CompilerImpl`'s parser-agnostic seam.

**Touch**:
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — replace the `partition { it is KtFileScriptSource }` split (lines 351-359) with `session.buildFirViaLightTree` over all sources. Either accept all `SourceCode` via LT or accept a `convertToFir` lambda (mirroring `ScriptJvmK2CompilerImpl`).
- `compiler/fir/raw-fir/light-tree2fir/src/.../LightTreeRawFirDeclarationBuilder.kt` — if a REPL-snippet marker analogous to `markAsReplSnippet()` is needed in the LT path, add it.
- `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetConfiguratorExtensionImpl.kt:173` — drop `scriptSource.psi as? KtScript`; rely on `KtSourceElement` abstractions.

**Done when**: snippets compile through LT regardless of `SourceCode` subtype; `KtFileScriptSource` no longer special-cased.

### 3. Design + prototype stateless remote REPL compilation

**Goal**: move REPL compilation state out of the compiler. Snippet output = class files + sidecar metadata; subsequent calls deserialise and continue.

**Why**: at least one IntelliJ consumer relies on remote (out-of-process) JSR-223 compilation today. Current daemon path dies with K1. Stateless design unblocks both a new transport and (eventually) in-process hosting once IntelliJ-platform-dep cleanup completes.

**Touch (prototype)**:
- Storage-backed `FirReplHistoryProvider` impl (likely in `plugins/scripting/scripting-compiler-impl`).
- Sidecar metadata writer/reader (snippet name, index, default imports, result-prop ref, snippet-symbol shape). Decide format (JSON / proto / binary).
- `K2ReplCompiler` per-call entry that takes `(prevArtifacts, sourceCode)` and returns a new artifact bundle; no cross-call session state.
- Validation prototype: confirm `FirReplSnippetSymbol` + `FirReplSnippetResolveExtension.getSnippetScope` reconstruct correctly from class metadata + sidecar.

**Done when**: prototype compiles a multi-snippet sequence stateless-end-to-end; output cross-references resolve; sidecar format frozen.

**Out of scope for the prototype**: BTA transport, in-process embedding, IntelliJ consumer migration — those follow once the core proves out. See [40-jsr223-target.md](40-jsr223-target.md).

### 4. Delete daemon REPL

**Touch**:
- `compiler/daemon/daemon-common/src/.../CompileService.kt` — drop REPL methods + `ReplStateFacade`
- `compiler/daemon/src/.../KotlinRemoteReplService.kt` — delete
- `compiler/daemon/daemon-client/src/main/kotlin/.../KotlinRemoteReplCompilerClient.kt` + `RemoteReplCompilerState.kt` — delete
- Update daemon protocol version

**Coupling**: stops the only caller of cli-base/repl/* from the daemon side.

### 5. Delete `-Xrepl` CLI flag and shell

**Touch**:
- `compiler/arguments/.../CommonCompilerArguments.kt` — remove `-Xrepl`
- `compiler/cli/.../AbstractConfigurationPhase.kt` — remove `replMode` plumbing
- `plugins/scripting/scripting-compiler/.../pluginRegisrar.kt` — remove `JvmCliReplShellExtension` registration
- Delete `JvmCliReplShellExtension`, `JvmStandardReplFactoryExtension`, `ReplFactoryExtension` EP

### 6. Delete `cli-base/cli/common/repl/*`

After steps 4 + 5 + step 7 prerequisite (no jvm-host caller left). Delete `compiler/cli/cli-base/src/.../cli/common/repl/` directory.

### 7. Delete jvm-host legacy REPL wrappers

**Touch**:
- `libraries/scripting/jvm-host/legacyReplCompilation.kt` — delete
- `libraries/scripting/jvm-host/legacyReplEvaluation.kt` — delete
- `libraries/scripting/jvm-host/obsoleteJvmScriptEvaluation.kt` — delete (some entries already `DeprecationLevel.ERROR`)

**Prereq**: external users updated to K2 engine. If public Kotlin API stability matters, do a deprecation cycle first.

### 8. Delete K1 `GenericReplCompiler` + Scripting K1 registrar

**Touch**:
- `plugins/scripting/scripting-compiler/src/.../repl/GenericReplCompiler.kt` — delete
- `plugins/scripting/scripting-compiler/.../pluginRegisrar.kt` — remove `ScriptingCompilerConfigurationComponentRegistrar` entirely
- `JvmScriptCompiler.createLegacy()` — delete

### 9. Delete `scripting-ide-services` + companions

**Touch**:
- `plugins/scripting/scripting-ide-services/` — delete
- `plugins/scripting/scripting-ide-services-embeddable/` — delete
- `plugins/scripting/scripting-ide-services-test/` — delete
- Remove from settings.gradle.kts + parent module deps

**Coupling**: confirm no in-tree consumer (search: `KJvmReplCompiler`, `IdeLikeReplCodeAnalyzer`).

### 10. Delete `scripting-ide-common`

**Touch**:
- `plugins/scripting/scripting-ide-common/` — delete
- Remove from settings.gradle.kts + parent module deps

**Note**: Future reimplementation possible in a different form, definitely without K1. Not in scope here.

### 11. Delete K1 frontend bindings

Once K1 frontend is removed compiler-wide:

**Touch**:
- `plugins/scripting/scripting-compiler-impl/src/.../resolve/LazyScriptDescriptor.kt` — delete
- `plugins/scripting/scripting-compiler-impl/src/.../resolve/LazyScriptClassMemberScope.kt` — delete
- `plugins/scripting/scripting-compiler-impl/src/.../resolve/ScriptProvidedPropertyDescriptor.kt` — delete
- `plugins/scripting/scripting-compiler-impl/src/.../resolve/ReplResultPropertyDescriptor.kt` — delete
- K1 extensions in scripting-compiler (`ScriptingResolveExtension`, `ScriptExtraImportsProviderExtension`, etc.) — delete
- `IrScript` schema: regenerate without `providedProperties`, `providedPropertiesParameters`
- `BasicJvmScriptEvaluator` — remove `isCompiledWithK2` branches

### 12. Compiler-side scripting test cleanup

**Goal**: drop K1 test data; relocate K2 custom-script tests next to the scripting plugin.

**Touch**:
- Delete `compiler/tests-integration/testData/repl/` (~30 fixture dirs).
- Delete `compiler/tests-integration/tests/.../cli/jvm/repl/GenericReplTest.kt`.
- Delete `compiler/tests-integration/tests/.../codegen/ScriptGenTest.kt` (K1).
- Delete K1 PSI script parse cases in `compiler/psi/psi-impl/tests/.../psi/CustomPsiTest.kt` + fixtures under `compiler/psi/psi-impl/testData/psi/script/` and `testData/psi/repl/`.
- Move `compiler/tests-integration/tests/.../codegen/FirLightTreeCustomScriptCodegenTest.kt` and `FirPsiCustomScriptCodegenTest.kt` → `plugins/scripting/scripting-tests` (alongside corresponding fixture moves from `compiler/testData/codegen/scriptCustom/` after audit).
- Audit `compiler/testData/codegen/scriptCustom/` per file: K1-only → delete; K2 → move with the tests.
- Regenerate test runners (`./gradlew generateTests`).
- Audit `compiler/daemon/daemon-tests/test/` for REPL-specific cases; drop those when daemon REPL goes (step 4).

**Order**: After steps 4 (daemon REPL), 5 (`-Xrepl`), 6 (`cli-base/repl/*`).

### 13. Decide classpath discovery (KT-82551)

Un-deprecate + document, or design successor SPI.

### 14. (Post-cleanup) Drop residual PSI for scripts

Once K1 frontend is gone and snippet LT path is complete (step 2), audit any remaining `KtScript` usage in `plugins/scripting/` — likely only stub-indexing remnants. Decide on full PSI removal for scripts.

## Sequencing constraints

- Steps 1, 2, 3 are independent of each other.
- Steps 4, 5 are independent of each other.
- Step 6 requires 4, 5, and (7 done OR jvm-host legacy still building, then delete after).
- Step 8 requires no remaining K1 REPL callers (i.e. after 5 + 7).
- Step 11 gates on whole-compiler K1 retirement — not scripting's call.
- Step 12 (test cleanup) follows steps 4, 5, 6 (REPL removal cascade) and step 11 (K1 retirement) for the K1 PSI parts.
- Steps 13, 14 are independent of the K1 cleanup chain.

## Tracking

Each step → YouTrack issue with `KT-XXXXX`. Commit messages reference issues (`^KT-XXXXX Fixed`). Tests committed with code per [`code_authoring_and_core_review.md`](../../../docs/code_authoring_and_core_review.md).
