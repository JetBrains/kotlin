# Current — Known Gotchas (K2 REPL / JSR-223)

> **When to consult**: before any edit that emits Kotlin source into a REPL snippet (synthetic-snippet generators, JSR-223 binding code), before chasing a `IR_EXTERNAL_DECLARATION_STUB` failure, before reasoning about cross-snippet symbol resolution or classloader-only dependencies. Cache load position: **2** (stable prefix, immediately after the file-load order block in [`../AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md)).
> **Cache lifetime**: stable (escalate items here from iteration `Key Learnings` only when they are demonstrably stable across at least two iterations)
> **Last verified**: 2026-05-18 (third iteration — G11 fix landed in `ReplSnippetsToClassesLowering`; G1 / G2 / G11 all marked **FIXED** in the K2 REPL JVM path)

Hard-won facts about the K2 REPL pipeline and the JSR-223 bridge that are easy to rediscover the slow way. Each entry has the shape `symptom → cause → workaround → reference`.

## G1. `IR_EXTERNAL_DECLARATION_STUB` on `@InlineOnly` members in snippet `$$eval` — **FIXED 2026-05-18** (via G11 umbrella fix)

- **Symptom (historical)**: `Unhandled intrinsic in ExpressionCodegen: FUN IR_EXTERNAL_DECLARATION_STUB ... [inline]`. Triggers most often on `kotlin.collections.MutableMap.set` (`bindings[k] = v`), `kotlin.let`, `kotlin.also`, `kotlin.apply`, `kotlin.takeIf`, `joinToString$default`, and other `@InlineOnly` / default-arg-bearing stdlib operators.
- **Cause (historical)**: special case of G11 (parent-shape: `IrExternalPackageFragment` parent fails the codegen `require(callee.parent is IrClass)`). The earlier framing as an inliner-gap was a *secondary* concern (body not materialised) — the proximate exception is the parent shape.
- **Fix**: G11 fix in `ReplSnippetsToClassesLowering` (see G11 entry below) rewrites `IrExternalPackageFragment` parents to JVM file-class facades **before** codegen runs. `@InlineOnly` calls now lower normally for the REPL JVM path.
- **Generator workarounds remain valid as defensive coding**: `bindings.put(k, v)` over `bindings[k] = v` etc. — these never relied on the codegen fix, are slightly more conservative, and avoid re-introducing the issue if the EPPL-equivalent pass is ever removed or bypassed.
- **Reference**: [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md#key-learnings) (initial discovery, `MutableMap.set` workaround), [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#key-learnings) (`?.let`/`?.also`/`?.apply`/`?.takeIf` confirmation), [iterations/2026-05-18_step1b-rootcause-refinement.md](../iterations/2026-05-18_step1b-rootcause-refinement.md) (G11 umbrella refinement), [iterations/2026-05-18_step1b-fix-landed.md](../iterations/2026-05-18_step1b-fix-landed.md) (production fix landed).

## G2. `[fake_override]` on cross-module REPL symbols — **FIXED 2026-05-18** (via G11 umbrella fix)

- **Symptom (historical)**: `IR_EXTERNAL_DECLARATION_STUB` failure where the IR call target is marked `[fake_override]` — e.g. `ReplState.put`, `ReplState.get`, sometimes `kotlin.Result.value`.
- **Cause (historical)**: special case of G11 — the fake-override callable still has `IrExternalPackageFragment` as parent because the inherited member's source is the external (deserialised) declaring class's file-class. Same proximate codegen require failure as G1.
- **Fix**: G11 fix in `ReplSnippetsToClassesLowering` reparents the callee onto a synthesised JVM file-class facade. `[fake_override]` calls (including inherited `ReplState.put` / `ReplState.get` paths) now reach codegen with `callee.parent is IrClass`.
- **Generator workarounds remain valid as defensive coding**: prefer direct `bindings.put(...)` / `bindings.get(...)` over a `ReplState` indirection — same defensive rationale as G1.
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#remaining-9-failures-after-this-iteration), [iterations/2026-05-18_step1b-rootcause-refinement.md](../iterations/2026-05-18_step1b-rootcause-refinement.md) (refinement / partial retraction), [iterations/2026-05-18_step1b-fix-landed.md](../iterations/2026-05-18_step1b-fix-landed.md) (production fix landed).

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

## G11. `IR_EXTERNAL_DECLARATION_STUB` on *any* external Kotlin top-level decl referenced from a snippet (umbrella over G1/G2) — **FIXED 2026-05-18** (migration step 1b)

- **Symptom (historical)**: `java.lang.IllegalArgumentException: Unhandled intrinsic in ExpressionCodegen: FUN IR_EXTERNAL_DECLARATION_STUB name:<X> ...` thrown from `org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen.visitCall` at line 519 during JVM bytecode generation for a snippet's `$$eval` body. `<X>` is any callable resolved against an external module: regular top-level `val`/`fun`, `@InlineOnly` stdlib operator, fake-override on a cross-module class — they all funnel into the same failure.
- **Proximate failure point**: `compiler/ir/backend.jvm/codegen/src/.../ExpressionCodegen.kt:519` — `require(callee.parent is IrClass) { "Unhandled intrinsic in ExpressionCodegen: ${callee.render()}" }`. The require fires whenever an `IrCall` survives all lowerings pointing at an `IrSimpleFunction` whose `parent` is *not* an `IrClass`. For external Kotlin top-level decls, the parent set by `Fir2IrDeclarationStorage.findIrParent` is `IrExternalPackageFragment` (a package, not a class), and no JVM-pipeline lowering rewrites it into the equivalent JVM file-class for the REPL build.
- **Cause (mechanism)**: the K2 JVM `ExternalPackageParentPatcherLowering` is supposed to rewrite this exact parent shape for the *currently-lowered* file, but it never visits the snippet `targetClass` in the REPL pipeline (the snippet's class is structurally hidden by `IrReplSnippet` / `ReplSnippetsToClassesLowering` removal sequencing). Even if it did, the standard JVM `FileClassLowering` pass operates on the *current* module's top-level decls only, not on referenced external decls. For source-form symbols (IDE / `allowNonCachedDeclarations` mode) `Fir2IrDeclarationStorage` at `Fir2IrDeclarationStorage.kt:1390-1427` synthesises a non-cached file-class facade — but that branch never fires in the production REPL compile.
- **Fix landed 2026-05-18 (migration step 1b)**: added a REPL-scoped EPPL-equivalent post-pass — `ReplSnippetExternalPackageParentPatcher` inside `ReplSnippetsToClassesLowering` (`plugins/scripting/scripting-compiler/src/.../irLowerings/ReplSnippetLowering.kt`). It mirrors `org.jetbrains.kotlin.backend.jvm.lower.ExternalPackageParentPatcherLowering.Visitor.visitMemberAccess`: for every `IrMemberAccessExpression` whose callee implements `IrMemberWithContainerSource`, has a `FacadeClassSource` container, and is currently parented on `IrExternalPackageFragment`, it builds a JVM file-class facade via `createJvmFileFacadeClass` + `classNameOverride` and reparents the callee (and corresponding property, if any) on the facade. The pass runs eagerly on each snippet's `targetClass` after `finalizeReplSnippetClass`, so the snippet body's IR is rewritten before any later JVM lowering observes the package parent. Bytecode references the real `*Kt` (or multifile-facade) class on the classpath, matching what normal `.kt` compilation emits.
- **Verification**: JSR-223 suite went from 12 PASS / 6 SKIP / 3 FAIL (baseline) to 17 PASS / 1 SKIP / 3 FAIL. 5 of the 6 BLOCKED-CODEGEN tests now pass: `testResolveFromContextStandard` (plain external `val`), `testResolveFromContextLambda` (`@InlineOnly` `inlineCallLambda` + `callLambda`), `testResolveFromContextDirectExperimental` (classloader-backed plain external `val`), `testMultipleCompilable` (`joinToString$default`), `testEvalWithContext` (`bindings["nullable"]?.let { ... }`). The 6th (`testEvalWithContextDirect`) now fails for a different, non-codegen reason (synthetic-snippet null-binding type bug — see Q17). The 3 remaining failures are pre-existing Q14 / Q15 / Q16 design issues, unchanged.
- **Reference**: [iterations/2026-05-18_step1b-fix-landed.md](../iterations/2026-05-18_step1b-fix-landed.md) (this iteration — fix landed + verification), [iterations/2026-05-18_step1b-rootcause-refinement.md](../iterations/2026-05-18_step1b-rootcause-refinement.md) (root-cause refinement, second iteration), [iterations/2026-05-18_codegen-stub-investigation.md](../iterations/2026-05-18_codegen-stub-investigation.md) (initial investigation, first iteration).

## Promotion rule

A finding belongs here only after the same workaround / cause has appeared in ≥ 2 iterations or has been confirmed by code inspection beyond the discovery iteration. Iteration entries' `Key Learnings` sections should keep volatile / single-shot findings; promote to this doc with a back-link, then trim the iteration entry to a one-line "promoted to G_n_" pointer (do not delete from iterations — they are append-only).
