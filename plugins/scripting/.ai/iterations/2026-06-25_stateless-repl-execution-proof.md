# Stateless K2 REPL — end-to-end execution proof (+ 3 runtime bugs fixed) — 2026-06-25

## Overview

Closed the loop on the stateless prototype: built the missing *execution* glue and a multi-snippet
run-and-assert test that compiles `val x = 42` → `val y = x + 1` → `x + y` through
`K2ReplStatelessCompiler` and actually **runs** the resulting artifacts, proving cross-snippet
runtime state (`x=42`, `y=43`, `x+y=85`) — not just diagnostics parity. Driving real execution
surfaced and fixed **three** prototype bugs that the diagnostics-only corpus could never have caught.

## Workstream / Issue

Migration step 3 (Stateless remote REPL compilation prototype) — round 7. Direction confirmed with
the user: approach is sound/workable; build the execution proof rather than redesign.

## Changes

- `…/impl/SnippetArtifactEvaluator.kt` — **new**. Execution-side counterpart of
  `K2ReplStatelessCompiler`: materialises every snippet's class bytes onto one in-memory
  `ClassLoader` and invokes each `$$eval` in history order, then reads the last snippet's result
  field. Internal/prototype-only, mirrors the compiler's placement.
- `…/services/ArtifactBackedFirReplHistoryProvider.kt` — **bug #1**: `isFirstSnippet` returned
  `false` for the empty-priors case, so the *first* stateless snippet never emitted the shared
  `ReplState` `HashMap` object → `NoClassDefFoundError: ReplState` at eval. Now returns `true` when
  there are no priors (the snippet being compiled is never in the reconstructed history).
- `…/services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt` — **bug #2**: a *deserialized* snippet
  wrapper has origin `Library` (not `Synthetic.ReplContainerClass`), so `getOrBuildActualParent`
  re-nested cross-snippet refs under a never-emitted `Wrapper$Wrapper` → `NoClassDefFoundError:
  S1_repl$S1_repl`. Added an identity discriminator (`isReconstructedSnippetContainerFor`) — the
  accessed symbol's `originalReplSnippetSymbol.snippetClass.symbol == container` — which a user
  class declared *inside* a snippet never satisfies, so it still nests correctly. Provably a no-op
  for the stateful path (the new clause is only reached when origin != ReplContainerClass).
- `…/impl/K2ReplCompiler.kt` — **bug #3 (producer)**: extended `snippetCompilationObserver` with the
  *actual* emitted result-field name, extracted via `extractResultFields(irModuleFragment)` at the
  fire site. The REPL result field is `res<snippetId>` (e.g. `res2`), not the `resultField` config
  default (`$$result`).
- `…/impl/K2ReplStatelessCompiler.kt` — threads the new `resultFieldName` through a small
  `CapturedCompile` holder into `buildSnippetArtifactFromCompile`.
- `…/impl/SnippetArtifactEmission.kt` — `buildSidecar` now records the real result-field name
  (falling back to the config value only when the producer had no IR).
- `…/test/K2ReplStatelessCompilerTest.kt` — **new** `testStatelessReplExecutesMultiSnippetSequence`.
- `libraries/scripting/jvm-host/…/jsr223/KotlinJsr223ScriptEngineImpl.kt` — replaced a broken Guava
  `Throwables.getStackTraceAsString` dependency (which blocked the `:kotlin-scripting-compiler:test`
  graph from compiling) with the stdlib `Throwable.stackTraceToString()`. Behaviour-identical.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-compiler:test --tests *K2ReplStatelessCompilerTest` | 4 pass (no exec test) | 5 pass / 0 fail | new execution test green |
| `:plugins:scripting:scripting-tests:test --tests *ReplStatelessDiagnosticsTestGenerated` | 24 / 0 | 24 / 0 | no regression |
| `:plugins:scripting:scripting-tests:test --tests *ReplViaApiDiagnosticsTestGenerated` | 24 / 0 | 24 / 0 | no regression (stateful frontend) |
| `:kotlin-scripting-jsr223-test:test --tests *KotlinJsr223ScriptEngineIT` | 17 pass / 3 fail / 1 disabled | 17 / 3 / 1 | **same 3 pre-existing** failures (STEP-1-FOLLOWUP `testSimpleEvalInEval`, Q14 `…NamesWithSymbols`, Q16 `…EvalInEvalWithBindingsWithLambda`); validates the jvm-host fix |

## Files Modified

| File | Change |
|---|---|
| `…/impl/SnippetArtifactEvaluator.kt` | new — in-memory replay evaluator |
| `…/services/ArtifactBackedFirReplHistoryProvider.kt` | `isFirstSnippet` empty-priors fix |
| `…/services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt` | deserialized-wrapper container discriminator |
| `…/impl/K2ReplCompiler.kt` | observer passes real result-field name |
| `…/impl/K2ReplStatelessCompiler.kt` | `CapturedCompile` holder threads result-field name |
| `…/impl/SnippetArtifactEmission.kt` | sidecar records real result-field name |
| `…/test/K2ReplStatelessCompilerTest.kt` | new execution test |
| `libraries/scripting/jvm-host/…/jsr223/KotlinJsr223ScriptEngineImpl.kt` | Guava → stdlib stack-trace render |

## Key Learnings

- **Diagnostics parity ≠ runnable.** The 24/24 stateless diagnostics corpus checks only the report
  stream; it never loaded a class or invoked `$$eval`. All three bugs here were invisible to it and
  only appeared under real execution. An execution smoke test is essential coverage for this
  workstream.
- **`ReplState` is emitted once, by the first snippet**, gated on `isFirstSnippet`. The stateless
  history provider only holds *priors*, so "first" must mean "no priors", not "head of the
  reconstructed list".
- **Origin is not a reliable wrapper discriminator across the stateful/stateless split.** A
  deserialized wrapper and a user class declared in a snippet both have origin `Library`; the
  distinguishing signal is `originalReplSnippetSymbol.snippetClass` identity.
- **The REPL result field is `resultFieldPrefix + snippetId` (e.g. `res2`)**, not the `resultField`
  config default `$$result`. The stateful evaluator gets the real name from `extractResultFields`;
  the sidecar must capture the same, not the config value.

## Resources & Cost

Not captured this session (metrics script not run in this environment). Single Junie session; the
expensive operations were 5 Gradle test invocations (stateless unit ×4 iterations, diagnostics ×1,
JSR-223 ×1), each ~10–35 s wall-time on a warm daemon.

## Post-iteration checklist

- [ ] Resources & Cost — metrics not captured (noted above)
- [x] No migration-plan step fully landed yet (step 3 still in progress — execution proof added)
- [x] Active Workstreams: step-3 row updated in `ITERATION_RESULTS.md`
- [ ] `current/90-legacy-inventory.md` — no artifact deleted
- [x] `current/70-tests.md` — unchanged JSR-223 baseline (3 known failures reconfirmed)
- [ ] `target/90-open-questions.md` — no Q* fully resolved (Q5* still open; execution-feasibility now demonstrated)
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
