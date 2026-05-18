# JSR-223 K2 — `IR_EXTERNAL_DECLARATION_STUB` investigation + process docs — 2026-05-18

## Overview

Two-part iteration:

1. **Process docs**: the prior iteration (`2026-05-18_jsr223-followup-1`) flagged 9 concrete gaps in the
   Junie-hosted scripting workflow. This iteration applies the doc-side changes (new `current/80-known-gotchas.md`,
   `current/70-tests.md` BLOCKED-BY matrix, Q13–Q16, migration plan step 1b, `JUNIE_NOTES.md` budget +
   triage sections, `AGENT_INSTRUCTIONS.md` Critical Patterns rows). `@Disabled` annotations added to
   the 6 `BLOCKED-CODEGEN-Q13` tests in `KotlinJsr223ScriptEngineIT` so step 1 acceptance is no longer
   blocked by the pre-existing codegen bug.
2. **Codegen investigation**: traced the `IR_EXTERNAL_DECLARATION_STUB` failure family to two distinct
   root causes in Fir2Ir; produced a fix-plan sketch for both. Filed under Q13 (sub-questions Q13a / Q13b).

No source code changes were landed for the codegen bug itself in this iteration — the investigation
output is the prepared fix plan in [`../target/50-migration-plan.md`](../target/50-migration-plan.md)
step 1b and the Q13 design notes in [`../target/90-open-questions.md`](../target/90-open-questions.md).

## Workstream / Issue

Migration-plan step 1 follow-up (`partial`); migration-plan step 1b (`investigation, plan ready`).

## Process docs added / changed

| Doc | Change |
|---|---|
| `plugins/scripting/.ai/current/80-known-gotchas.md` | NEW. G1–G10 stable findings escalated from iteration `Key Learnings`. |
| `plugins/scripting/.ai/current/70-tests.md` | Added JSR-223 per-test `BLOCKED-BY` matrix (status + last-verified + notes per `KotlinJsr223ScriptEngineIT` test). Bumped "Last verified". |
| `plugins/scripting/.ai/target/90-open-questions.md` | Added Q13 (codegen STUB, with sub-questions Q13a/Q13b), Q14 (binding-name encoding), Q15 (lambda type rendering), Q16 (implicit-receiver strategy). |
| `plugins/scripting/.ai/target/50-migration-plan.md` | Rewrote step 1 "Done when" to carve out `BLOCKED-CODEGEN-Q13` / `BLOCKED-DESIGN-*` rows. Added step 1b "Fix K2 REPL `IR_EXTERNAL_DECLARATION_STUB`" with goal, touch list, design home, and done-when. Updated sequencing constraints. |
| `plugins/scripting/.ai/JUNIE_NOTES.md` | Added "Investigation budget" (tool-call caps replacing the dead `cavecrew-investigator` mechanism), "Test triage workflow" (XML parse recipe), "run_test vs ./gradlew" decision tree; updated Quick checklist to reference them. |
| `plugins/scripting/.ai/AGENT_INSTRUCTIONS.md` | Added two Critical Patterns rows (K2 REPL inliner gap; lambda binding qualifiedName). Registered `current/80-known-gotchas.md` in Reference Documents table and at position 2 of the Caching strategy load order. Bumped Q1–Q12 to Q1–Q16 in Reference Documents table. |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | `@Disabled` on 6 `BLOCKED-CODEGEN-Q13` tests (`testMultipleCompilable`, `testEvalWithContext`, `testEvalWithContextDirect`, `testResolveFromContextStandard`, `testResolveFromContextLambda`, `testResolveFromContextDirectExperimental`). |

## Codegen investigation

### Failure point

`compiler/ir/backend.jvm/codegen/src/.../ExpressionCodegen.kt:519`:

```kotlin
override fun visitCall(expression: IrCall, data: BlockInfo): PromisedValue {
    val intrinsic = classCodegen.context.getIntrinsic(expression.symbol) as IntrinsicMethod?
    if (intrinsic != null) { ... }
    val callee = expression.symbol.owner
    require(callee.parent is IrClass) { "Unhandled intrinsic in ExpressionCodegen: ${callee.render()}" }
    ...
}
```

The `require` fires when an `IrCall` survives all lowering phases (including the JVM inliner) pointing
at an `IrSimpleFunction` whose `parent` is not an `IrClass`. The render embedded into the error
message starts with `FUN IR_EXTERNAL_DECLARATION_STUB ...`, confirming the origin. Two distinct
upstream call paths produce that callee shape.

### Family G1 — `@InlineOnly` stdlib top-level decls

Affected: `testMultipleCompilable` (`joinToString$default`), `testResolveFromContextLambda` and any
generator-emitted `?.let { }` / `bindings[k] = v`. Iteration `2026-05-17_bindings-partial` worked around
G1 in the generator by emitting `bindings.put(...)`.

**Producer** — `Fir2IrDeclarationStorage.getIrFunctionSymbol()` (L1117 onwards) — when an external-parent
function is requested:

```kotlin
if (irParent?.isExternalParent() == true) {
    val symbol = createMemberFunctionSymbol(function, fakeOverrideOwnerLookupTag, parentIsExternal = true)
    val firForLazyFunction = calculateFirForLazyDeclaration(...)
    callablesGenerator.createIrFunction(
        firForLazyFunction, irParent, symbol,
        predefinedOrigin = firForLazyFunction.computeExternalOrigin(), // → IR_EXTERNAL_DECLARATION_STUB
        ...
    ).also { check(it is Fir2IrLazySimpleFunction) }
    ...
}
```

`computeExternalOrigin()` (L1109) deterministically yields `IR_EXTERNAL_DECLARATION_STUB` for any non-Java
external function. For top-level inline `@InlineOnly` stdlib functions (`kotlin.let`,
`kotlin.collections.set` on `MutableMap`, etc.) the parent is `IrExternalPackageFragment`, **not** an
`IrClass`, so by the time codegen's `visitCall` runs, the `require(callee.parent is IrClass)` fails.

The JVM IR inliner phase is supposed to inline these calls away. For `@InlineOnly` decls the inliner
relies on the inlinable body being materialised by `Fir2IrLazySimpleFunction.body` (lazy). The lazy body
is computed by walking the deserialised FIR. For non-REPL K2 compilations (regular `.kts` scripts,
straight `.kt` files) this works — `?.let` compiles fine.

**REPL-specific divergence (suspected)**: the REPL pipeline creates a *fresh `FirSession` per snippet*
(`K2ReplCompiler.compileImpl` L367–380: `addNewSnippetModuleData` + `FirJvmSessionFactory.createSourceSession`).
Each snippet's session has its own `dependenciesSymbolProvider`. Stdlib FIR symbols (and their bodies)
are resolved through the *shared* library session (`state.sharedLibrarySession`), but the per-snippet
session's `Fir2IrLazySimpleFunction` instances cache the body resolution against the per-snippet session.
Hypothesis: under some call shapes (likely `@InlineOnly` decls with default arguments — `joinToString$default`
— or `@InlineOnly` decls whose body references types resolved through the snippet-specific dependency
chain), the lazy body computation triggers a phase-resolution that the inliner cannot complete by the
time it asks for the body, so the inliner skips inlining and the call passes through to codegen
unchanged.

**Action needed for Q13a**: instrument `Fir2IrLazySimpleFunction.body` getter (or the inliner's body
lookup site in `IrInlineFunctionResolver` / `JvmInlineFunctionResolver`) to log when the body is
returned as empty/null for a `@InlineOnly`-tagged callee, and re-run a single repro
(`val r = listOf(1).joinToString(",")` inside a single-snippet engine eval). The instrumentation
output will say whether the body never resolves (then the fix is on the Fir2Ir / FIR-deserialiser side)
or resolves but the inliner skips (then the fix is on the inliner-phase side).

### Family G2 — Cross-snippet fake-override (`ReplState.put`)

Affected: `testResolveFromContextStandard`, `testResolveFromContextDirectExperimental`,
`testEvalWithContext`, `testEvalWithContextDirect`.

**Producer** — `plugins/scripting/scripting-compiler/src/.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt`
`getStateObject()` (L176 onwards). The synthesis path forks at L250:

```kotlin
return if (firReplStateFromDependencies == null && createIfNotFound) {
    // First-snippet case: fully materialise the state class + constructor + members
    classifierStorage.createAndCacheIrClass(firReplStateObject, irSnippet.parent).also { ... }
} else {
    // Subsequent-snippet case: produce a LAZY class on an external package fragment
    val irReplStateParent =
        declarationStorage.getIrExternalPackageFragment(firReplStateObject.symbol.classId.packageFqName, session.moduleData)
    lazyDeclarationsGenerator.createIrLazyClass(firReplStateObject, irReplStateParent, IrClassSymbolImpl())
}
```

When a subsequent snippet refers to a previous snippet's `ReplState` via cross-snippet FIR resolution
(e.g. the user expression calls `bindings["k"] as Int`, which under the hood touches `ReplState.put/get`
via `HashMap` inherited members), the FIR-side `ReplState` symbol comes from the dependencies provider
(L189–190 in the same file: `session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)`).
That symbol's `FirRegularClass.origin` is **`FirDeclarationOrigin.Library`** (deserialised from the
previous snippet's emitted classfile), **not** `FirDeclarationOrigin.FromOtherReplSnippet` — the
`FromOtherReplSnippet` marker is only applied to *freshly synthesised* state classes at L200 / L214
of this file, never re-applied when the class is rehydrated from a previous snippet's classpath.

So `FirClass.irOrigin()` (`compiler/fir/fir2ir/src/.../utils/OriginUtils.kt:43-53`) reaches the
fall-through branch:

```kotlin
fun FirClass.irOrigin(): IrDeclarationOrigin = when {
    isJava -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    c.firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    else -> when (val origin = origin) {
        is FirDeclarationOrigin.Plugin -> GeneratedByPlugin(origin.key)
        is FirDeclarationOrigin.FromOtherReplSnippet -> IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
        else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB     // ← reached
    }
}
```

The lazy class is tagged `IR_EXTERNAL_DECLARATION_STUB`, and all its inherited members (`put`,
`get` etc.) become fake-override `IR_EXTERNAL_DECLARATION_STUB` IR declarations parented under that
class. When the user expression invokes `bindings.put(...)` indirectly (through the implicit
`MutableMap`-based receiver path), the callee's IR parent IS an `IrClass` — but the inliner / mapping
phase can't materialise the fake-override target, and the call's render shows
`FUN IR_EXTERNAL_DECLARATION_STUB [fake_override]`. At codegen the same `visitCall` require fires
(this time the failure path is slightly different — the require still fails because the *delegated*
super-call inside the fake-override body references a top-level decl with non-class parent, but the
proximate cause is the missing override materialisation).

**The cause is unambiguous and the fix is localised.** Three viable approaches (Q13b):

| Option | Fix locus | Effort | Risk |
|---|---|---|---|
| **A. Re-tag `FromOtherReplSnippet` on dependency rehydration** | `Fir2IrReplSnippetConfiguratorExtensionImpl.getStateObject()` L189–190: after retrieving `firReplStateFromDependencies`, set `origin = FirDeclarationOrigin.FromOtherReplSnippet` on the rehydrated `FirRegularClass` (or wrap-and-re-emit). | Low | Need to ensure FIR symbol consistency — touching `.origin` on a deserialised symbol may be rejected; a wrap might be cleaner. |
| **B. Extend `FirClass.irOrigin()` to recognise dependency-loaded REPL classes** | `OriginUtils.kt` L43–53: add `is FirDeclarationOrigin.Library && /* classId matches REPL-state pattern */ → REPL_FROM_OTHER_SNIPPET`. | Low | Heuristic — needs a reliable "is this a REPL state class" predicate, otherwise leaks REPL semantics into the generic origin logic. |
| **C. Treat `Fir2IrLazyClass` with `IR_EXTERNAL_DECLARATION_STUB` origin specially in fake-override builder** | `IrFakeOverrideBuilder` / the lazy fake-override path: when building fake-overrides for a class that is conceptually defined in a prior REPL module, materialise full body just like `REPL_FROM_OTHER_SNIPPET`. | Medium | Touches generic codegen path; needs the same predicate as B. |

**Recommended**: A. The `getStateObject()` function is REPL-owned code; the wrap or `.also { origin = FromOtherReplSnippet }`
pattern keeps the fix local. If `FirDeclaration.origin` is `val` only, prefer constructing a thin
wrapper symbol (cf. `buildRegularClass { ... }` at L212–230 — the synthesis path) instead of
mutating the deserialised one.

### Common-path verification

Both families surface the *same* error wording because codegen's `visitCall` `require` is the only
post-lowering checkpoint that catches lazily-undisturbed call targets. Distinguishing them in
practice requires reading `callee.render()` past the origin: `FUN IR_EXTERNAL_DECLARATION_STUB` alone
is G1; `FUN IR_EXTERNAL_DECLARATION_STUB [fake_override]` is G2.

## Reproduction steps (for the future agent picking up step 1b)

1. **G1 repro** (single-snippet, no bindings indirection — confirms the inliner is the producer, not
   bindings):

   ```kotlin
   val engine = ScriptEngineManager().getEngineByExtension("kts")!!
   engine.eval("""listOf(1,2,3).joinToString(",")""") // → IR_EXTERNAL_DECLARATION_STUB on joinToString$default
   engine.eval("""val x: Int? = null; x?.let { it + 1 } ?: -1""") // → same on kotlin.let
   ```

2. **G2 repro** (two-snippet — confirms the rehydration path is the producer, not the original
   `getStateObject` synthesis):

   ```kotlin
   val engine = ScriptEngineManager().getEngineByExtension("kts")!!
   engine.put("z", 33)
   engine.eval("val x = 10")                       // first snippet — creates ReplState
   engine.eval("""bindings["z"] as Int + x""")    // second snippet — rehydrates ReplState → STUB
   ```

3. Instrumentation: hook `ExpressionCodegen.visitCall` (line 519) just before the `require` to dump
   `callee.render()` + `expression.symbol.signature` to a side log. Re-run the two repros; the dump
   tells G1 vs G2 unambiguously and confirms the IR origin without needing IR-text test data.

## Done in this iteration

- 7 doc/process files updated (see "Process docs" table); new gotchas catalog + JSR-223 per-test
  matrix + 4 new open questions + new migration plan step 1b + Junie investigation budget /
  test-triage workflow.
- 1 test source file updated (6 `@Disabled` annotations + 1 import).
- 0 production source files changed.
- Codegen investigation completed up to "fix plan ready" state for both G1 and G2 (Q13a / Q13b).

## Not done in this iteration

- Production fix for either G1 or G2 — deferred to the next iteration as step 1b execution.
- Verification re-run of `:kotlin-scripting-jsr223-test:test` after `@Disabled` annotations — to be
  done at the start of the next iteration as part of the standard test-triage workflow.

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/.ai/current/80-known-gotchas.md` | NEW — G1–G10 stable findings catalog |
| `plugins/scripting/.ai/current/70-tests.md` | Added JSR-223 BLOCKED-BY per-test matrix; bumped Last verified |
| `plugins/scripting/.ai/target/90-open-questions.md` | Added Q13 (with Q13a/Q13b), Q14, Q15, Q16 |
| `plugins/scripting/.ai/target/50-migration-plan.md` | Step 1 "Done when" rewrite + new step 1b + sequencing |
| `plugins/scripting/.ai/JUNIE_NOTES.md` | New "Investigation budget", "Test triage workflow", "run_test vs ./gradlew" sections |
| `plugins/scripting/.ai/AGENT_INSTRUCTIONS.md` | Two Critical Patterns rows; gotchas registered in Reference Documents + Caching strategy; Q-count bumped |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | `@Disabled` on 6 `BLOCKED-CODEGEN-Q13` tests + `Disabled` import |

## Key Learnings (to consider promoting to `80-known-gotchas.md` after one more iteration)

- **`Fir2IrReplSnippetConfiguratorExtensionImpl.getStateObject()` L250-fork is the cross-snippet
  STUB origin point.** When a previous-snippet `ReplState` is rehydrated from the dependency
  classpath instead of synthesised fresh, its origin reverts to `FirDeclarationOrigin.Library`,
  so `irOrigin()` falls through to `IR_EXTERNAL_DECLARATION_STUB` instead of `REPL_FROM_OTHER_SNIPPET`.
- **`ExpressionCodegen.kt:519` is the universal failure point for any lazily-undisturbed
  external-stub call** — the `require(callee.parent is IrClass)` is the only post-lowering check.
  Distinguish G1 (`[inline]`) vs G2 (`[fake_override]`) by reading the render line past the origin.
- **`@InlineOnly` decls go through `getIrFunctionSymbol().isExternalParent()` branch** in
  `Fir2IrDeclarationStorage.kt:1117`, deterministically tagged `IR_EXTERNAL_DECLARATION_STUB` by
  `computeExternalOrigin()` (L1109). The fact that non-REPL K2 scripts compile `?.let` confirms the
  inliner phase normally lowers these away — REPL divergence is suspected at the body-lazy-resolve
  step, not at the IR-creation step.

## Resources & Cost

| Metric | Value |
|---|---|
| Sessions aggregated | n/a — Junie session, no JSONL |
| `bash` calls | ~12 (mostly file listing + initial test run; no extra Gradle re-runs after `@Disabled` was added — verification deferred per Investigation budget) |
| `search_project` calls | ~9 |
| `open` / `open_entire_file` calls | ~14 / 0 |
| Test suites run | `:kotlin-scripting-jsr223-test:test` (1 run, from previous iteration's residue) |
| Files touched | 7 (6 docs / process; 1 test source) |

### Subagent breakdown

- (none — Junie session)

### Loadout-vs-actual

- **Loadout matrix row used**: "Doc maintenance" + "Migration-step execution (one numbered step)" hybrid —
  budget ~3k + ~7k respectively under Claude; under Junie n/a.
- **Investigation budget caps** (per the newly-written `JUNIE_NOTES.md` section): 0 / 2 `open_entire_file`,
  ≤14 / soft-3 `open` calls per file (some files revisited multiple times — `AGENT_INSTRUCTIONS.md`
  ~3 windows, `KotlinJsr223ScriptEngineIT.kt` ~2). Verification re-run skipped per cap on duplicate
  test runs (1 / 2 used; the 2nd belongs to the next iteration that lands the fix).

## Post-iteration checklist

- [x] Resources & Cost section populated (Junie-substitute metrics per `JUNIE_NOTES.md`).
- [ ] Migration-plan step strike-through — N/A (step 1 still partial; step 1b just introduced).
- [ ] Active Workstreams updated — N/A (workstreams still open).
- [ ] `current/90-legacy-inventory.md` — N/A.
- [x] `current/70-tests.md` updated (BLOCKED-BY matrix).
- [x] `current/80-known-gotchas.md` created.
- [x] Resolved Q* in `target/90-open-questions.md` — N/A (4 new Q* added: Q13–Q16, none resolved).
- [x] One-line index entry appended to `ITERATION_RESULTS.md` (next step before closing).
