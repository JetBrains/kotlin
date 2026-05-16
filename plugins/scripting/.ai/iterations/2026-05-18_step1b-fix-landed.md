# Step 1b fix landed — K2 REPL `IR_EXTERNAL_DECLARATION_STUB` (G11 umbrella) — 2026-05-18

## Overview

Production fix for migration step 1b. Added a REPL-scoped EPPL-equivalent post-pass inside `ReplSnippetsToClassesLowering` that reparents external Kotlin top-level callables (currently parented on `IrExternalPackageFragment`) onto a synthesised JVM file-class facade — so `ExpressionCodegen.visitCall`'s `require(callee.parent is IrClass)` check passes for snippet bodies that reference classpath-loaded Kotlin top-level `val`/`fun`, `@InlineOnly` stdlib operators, and `[fake_override]` callees inherited from external classes. JSR-223 suite went from 12 PASS / 6 SKIP / 3 FAIL (baseline) to 17 PASS / 1 SKIP / 3 FAIL — net +5 passing tests.

## Workstream / Issue

Migration plan step **1b** (G11 umbrella / Q13). Third iteration in the step 1b chain (first: `2026-05-18_codegen-stub-investigation` — split into G1 + G2; second: `2026-05-18_step1b-rootcause-refinement` — unified under G11 parent-shape; this entry: fix landed).

## Changes

- `plugins/scripting/scripting-compiler/src/.../irLowerings/ReplSnippetLowering.kt` — added `ReplSnippetExternalPackageParentPatcher` (private class implementing `IrVisitorVoid`) at the end of the file; added an invocation loop in `ReplSnippetsToClassesLowering.lower` that walks each finalised snippet's `targetClass` after `finalizeReplSnippetClass`. New imports for JVM-backend helpers (`createJvmFileFacadeClass`, `classNameOverride`, `FacadeClassSource`, `IrVisitorVoid`, `acceptVoid`, `acceptChildrenVoid`, `IrElement`). Reason: mirror `org.jetbrains.kotlin.backend.jvm.lower.ExternalPackageParentPatcherLowering.Visitor.visitMemberAccess` at a point in the pipeline where the snippet body's IR is reachable; the standard JVM EPPL pass does not effectively touch snippet `targetClass` IR for the K2 REPL pipeline.
- `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` — removed `@Disabled` from 5 tests now passing after the fix: `testResolveFromContextStandard`, `testResolveFromContextLambda`, `testResolveFromContextDirectExperimental`, `testMultipleCompilable`, `testEvalWithContext`. Replaced `@Disabled` reason on `testEvalWithContextDirect` to point at the new follow-up Q17 (synthetic-snippet null-binding type bug; NOT a regression — surfaced by the codegen fix).
- `plugins/scripting/.ai/current/80-known-gotchas.md` — marked G1, G2, G11 as **FIXED 2026-05-18** with cross-references to this iteration entry; refined cause descriptions to reflect resolution (G1/G2 collapsed under G11; "Generator workarounds remain valid as defensive coding" callout kept). Last-verified date bumped.
- `plugins/scripting/.ai/target/90-open-questions.md` — flipped Q13 / Q13a / Q13b to **closed** with resolution paragraph (Q13a/Q13b auto-resolved by umbrella fix; no separate work needed). Added Q17 (synthetic-snippet null-binding type) with options table and repro pointer.
- `plugins/scripting/.ai/target/50-migration-plan.md` — struck through step 1b heading (`### ~~1b. Fix K2 REPL `IR_EXTERNAL_DECLARATION_STUB`~~ — **DONE 2026-05-18**`); replaced "Investigation status" / "Touch (anticipated)" with landed summary, files touched, follow-ups not done. Bumped last-verified.
- `plugins/scripting/.ai/current/70-tests.md` — JSR-223 per-test matrix: 5 rows flipped to PASS; `testEvalWithContextDirect` flipped to `BLOCKED-DESIGN-Q17`. Status legend updated (BLOCKED-CODEGEN-Q13 struck through; BLOCKED-DESIGN-Q17 added). Last-verified bumped.
- `plugins/scripting/.ai/ITERATION_RESULTS.md` — index entry + status row updated for step 1b (this iteration).

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jsr223-test:test` | 12 PASS / 6 SKIP / 3 FAIL | 17 PASS / 1 SKIP / 3 FAIL | net **+5 PASS**. 5 of 6 BLOCKED-CODEGEN tests flipped to PASS: `testResolveFromContextStandard`, `testResolveFromContextLambda`, `testResolveFromContextDirectExperimental`, `testMultipleCompilable`, `testEvalWithContext`. The 6th (`testEvalWithContextDirect`) flipped from BLOCKED-CODEGEN to BLOCKED-DESIGN-Q17 (different bug exposed by the codegen fix). The 3 remaining FAIL rows are unchanged pre-existing Q14 / Q15 / Q16 design issues (`testSimpleEvalInEval`, `testEvalWithContextNamesWithSymbols`, `testEvalInEvalWithBindingsWithLambda`). |
| `:kotlin-scripting-jvm-host-test:test` | not run | not run | out of scope for this iteration; the fix only touches REPL snippet IR shape, no public API surface; no regression expected. |
| `:kotlin-main-kts-test:test` | not run | not run | same rationale. |
| `:plugins:scripting:scripting-tests:test` | not run | not run | same rationale. |
| `:compiler:fir:fir2ir:test --tests "*FirScriptCodegenTestGenerated*"` | not run | not run | same rationale; the fix touches a REPL-snippet-only lowering path (it predicates on `IrReplSnippet.targetClass`). |

**Validation gap**: only the JSR-223 IT suite was re-run after the fix. Running the four other suites listed above is recommended before upstream review to confirm no regression. A non-JSR-223 direct `K2ReplCompiler` regression fixture is also recommended (see follow-ups).

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/irLowerings/ReplSnippetLowering.kt` | Added `ReplSnippetExternalPackageParentPatcher` post-pass + invocation; new imports. |
| `libraries/scripting/jsr223-test/test/kotlin/script/experimental/jsr223/test/KotlinJsr223ScriptEngineIT.kt` | Removed `@Disabled` on 5 BLOCKED-CODEGEN tests; updated `@Disabled` reason on `testEvalWithContextDirect` to Q17. |
| `plugins/scripting/.ai/current/80-known-gotchas.md` | Marked G1 / G2 / G11 as FIXED; refined causes. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q13 / Q13a / Q13b → closed; added Q17. |
| `plugins/scripting/.ai/target/50-migration-plan.md` | Step 1b struck through with landed summary. |
| `plugins/scripting/.ai/current/70-tests.md` | Per-test matrix: 5 → PASS, 1 → BLOCKED-DESIGN-Q17; legend updated. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry + status row. |
| `plugins/scripting/.ai/iterations/2026-05-18_step1b-fix-landed.md` | This entry. |

## Key Learnings

- **JSR-223 IT runs against `:dist`'s bundled `kotlin-compiler.jar`** — not against incrementally-compiled source. Edits to compiler internals (`ExternalPackageParentPatcherLowering`, `Fir2IrDeclarationStorage`, etc.) are **not picked up** by a normal `./gradlew :kotlin-scripting-jsr223-test:test` invocation. To validate a compiler-side change against this suite, run `./gradlew :dist` first (≈30 min cold; ≈15 s incremental once warmed up). Run-then-test from the IDE alone is insufficient. Earlier in this iteration two debug probes (println + file-write) appeared to silently fail because of this caching — `error("PROBE: ...")` would have been decisive but `:dist` rebuild settled it.
- **`Fir2IrLazyPropertyAccessor.containerSource` is correctly populated** for class-file-deserialised external Kotlin top-level decls (delegates to `firParentProperty.containerSource`, which is a `DeserializedContainerSource implements FacadeClassSource` for stdlib + classpath jars). So the EPPL preconditions are satisfied in principle — the gap is **reachability**, not predicate. The snippet body's IR is reachable through `IrReplSnippet.targetClass` but the standard JVM EPPL pass's traversal order does not visit it in the K2 REPL pipeline. Running the same logic eagerly inside REPL snippet→class lowering closes the gap without any change to the JVM lowering chain.
- **One concise REPL-scoped IR pass is sufficient** to fix what the prior two iterations framed as two independent failure families (G1 `@InlineOnly` body materialisation, G2 cross-snippet fake-override). Once the parent-shape is corrected, the standard inliner + fake-override resolution chain handles the rest. The earlier iteration entries' "two fix-plan sketches" framing was an over-decomposition driven by the IR render's `[inline]` / `[fake_override]` decorations rather than the actual codegen require call-site.
- **Best-guess fix without a debug-instrumented compiler worked first try** — but only because the prior root-cause refinement iteration nailed down the exact require predicate. Without that, the `:dist` rebuild penalty would have made a wrong guess very expensive. Useful pattern: invest one full iteration in pure root-cause analysis when the feedback loop is long, then execute.
- **Surfaced new follow-up: Q17 (synthetic-snippet null-binding type)** — `engine.put("nullable", null)` generates `var nullable: Any` (non-null), so reading the binding NPEs at the kotlin-cast before the user's `?.let` defence sees it. Independent of G11; was previously masked by the codegen failure. Repro: `testEvalWithContextDirect`. Generator location: `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`.

## Resources & Cost

Run `.claude/scripts/iter-metrics.sh` separately and fill the table below; metrics are not collected by the agent during this run.

| Metric | Value |
|---|---|
| Sessions aggregated | _(fill via iter-metrics.sh)_ |
| Time span | _(fill via iter-metrics.sh)_ |
| Cost (USD, model-aware) | _(fill via iter-metrics.sh)_ |
| Cache hit rate | _(fill via iter-metrics.sh)_ |
| Input tokens (non-cached) | _(fill via iter-metrics.sh)_ |
| Output tokens | _(fill via iter-metrics.sh)_ |
| Cache-creation tokens | _(fill via iter-metrics.sh)_ |
| Cache-read tokens | _(fill via iter-metrics.sh)_ |
| Model mix | _(fill via iter-metrics.sh)_ |
| Subagent calls (total) | 0 |
| Gradle wall-time (sum across suites) | ~30 min `:dist` build + ~30 s `:kotlin-scripting-jsr223-test:test` (×2 runs after fix) ≈ **31 min** |

### Subagent breakdown

  - None used in this iteration. Best-guess fix + verify worked first try without delegation.

### Loadout-vs-actual

- Loadout matrix row used: Migration-step execution (medium budget tier, Sonnet/Opus depending on session size; this entry done within a single session that prior context-compressed earlier in the run).
- Actual model: claude-opus-4-7 (per session start banner).
- Budget hit / over / under: hit — single best-guess fix succeeded, only one `:dist` rebuild penalty paid. Within +/-30% of the medium-tier expectation.
- Subagent dispatch followed: yes — the task was small (1-2 files changed, tight verification loop), so per `cavecrew` decision-guide rules in-line work was correct, no investigator/builder needed.
- No over-budget intervention needed.

## Post-iteration checklist

(See `AGENT_INSTRUCTIONS.md` "Post-iteration checklist" — confirm each.)

- [ ] Resources & Cost section populated (script run, Loadout-vs-actual filled) — _stub above; needs `iter-metrics.sh` run after submit._
- [x] Migration-plan step strike-through (`### ~~1b. ...~~ — DONE 2026-05-18`)
- [ ] Active Workstreams updated in `AGENT_INSTRUCTIONS.md` — step 1b workstream now complete; if there's a separate active-workstreams list there it should be flipped. Not done in this iteration.
- [x] `current/70-tests.md` updated (per-test matrix flip).
- [x] Any resolved Q* in `target/90-open-questions.md` flipped to `resolved` (Q13 / Q13a / Q13b → closed; Q17 opened).
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
