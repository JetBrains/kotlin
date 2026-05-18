# Current — Known Gotchas (K2 REPL / JSR-223)

> **When to consult**: before any edit that emits Kotlin source into a REPL snippet (synthetic-snippet generators, JSR-223 binding code), before chasing a `IR_EXTERNAL_DECLARATION_STUB` failure, before reasoning about cross-snippet symbol resolution or classloader-only dependencies. Cache load position: **2** (stable prefix, immediately after the file-load order block in [`../AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md)).
> **Cache lifetime**: stable (escalate items here from iteration `Key Learnings` only when they are demonstrably stable across at least two iterations)
> **Last verified**: 2026-05-18 (second iteration — added G11 umbrella, refined G1 / G2)

Hard-won facts about the K2 REPL pipeline and the JSR-223 bridge that are easy to rediscover the slow way. Each entry has the shape `symptom → cause → workaround → reference`.

## G1. `IR_EXTERNAL_DECLARATION_STUB` on `@InlineOnly` members in snippet `$$eval`

- **Symptom**: `Unhandled intrinsic in ExpressionCodegen: FUN IR_EXTERNAL_DECLARATION_STUB ... [inline]`. Triggers most often on `kotlin.collections.MutableMap.set` (`bindings[k] = v`), `kotlin.let`, `kotlin.also`, `kotlin.apply`, `kotlin.takeIf`, `joinToString$default`, and other `@InlineOnly` / default-arg-bearing stdlib operators.
- **Cause**: K2 REPL pipeline currently deserialises `@InlineOnly` stdlib declarations as `IR_EXTERNAL_DECLARATION_STUB` instead of attaching the inlinable body. The inliner phase never fires for them in the REPL build, so `ExpressionCodegen` is asked to emit a call to a stub it doesn't know how to lower. Pre-existing K2 REPL inliner gap. See `Q13` in [`../target/90-open-questions.md`](../target/90-open-questions.md) and migration plan step **1b** in [`../target/50-migration-plan.md`](../target/50-migration-plan.md#1b-fix-k2-repl-ir_external_declaration_stub).
- **Workaround for generators** (until the codegen fix lands): emit non-inline call shapes from synthetic-snippet code. Use `bindings.put(k, v)` instead of `bindings[k] = v`; explicit `if (x != null) ...` instead of `x?.let { ... }`; explicit loops instead of `joinToString { ... }`.
- **Refinement (2026-05-18, second iteration)**: G1 is a *special case* of the broader G11 (any external Kotlin top-level decl referenced from a snippet hits the same codegen `require(callee.parent is IrClass)` failure). `@InlineOnly` is the most user-visible trigger because stdlib operators are everywhere, but the underlying failure mode is parent-shape, not inliner-shape. The inliner-gap framing in the iteration `2026-05-18_codegen-stub-investigation` is still useful for *why the body isn't materialised*, but the proximate codegen failure is the parent-shape issue covered by G11.
- **Reference**: [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md#key-learnings) (initial discovery, `MutableMap.set` workaround), [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#key-learnings) (`?.let`/`?.also`/`?.apply`/`?.takeIf` confirmation), [iterations/2026-05-18_step1b-rootcause-refinement.md](../iterations/2026-05-18_step1b-rootcause-refinement.md) (G11 umbrella refinement).

## G2. `[fake_override]` on cross-module REPL symbols

- **Symptom**: `IR_EXTERNAL_DECLARATION_STUB` failure where the IR call target is marked `[fake_override]` — e.g. `ReplState.put`, `ReplState.get`, sometimes `kotlin.Result.value`.
- **Cause**: K2 REPL fake-override resolver does not always materialise the override chain when the receiver class crosses an in-memory FIR-module boundary (`ReplState extends HashMap` lives on the synthetic-snippet class; the user snippet calls it through the inherited `MutableMap` signature). See Q13.
- **Workaround**: avoid generating user-visible APIs that funnel through `ReplState.put/get`. Bindings should be exposed via direct property accessors that call `bindings.put(...)` / `bindings.get(...)` on a `Bindings` (= `javax.script.Bindings = MutableMap<String, Any?>`) field, not via a `ReplState` indirection.
- **Refinement (2026-05-18, second iteration)**: `testResolveFromContextStandard` was originally attributed to G2 because the IR render of the failing `$$eval` body *contains* a `ReplState.put [fake_override]` call (cross-snippet state record), but the actual codegen exception fires on a different call further down the body — `<get-shouldBeVisibleFromRepl>` — which is a plain external Kotlin top-level `val`, not a fake-override. The hypothesis that G2 specifically blocks this test is therefore not confirmed by the captured stack. G2 remains a real failure shape (the previous iteration's analysis of `Fir2IrReplSnippetConfiguratorExtensionImpl.getStateObject()` rehydration is still valid as a mechanism), but it is not the proximate cause for the four "context" tests in `KotlinJsr223ScriptEngineIT`. See G11 for the broader umbrella.
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#remaining-9-failures-after-this-iteration), [iterations/2026-05-18_step1b-rootcause-refinement.md](../iterations/2026-05-18_step1b-rootcause-refinement.md) (refinement / partial retraction).

## G3. `JvmDependencyFromClassLoader` is invisible to K2 FIR

- **Symptom**: types loaded only via the host classloader (test-`HashMap` subclasses, JSR-223 bindings types, implicit receivers from `getScriptContext`) resolve to nothing during FIR analysis — `UnresolvedSymbol` / `UNRESOLVED_REFERENCE`.
- **Cause**: K1 used `PackageFragmentFromClassLoaderProviderExtension` (K1-only legacy EP) to materialise classloader-only deps into descriptors. K2's FIR session does **not** see `JvmDependencyFromClassLoader` directly — it only consumes file-system jars/dirs in `dependencies`.
- **Workaround**: pre-extract a classpath with `scriptCompilationClasspathFromContext(classLoader, wholeClasspath = true, unpackJarCollections = true)` and feed it into `dependencies` as ordinary jars/dirs. Already wired in `K2ReplCompiler` (eager classpath extraction landed in commit `54cd2163`).
- **Reference**: [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md#key-learnings).

## G4. `LinkedSnippet` chain is producer-driven, consumer-walk

- **Symptom**: `NoClassDefFoundError` for classes the compiler clearly emitted (e.g. `ReplState` defined in synthetic snippet not visible at user snippet eval time); silent drops when the compiler appends N snippets but the evaluator only sees 1.
- **Cause**: K2 REPL compiler appends synthetic + user snippets onto the `LinkedSnippet` chain and returns the tail. Tail-only evaluation skips intermediate nodes.
- **Workaround**: `K2ReplEvaluator.eval()` walks the chain back from tail to `lastEvaluatedSnippet` and evaluates pending nodes in order. Don't bypass this walk if you add a new entry point.
- **Reference**: [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md#key-learnings).

## G5. Compiled-snippet `resultField` metadata can disagree with bytecode

- **Symptom**: `NoSuchFieldException: resN` in the evaluator after compilation succeeds.
- **Cause**: snippets that the compiler marks with `resultField("")` (e.g. synthetic snippets, statement-only forms) still ship metadata that the strict reflective lookup path picks up.
- **Workaround**: use lenient `runCatching { getDeclaredField(...) }` in the evaluator's result-field lookup. Already in place in `K2ReplEvaluator.evalSnippet()`.
- **Reference**: [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md#changes).

## G6. `getCurrentState(getContext())`, not `getCurrentState(context)` for `lineCounter`

- **Symptom**: snippet class-name collision (`snippet_0.repl.kts` produced twice); outer engine compiler's history conflicts with a custom-`ScriptContext`'s fresh state.
- **Cause**: when a caller passes a custom `ScriptContext` to `eval(String, Bindings)`, `getCurrentState(context)` produces a fresh state with `lineCounter = 0`, colliding with the outer compiler's history. The outer default-context state is the correct counter source — it co-ordinates with `replCompiler = getCurrentState(getContext()).compiler`.
- **Workaround**: always use `getCurrentState(getContext()).lineCounter++` on the engine's bookkeeping path. Custom-context bookkeeping flows through `lastScriptContext`, not through `getCurrentState`.
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#key-learnings).

## G7. `val bindings` must be per-eval, not per-session

- **Symptom**: stale bindings on the second eval that uses a different `ScriptContext` (custom `Bindings`); NullPointerException-on-access for entries that exist in the new context.
- **Cause**: declaring `val bindings = getBindings(ENGINE_SCOPE)` only in the first synthetic snippet stores a static field capturing that eval's `ScriptContext`. Subsequent evals with a different `ScriptContext` see the stale field.
- **Workaround**: declare `val bindings` in **every** synthetic snippet so each property getter references its own class's `bindings` field, set from the `ScriptContext` active at that eval's `$$eval` invocation. Declare top-level eval helpers (`fun eval(String)`, `fun eval(String, Bindings)`) only in the first synthetic snippet.
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#key-learnings).

## G8. JVM identifier constraints block `\`-based binding-name escaping

- **Symptom**: `testEvalWithContextNamesWithSymbols` fails — any `\`-escaped binding identifier (the K1 contract used `\,` for `.`, `\!` for `:`, etc.) is rejected by `FirJvmNamesChecker.INVALID_CHARS`.
- **Cause**: `FirJvmNamesChecker.INVALID_CHARS` includes `\` along with `.`, `:`, `;`, `[`, `]`, `/`, `<`, `>`. There is no JVM-safe escape using backslash. Underscore-only fallbacks hit "Names `_`, `__`, `___`, ... are reserved" parser rule.
- **Status**: design needed — see `Q14` in [`../target/90-open-questions.md`](../target/90-open-questions.md) (JVM-safe binding-name encoding).
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#key-learnings).

## G9. Lambda binding types have non-parseable `qualifiedName`

- **Symptom**: synthetic snippet emits e.g. `var myFun: Foo$$Lambda$1` and parser fails with "Property getter or setter expected" + cascading parse errors. Common under `-Xlambdas=indy`.
- **Cause**: indy-lambdas and other local/anonymous classes have a non-null `KClass.qualifiedName` on some JDKs (`Foo$$Lambda$1`, `MyKt$f$lambda$1`, names with `/` or `<`), but that string is not a valid Kotlin type reference.
- **Workaround**: filter such bindings in the generator with `isParseableKotlinQualifiedName(qn)` (dot-separated chain of valid Kotlin identifiers). Filtered bindings remain accessible via `bindings["..."]` from user code, just not as auto-generated typed properties. Landed in `propertiesFromContext.kt` (iteration 2026-05-18 follow-up).
- **Status**: see `Q15` in [`../target/90-open-questions.md`](../target/90-open-questions.md) for a typed-access design option (functional-interface synthesis vs erased `Any?` accessor).
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#remaining-9-failures-after-this-iteration) + this session's follow-up.

## G10. K2 JSR-223 implicit receiver is `ScriptContext`, not `ScriptTemplateWithBindings`

- **Symptom**: user-defined `fun ScriptTemplateWithBindings.foo(...)` is unreachable from a subsequent snippet: "Candidate ... is inapplicable because of a receiver type mismatch".
- **Cause**: K1 path exposed bindings via helpers receiving `ScriptTemplateWithBindings`. The K2 path uses `ScriptContext` as the implicit receiver of `$$eval`. Architectural divergence between the two paths.
- **Status**: design needed — see `Q16` in [`../target/90-open-questions.md`](../target/90-open-questions.md) (JSR-223 K2 implicit-receiver strategy).
- **Reference**: this session's investigation of `testEvalInEvalWithBindingsWithLambda`.

## G11. `IR_EXTERNAL_DECLARATION_STUB` on *any* external Kotlin top-level decl referenced from a snippet (umbrella over G1/G2)

- **Symptom**: `java.lang.IllegalArgumentException: Unhandled intrinsic in ExpressionCodegen: FUN IR_EXTERNAL_DECLARATION_STUB name:<X> ...` thrown from `org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen.visitCall` at line 519 during JVM bytecode generation for a snippet's `$$eval` body. `<X>` is any callable resolved against an external module: regular top-level `val`/`fun`, `@InlineOnly` stdlib operator, fake-override on a cross-module class — they all funnel into the same failure.
- **Proximate failure point**: `compiler/ir/backend.jvm/codegen/src/.../ExpressionCodegen.kt:519` — `require(callee.parent is IrClass) { "Unhandled intrinsic in ExpressionCodegen: ${callee.render()}" }`. The require fires whenever an `IrCall` survives all lowerings pointing at an `IrSimpleFunction` whose `parent` is *not* an `IrClass`. For external Kotlin top-level decls, the parent set by `Fir2IrDeclarationStorage.findIrParent` is `IrExternalPackageFragment` (a package, not a class), and no JVM-pipeline lowering rewrites it into the equivalent JVM file-class for the REPL build.
- **Cause (mechanism)**: Two contributing facts. (1) For source-form symbols (IDE / `allowNonCachedDeclarations` mode) `Fir2IrDeclarationStorage` at `Fir2IrDeclarationStorage.kt:1390-1427` *does* synthesise a non-cached file-class facade with `isNonCachedSourceFileFacade = true` and `parent = parentPackage`, satisfying the codegen require. (2) For class-file-deserialised external Kotlin top-level decls (the normal JSR-223 / REPL case — `kotlin-stdlib`, test classes, user classpath jars) that branch isn't taken and the standard JVM `FileClassLowering` pass operates on the *current* module's top-level decls only, not on referenced external decls. So the call survives with `IrExternalPackageFragment` parent.
- **Why normal `.kts` / `.kt` compilation works**: in non-REPL compilation, calls to external Kotlin top-level decls go through different IR shape (the callee is *imported* against a file-class derived from the deserialised `KotlinClassFinder` metadata), so `callee.parent` ends up being an `IrClass` (the file-class) by the time codegen runs. The REPL pipeline's `convertAnalyzedFirToIr` → `generateCodeFromIr` runs the same JVM lowering chain, but the lazy fir2ir builder behind the scenes parents the lazy callable on the package fragment rather than the JVM file-class facade. This is the divergence that needs a fix.
- **Failure manifests via three lenses** (these are the historic G1/G2 splits, now subsumed):
  - `[inline]` (G1) — `@InlineOnly` stdlib operators. The inliner-gap framing is correct as a *secondary* cause (the body fails to materialise so the inliner can't lower it away), but the *proximate* exception is the codegen require.
  - `[fake_override]` (G2) — fake-overrides inherited from external classes. Same proximate failure.
  - *plain regular* — no `[inline]` / `[fake_override]` annotation; just a regular external Kotlin `val`/`fun`. Example: `kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl` (a 7-int top-level `val`). This was historically misattributed to G2 because the IR render of the *enclosing* `$$eval` function contained a `ReplState.put [fake_override]` call adjacent to the failing call, but the captured `Caused by` chain points unambiguously at the plain external decl.
- **Workaround**: none for end users — synthetic-snippet generators can avoid `@InlineOnly` operators (G1 workarounds), but plain external decls (G11 in the strict sense) cannot be worked around at the source level; the test has to be `@Disabled` pending the codegen fix.
- **Fix direction (anticipated for step 1b execution)**: ensure the K2 REPL pipeline gives external Kotlin top-level decls a JVM file-class facade parent equivalent to what `FileClassLowering` produces in normal compilation. Candidate touch sites: `Fir2IrDeclarationStorage.findIrParent` (extend the file-class-facade synthesis branch to fire for the REPL case, not just `allowNonCachedDeclarations`); or extend the JVM lowering pipeline applied by `K2ReplCompiler` to include a phase that rewrites `IrExternalPackageFragment` parents on referenced external decls. Q13a (`@InlineOnly` body materialisation) and Q13b (cross-snippet fake-override) become *follow-up* sub-questions once the umbrella parent-shape fix lands, not pre-requisites.
- **Reference**: [iterations/2026-05-18_step1b-rootcause-refinement.md](../iterations/2026-05-18_step1b-rootcause-refinement.md) — root-cause captured via temporary `@Disabled` removal on `testResolveFromContextStandard`; failure stack saved in `plugins/scripting/.ai/tmp/junie/2026-05-18/g2_solo.txt` and `libraries/scripting/jsr223-test/build/test-results/test/TEST-*.xml` for that run.

## Promotion rule

A finding belongs here only after the same workaround / cause has appeared in ≥ 2 iterations or has been confirmed by code inspection beyond the discovery iteration. Iteration entries' `Key Learnings` sections should keep volatile / single-shot findings; promote to this doc with a back-link, then trim the iteration entry to a one-line "promoted to G_n_" pointer (do not delete from iterations — they are append-only).
