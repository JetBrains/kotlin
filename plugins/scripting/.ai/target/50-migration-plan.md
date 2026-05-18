# Target — Migration Plan

> **When to consult**: picking a step or checking sequencing constraints. Steps 1–14 are referenceable IDs. Canonical home for KT-83498 design notes (step 2).
> **Cache lifetime**: mutable-per-iteration (step strike-throughs accumulate)
> **Last verified**: 2026-05-18 (step 1b root-cause refined to G11 umbrella)

Ordered, each step independently mergeable. Each step is a small set of commits, not a single mega-MR.

## Sequence

### 1. K2 JSR-223 bindings — via refinement-DSL "synthetic snippets" callback

> **Partial — 2026-05-17.** Synthetic-snippets API + K2 REPL plumbing landed. 11/21 `KotlinJsr223ScriptEngineIT` passing (was 3/21). See [iteration entry](../iterations/2026-05-17_bindings-partial.md). Remaining: custom-`ScriptContext` threading, eval-in-eval, identifier escaping, and pre-existing K2 codegen bugs (`@InlineOnly` / fake-override) — last group out of scope for this step.

**Goal**: close the feature gap from commit `04ecbd1f8a7f` ("jsr223: k2 impl without bindings support (yet)"). Recommended approach: **Option D** — add a refinement-DSL callback (`prependSyntheticSnippets`) returning synthetic snippets to compile + eval before the user's snippet; binding-diff logic lives in a definition-side configurator. See [40-jsr223-target.md](40-jsr223-target.md) Option D.

**Touch**:
- `libraries/scripting/common/src/.../api/replData.kt` — `prependSyntheticSnippets` API (landed `669ece00`).
- `libraries/scripting/common/src/.../impl/compilationInternals.kt` — `_isSyntheticSnippet` non-API key (landed `669ece00`).
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — invokes the handler; compiles synthetic + user snippets together; eager classpath extraction for `JvmDependencyFromClassLoader` (landed `54cd2163`).
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplEvaluator.kt` — walks the `LinkedSnippet` chain back to last-evaluated and evals pending nodes in order; lenient result-field lookup (landed `54cd2163` / `534bb354`).
- `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` — synthetic-snippet generator (landed `669ece00`); generated setter uses `bindings.put(...)` to avoid the `@InlineOnly` `MutableMap.set` codegen stub.
- No changes needed in `FirReplSnippetConfiguratorExtensionImpl` or `Fir2IrReplSnippetConfiguratorExtensionImpl` — synthetic snippets are just snippets.

**Done when**: all binding tests in `KotlinJsr223ScriptEngineIT` pass on K2 path **except those blocked by Q13 (`BLOCKED-CODEGEN`) or Q14/Q15/Q16 (`BLOCKED-DESIGN`)**; cross-snippet binding tests (bind → use → rebind → use) pass; synthetic snippets visible to subsequent user snippets via the normal history scope. Per-test `BLOCKED-BY` matrix lives in [`../current/70-tests.md`](../current/70-tests.md#jsr-223-per-test-status-blocked-by-matrix); tests in `BLOCKED-CODEGEN` are `@Disabled("blocked by Q13")` and cease to count against step 1's acceptance.

### 1b. Fix K2 REPL `IR_EXTERNAL_DECLARATION_STUB`

**Goal**: K2 REPL pipeline must produce IR for which `ExpressionCodegen.visitCall` does NOT fail the `require(callee.parent is IrClass)` check at `compiler/ir/backend.jvm/codegen/src/.../ExpressionCodegen.kt:519` when a snippet's `$$eval` body invokes an external Kotlin top-level decl. After this step, all 6 `BLOCKED-CODEGEN` tests in [`../current/70-tests.md`](../current/70-tests.md#jsr-223-per-test-status-blocked-by-matrix) (`testResolveFromContextStandard`, `testResolveFromContextLambda`, `testResolveFromContextDirectExperimental`, `testMultipleCompilable`, plus `testEvalWithContext{,Direct}`) can be un-disabled.

**Design home**: see [`90-open-questions.md`](90-open-questions.md#q13-k2-repl-ir_external_declaration_stub-on-external-kotlin-top-level-decls-umbrella-was-inlineonly--fake_override) Q13 (umbrella + Q13a `@InlineOnly` body materialisation / Q13b fake-override resolution as historical sub-issues). Gotchas catalog: [`../current/80-known-gotchas.md`](../current/80-known-gotchas.md#g11-ir_external_declaration_stub-on-any-external-kotlin-top-level-decl-referenced-from-a-snippet-umbrella-over-g1g2) G11 (umbrella) plus G1 / G2 as special cases.

**Investigation status**: two iterations of investigation completed —
- `2026-05-18_codegen-stub-investigation` (round 1): split the failure into G1 (`@InlineOnly` body-materialisation in Fir2Ir / inliner) and G2 (cross-snippet fake-override on rehydrated `ReplState`). Produced fix-plan sketches for both.
- `2026-05-18_step1b-rootcause-refinement` (round 2 — this iteration): captured the actual codegen stack for `testResolveFromContextStandard` and **revised** the root cause. The proximate codegen exception fires on a *plain external Kotlin top-level `val`* (`<get-shouldBeVisibleFromRepl>`) — neither `[inline]` nor `[fake_override]`. G1 / G2 are special cases of a broader G11 umbrella: K2 REPL parents external Kotlin top-level decls on `IrExternalPackageFragment` instead of the equivalent JVM file-class facade, so codegen's `require(callee.parent is IrClass)` fails.

**Touch (anticipated, to be confirmed in the next iteration)**:
- `compiler/fir/fir2ir/src/.../Fir2IrDeclarationStorage.kt:1390-1427` — the `allowNonCachedDeclarations` branch already creates a non-cached file-class facade (`isNonCachedSourceFileFacade = true`); extending the predicate to cover the REPL case (or wiring an equivalent REPL-facade build path) may be the smallest fix. Validate that the resulting facade IR class satisfies `ExpressionCodegen.visitCall`'s require and doesn't break source-classpath compilation.
- Alternatively `compiler/ir/backend.jvm/lower/src/.../FileClassLowering.kt` (or a sibling phase) — extend to rewrite `IrExternalPackageFragment` parents on referenced external Kotlin top-level decls. Higher cost (touches generic JVM lowering pipeline; needs careful scoping to REPL).
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — if the fix requires a config flag passed through `Fir2IrConfiguration` (e.g. setting `allowNonCachedDeclarations = true` for the REPL path or introducing a `allowExternalDeclFileClassFacade` flag).
- After the umbrella fix lands, re-evaluate Q13a / Q13b: the `@InlineOnly` body-materialisation gap may *still* exist as a secondary issue (inliner can't lower without body) but won't crash codegen. Cross-snippet fake-override (Q13b) needs a fresh single-test repro before any production change.

**Repro**: cheapest is `scriptEngine.eval("kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl * 6")` from `KotlinJsr223ScriptEngineIT.testResolveFromContextStandard` (currently `@Disabled`). A non-JSR-223 direct `K2ReplCompiler` repro would also serve and would isolate the REPL pipeline from the bindings synthetic-snippet machinery — desirable as a regression fixture.

**Done when**: all 6 `BLOCKED-CODEGEN` tests in `KotlinJsr223ScriptEngineIT` pass on un-disable + run green; a regression fixture lives under `compiler/testData/codegen/script/` (or a new REPL-codegen fixture set) covering: plain external Kotlin top-level `val`/`fun`, `@InlineOnly` stdlib operator (`?.let`, `bindings[k] = v`, `joinToString$default`), and a cross-snippet inherited fake-override call.

**Sequencing**: independent of step 1 acceptance (step 1 ships with `@Disabled` carve-out); blocks the strike-through of step 1's `BLOCKED-CODEGEN` carve-out and removes the disables once landed.

### 2. Land KT-83498 — full LightTree path for `K2ReplCompiler`

**Goal**: drop the PSI branch from snippet parsing; align `K2ReplCompiler` with `ScriptJvmK2CompilerImpl`'s parser-agnostic seam.

**Touch** (line anchors authoritative in [`../current/10-compiler-representation.md`](../current/10-compiler-representation.md)):
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — replace the `partition { it is KtFileScriptSource }` split with `session.buildFirViaLightTree` over all sources. Either accept all `SourceCode` via LT or accept a `convertToFir` lambda (mirroring `ScriptJvmK2CompilerImpl`).
- `compiler/fir/raw-fir/light-tree2fir/src/.../LightTreeRawFirDeclarationBuilder.kt` — if a REPL-snippet marker analogous to `markAsReplSnippet()` is needed in the LT path, add it.
- `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetConfiguratorExtensionImpl.kt` — drop the residual `scriptSource.psi as? KtScript` touch; rely on `KtSourceElement` abstractions.

**Done when**: snippets compile through LT regardless of `SourceCode` subtype; `KtFileScriptSource` no longer special-cased.

**Design notes** (absorbed from former Q2):

- Priority: blocks the "fully PSI-free K2 scripting" claim and unblocks PSI-free JSR-223 embedding.
- Owner / scope: estimate medium. Touches `K2ReplCompiler`, LT builder (if a `markAsReplSnippet`-equivalent is needed), and the residual PSI touch in `FirReplSnippetConfiguratorExtensionImpl`.
- Shape decision: align with `ScriptJvmK2CompilerImpl` (`convertToFir` lambda) for symmetry, or keep `K2ReplCompiler` simpler with hardwired LT? Recommend lambda for parser-agnostic seam reuse.

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

- Steps 1, 1b, 2, 3 are independent of each other; step 1b removes the `@Disabled` markers introduced by step 1 once landed.
- Steps 4, 5 are independent of each other.
- Step 6 requires 4, 5, and (7 done OR jvm-host legacy still building, then delete after).
- Step 8 requires no remaining K1 REPL callers (i.e. after 5 + 7).
- Step 11 gates on whole-compiler K1 retirement — not scripting's call.
- Step 12 (test cleanup) follows steps 4, 5, 6 (REPL removal cascade) and step 11 (K1 retirement) for the K1 PSI parts.
- Steps 13, 14 are independent of the K1 cleanup chain.

## Tracking

Each step → YouTrack issue with `KT-XXXXX`. Commit messages reference issues (`^KT-XXXXX Fixed`). Tests committed with code per [`code_authoring_and_core_review.md`](../../../../docs/code_authoring_and_core_review.md).
