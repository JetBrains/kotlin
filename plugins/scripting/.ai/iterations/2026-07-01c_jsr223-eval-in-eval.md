# JSR-223 K2 bindings — eval-in-eval re-entrancy — 2026-07-01

## Overview

Fixed the eval-in-eval re-entrancy bug (migration step 1 / Option D, the last `STEP-1-FOLLOWUP` residual): a nested `eval("...")` inside a snippet threw `javax.script.ScriptException: java.lang.IllegalArgumentException: wrong number of arguments`. Root cause was an **implicit-receiver arity mismatch**, not evaluator re-entrancy as previously hypothesised. `configureExposedJsr223Context` (a `beforeCompiling` refinement) now adds the `ScriptContext` implicit receiver **idempotently**, un-blocking `testSimpleEvalInEval`.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Closes the eval-in-eval `STEP-1-FOLLOWUP` row in `current/70-tests.md`.

## Root cause (diagnosis-driven)

The JSR-223 engine (`KotlinJsr223ScriptEngineImpl`) threads and **mutates a single `compilationConfiguration` field** across evals (`compile(...)` writes `it.value.get().compilationConfiguration` back into the field). Separately, the generated `eval(script)` helper (emitted by `generateBindingSnippetIfNeeded`) removes `kotlin.script.state` from the engine bindings before re-entering, so a nested `eval(...)` runs on a **fresh REPL state** — and `createState()` seeds that fresh state from the same mutating `compilationConfiguration` field.

`configureExposedJsr223Context` (compile side) appended `implicitReceivers(ScriptContext::class)` unconditionally on every `beforeCompiling` refinement. Because the receiver leaked into the threaded/stored config, the count grew across evals. Instrumentation (temporary `System.err` probes in `evalSnippet` and both `configureExposedJsr223Context` variants) showed:

- Outer state: each snippet `$$eval` has **1** `ScriptContext` param; eval passes **1** arg → OK.
- Nested fresh state (seeded from the accumulated field): snippet `$$eval` has **3** `ScriptContext` params (`in=2 out=3`), while the evaluator always passes **1** (the eval config is a fixed `by lazy`, never accumulates) → `Method.invoke` "wrong number of arguments".

The evaluator's pending-chain walk (`K2ReplEvaluator`) was **not** the culprit; an earlier in-flight/evaluated-set guard attempt both failed to fix this and regressed `testSimpleCompilableWithBindings` (it blocked legitimate re-evaluation of the same compiled snippet), so it was reverted.

## Changes

- `libraries/scripting/jvm-host/src/kotlin/script/experimental/jvmhost/jsr223/propertiesFromContext.kt` — `configureExposedJsr223Context(ScriptConfigurationRefinementContext)` now adds `ScriptContext` as an implicit receiver only when it is not already present (`implicitReceivers?.contains(KotlinType(ScriptContext::class))`). `KotlinType.equals` compares `typeName` + `isNullable`, so the presence check is robust even if `fromClass` (transient) is dropped by config threading. Caps the receiver at exactly one in every state, including the nested fresh state; eval side unchanged (its base is a fixed `by lazy`, so it never accumulated).
- `libraries/scripting/jsr223-test/test/kotlin/script/experimental/jsr223/test/KotlinJsr223ScriptEngineIT.kt` — removed `@Disabled` from `testSimpleEvalInEval` (now PASS).

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jsr223-test:test` (`KotlinJsr223ScriptEngineIT`) | 21 / **2 fail** (`testSimpleEvalInEval` + `testSimpleCompilableWithBindings`, with the first attempt) / 2 skipped | 21 / **0 fail / 2 skipped** | `testSimpleEvalInEval` fail→PASS; `testSimpleCompilableWithBindings` stays PASS (first-attempt regression avoided). Remaining 2 skips = Q14 / Q16 (design sign-off). |
| `:kotlin-scripting-jvm-host-test:test` | green | green | regression check for the changed module (`jvm-host`). |

Pre-fix reproduction confirmed: `javax.script.ScriptException: javax.script.ScriptException: java.lang.IllegalArgumentException: wrong number of arguments` at `K2ReplEvaluator.evalSnippet` (`eval.invoke`), inner (fresh) evaluator.

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Add `ScriptContext` implicit receiver idempotently (compile-side refinement). |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | Un-`@Disabled` `testSimpleEvalInEval`. |
| `plugins/scripting/.ai/current/70-tests.md` | `testSimpleEvalInEval` → PASS; step-1 acceptance note (only Q14/Q16 blocked); Last verified bumped. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry + JSR-223 workstream row updated. |

## Key Learnings

- The mismatch was between a **mutating** compile config (`compilationConfiguration` field, written back after each compile) and a **fixed** eval config (`by lazy`). A per-snippet `beforeCompiling` refinement that appends to the config is safe only if it is idempotent — otherwise it accumulates once the config is threaded/reused, and any freshly-seeded (nested) state amplifies it.
- The visible symptom (`Method.invoke` "wrong number of arguments" in `K2ReplEvaluator`) pointed at the evaluator, but the evaluator was correct; the arity divergence originated in the compile-side receiver list. Instrumenting **both** the reflective invoke (params vs args) and the config refinements (in/out receiver counts) localized it in one build.
- `KotlinType.typeName` + `isNullable` are the equality basis (`fromClass` is `@Transient`); a `contains(KotlinType(SomeClass::class))` presence check therefore survives config serialization/threading.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional `current/70-tests.md`, `target/90-open-questions.md`).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: slightly over the ~9k row — the root cause required temporary instrumentation + several targeted builds (one wrong hypothesis reverted) before the one-line idempotent fix.
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through — N/A: eval-in-eval is a step-1 residual, not a whole step; step 1 stays "In progress" (Q14/Q15/Q16 design sign-off + `Jsr223BindingsConfigurator` extraction remain; classloader-reflection postponed).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [x] `current/70-tests.md` updated (matrix + step-1 acceptance note).
- [ ] `target/90-open-questions.md` — N/A: eval-in-eval was not a formal Q* (tracked as a `STEP-1-FOLLOWUP` row in `70-tests.md`); no Q status to flip.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
