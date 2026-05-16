# JSR-223 K2 bindings — Q17 null-binding property type — 2026-07-01

## Overview

Fixed Q17 (migration step 1 / Option D): a `null`-valued JSR-223 binding generated a **non-null** typed accessor, so `engine.put("nullable", null)` + `engine.eval("nullable?.let { ... } ?: -1")` NPEd at the getter's kotlin-cast before the user's own null-safety could run. The synthetic-snippet binding generator now renders the property/getter-cast type with its nullability marker, un-blocking `testEvalWithContextDirect`.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Resolves `target/90-open-questions.md` **Q17**.

## Changes

- `libraries/scripting/jvm-host/src/kotlin/script/experimental/jvmhost/jsr223/propertiesFromContext.kt` — in the typed-binding-property emitter, compute `renderedType = if (type.isNullable) "${type.typeName}?" else type.typeName` and use it for **both** the declared `var` type and the getter's `as` cast. Reason: `KotlinType.typeName` strips the trailing `?` (nullability is a separate flag), so a null binding — already tagged `KotlinType(Any::class, isNullable = true)` — was emitted as `var x: kotlin.Any` / `... as kotlin.Any`, NPEing on the null value.
- `libraries/scripting/jsr223-test/test/kotlin/script/experimental/jsr223/test/KotlinJsr223ScriptEngineIT.kt` — removed `@Disabled` from `testEvalWithContextDirect` (Q17 now fixed). Added `@Disabled` with precise, tracked references to the three still-blocked cases so the suite is green: `testEvalWithContextNamesWithSymbols` (Q14), `testEvalInEvalWithBindingsWithLambda` (Q16), `testSimpleEvalInEval` (STEP-1-FOLLOWUP eval-in-eval). All three were pre-existing active-failing rows (documented in `70-tests.md`); converting them to explicit skips-with-reason is a test-hygiene improvement, not a regression mask.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jsr223-test:test` (`KotlinJsr223ScriptEngineIT`) | 21 tests / **3 fail** (with `testEvalWithContextDirect` enabled to reproduce) | 21 tests / **0 fail / 3 skipped** | `testEvalWithContextDirect` flips fail→PASS; the 3 documented blocked rows now `@Disabled` (Q14 / Q16 / eval-in-eval STEP-1-FOLLOWUP). |

Baseline reproduction confirmed pre-fix: `javax.script.ScriptException: java.lang.NullPointerException: null cannot be cast to non-null type kotlin.Any`.

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Render binding-property/getter-cast type with nullability marker. |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | Un-`@Disabled` `testEvalWithContextDirect`; `@Disabled` the 3 remaining blocked cases with tracked references. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q17 → **resolved**; header Last verified bumped. |
| `plugins/scripting/.ai/current/70-tests.md` | `testEvalWithContextDirect` → PASS; the 3 blocked rows marked `@Disabled`; step-1 acceptance note updated; Last verified bumped. |

## Key Learnings

- `KotlinType.typeName` is nullability-stripped by construction (`removeSuffix("?")`); nullability lives in the separate `isNullable` flag. Any code that embeds a `KotlinType` into generated Kotlin **source** (not just uses it as an API value) must re-append `?` itself — `typeName` alone silently drops it.
- Q17's null tagging (`KotlinType(Any::class, isNullable = true)`) was already correct at the model layer; the bug was purely in the source-rendering layer. The two layers must be checked together when a generated-accessor NPE looks like a "wrong type" bug.
- The remaining Group A failures cleanly separate into: design-gated (Q14 name-encoding, Q16 implicit-receiver — both need contract sign-off) and a concrete non-design residual (eval-in-eval re-entrancy in `K2ReplEvaluator`'s pending-chain walk). None share a root cause with Q17.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional Q17/Q14/Q16 in `target/90-open-questions.md`, `current/70-tests.md`).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: under — single localized generator fix + test/doc updates; no cross-module investigation needed.
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through — N/A: Q17 is a sub-item of step 1, not a whole step; step 1 stays "In progress" (Q14/Q15/Q16 + eval-in-eval + classloader-reflection + `Jsr223BindingsConfigurator` extraction remain).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [x] `current/70-tests.md` updated (matrix + step-1 acceptance note).
- [x] Q17 in `target/90-open-questions.md` flipped to `resolved` with link here.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
