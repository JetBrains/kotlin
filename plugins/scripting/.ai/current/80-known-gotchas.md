# Current — Known Gotchas (K2 REPL / JSR-223)

> **When to consult**: before any edit that emits Kotlin source into a REPL snippet (synthetic-snippet generators, JSR-223 binding code), before chasing a `IR_EXTERNAL_DECLARATION_STUB` failure, before reasoning about cross-snippet symbol resolution or classloader-only dependencies. Cache load position: **2** (stable prefix, immediately after the file-load order block in [`../AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md)).
> **Cache lifetime**: stable (escalate items here from iteration `Key Learnings` only when they are demonstrably stable across at least two iterations)
> **Last verified**: 2026-05-18

Hard-won facts about the K2 REPL pipeline and the JSR-223 bridge that are easy to rediscover the slow way. Each entry has the shape `symptom → cause → workaround → reference`.

## G1. `IR_EXTERNAL_DECLARATION_STUB` on `@InlineOnly` members in snippet `$$eval`

- **Symptom**: `Unhandled intrinsic in ExpressionCodegen: FUN IR_EXTERNAL_DECLARATION_STUB ... [inline]`. Triggers most often on `kotlin.collections.MutableMap.set` (`bindings[k] = v`), `kotlin.let`, `kotlin.also`, `kotlin.apply`, `kotlin.takeIf`, `joinToString$default`, and other `@InlineOnly` / default-arg-bearing stdlib operators.
- **Cause**: K2 REPL pipeline currently deserialises `@InlineOnly` stdlib declarations as `IR_EXTERNAL_DECLARATION_STUB` instead of attaching the inlinable body. The inliner phase never fires for them in the REPL build, so `ExpressionCodegen` is asked to emit a call to a stub it doesn't know how to lower. Pre-existing K2 REPL inliner gap. See `Q13` in [`../target/90-open-questions.md`](../target/90-open-questions.md) and migration plan step **1b** in [`../target/50-migration-plan.md`](../target/50-migration-plan.md#1b-fix-k2-repl-ir_external_declaration_stub).
- **Workaround for generators** (until the codegen fix lands): emit non-inline call shapes from synthetic-snippet code. Use `bindings.put(k, v)` instead of `bindings[k] = v`; explicit `if (x != null) ...` instead of `x?.let { ... }`; explicit loops instead of `joinToString { ... }`.
- **Reference**: [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md#key-learnings) (initial discovery, `MutableMap.set` workaround), [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#key-learnings) (`?.let`/`?.also`/`?.apply`/`?.takeIf` confirmation).

## G2. `[fake_override]` on cross-module REPL symbols

- **Symptom**: same `IR_EXTERNAL_DECLARATION_STUB` failure, but on members like `ReplState.put`, `ReplState.get`, sometimes `kotlin.Result.value`. The IR call target is marked `[fake_override]` (not `[inline]`).
- **Cause**: K2 REPL fake-override resolver does not always materialise the override chain when the receiver class crosses an in-memory FIR-module boundary (`ReplState extends HashMap` lives on the synthetic-snippet class; the user snippet calls it through the inherited `MutableMap` signature). Same family as G1 — they share the codegen-side failure point but the IR producer is different. See Q13.
- **Workaround**: avoid generating user-visible APIs that funnel through `ReplState.put/get`. Bindings should be exposed via direct property accessors that call `bindings.put(...)` / `bindings.get(...)` on a `Bindings` (= `javax.script.Bindings = MutableMap<String, Any?>`) field, not via a `ReplState` indirection.
- **Reference**: [iterations/2026-05-18_jsr223-followup-1.md](../iterations/2026-05-18_jsr223-followup-1.md#remaining-9-failures-after-this-iteration).

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

## Promotion rule

A finding belongs here only after the same workaround / cause has appeared in ≥ 2 iterations or has been confirmed by code inspection beyond the discovery iteration. Iteration entries' `Key Learnings` sections should keep volatile / single-shot findings; promote to this doc with a back-link, then trim the iteration entry to a one-line "promoted to G_n_" pointer (do not delete from iterations — they are append-only).
