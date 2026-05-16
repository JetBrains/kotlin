# JSR-223 K2 bindings — Q16 implicit-receiver strategy: add `ScriptTemplateWithBindings` as a second receiver — 2026-07-02c

## Overview

Follow-up to the same-day Q16 investigation (no code changes) that concluded "add a second implicit receiver" requires zero FIR/IR/evaluator changes, since the whole implicit-receiver pipeline is already generic over N receivers. This iteration implements that option: `ScriptTemplateWithBindings` is added alongside `ScriptContext` as a second implicit receiver of `$$eval`, restoring reachability of the K1-era `fun ScriptTemplateWithBindings.foo(...)` extension-helper idiom.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Resolves `target/90-open-questions.md` Q16; resolves `current/80-known-gotchas.md` G10.

## Design decision

- **Chosen**: add `ScriptTemplateWithBindings::class` as a second implicit receiver, next to `ScriptContext::class`, in both `configureExposedJsr223Context` overloads (compile-time `beforeCompiling` + eval-time `refineConfigurationBeforeEvaluate`).
- **Compile-time**: generalized the existing single-receiver idempotency guard (which protected `testSimpleEvalInEval` from receiver-count drift across nested evals) into a list-based guard over `REQUIRED_IMPLICIT_RECEIVERS = listOf(ScriptContext::class, ScriptTemplateWithBindings::class)`; only the currently-missing ones are appended.
- **Eval-time**: added a private `Jsr223ScriptTemplateWithBindings(bindings: Map<String, Any?>) : ScriptTemplateWithBindings(bindings)` concrete wrapper (the base class is abstract) and pass an instance wrapping the *same* live `ScriptContext.ENGINE_SCOPE` `Bindings` map already used for the `ScriptContext` receiver — so the two receivers share one underlying data source with no separate synchronization needed.
- **Ambiguity risk**: `ScriptTemplateWithBindings` exposes only a `bindings` property; `ScriptContext` exposes JSR-223-specific methods (`getBindings`, `getAttribute`, `getWriter`, ...) — no member-name collision under normal use. Verified in practice: no ambiguous-receiver diagnostics observed after un-`@Disabled`-ing the target test.
- **Not chosen**: dropping the helper API (breaks K1-era user code) and switching `$$eval`'s sole receiver to `ScriptTemplateWithBindings` (would still need `ScriptContext` features reachable somehow, re-introducing a second receiver in disguise, for no benefit over the chosen option).

## Changes

- `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`: added `REQUIRED_IMPLICIT_RECEIVERS` list; generalized `configureExposedJsr223Context(ScriptConfigurationRefinementContext)` to add any missing receivers from that list idempotently; added private `Jsr223ScriptTemplateWithBindings` wrapper class; `configureExposedJsr223Context(ScriptEvaluationConfigurationRefinementContext)` now also adds a `Jsr223ScriptTemplateWithBindings` instance wrapping the engine-scope bindings.
- `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt`: un-`@Disabled`-ed `testEvalInEvalWithBindingsWithLambda`; removed the now-unused `org.junit.jupiter.api.Disabled` import.
- `plugins/scripting/.ai/target/90-open-questions.md`: Q16 flipped to resolved, options table annotated, new "known follow-up" note about the separate `MainKtsJsr223Test` classpath-discovery issue (likely Q6/KT-82551).
- `plugins/scripting/.ai/current/80-known-gotchas.md`: G10 marked FIXED with the fix summary.

## Test Results

| Suite | Result | Notes |
|---|---|---|
| `:kotlin-scripting-jvm-host:assemble` | **PASS** | Compiles cleanly with the new dual-receiver code. |
| `:kotlin-scripting-jsr223-test:test` (`KotlinJsr223ScriptEngineIT`) | **23 / 0-fail / 0-skip** | Full regression — the previously `@Disabled` `testEvalInEvalWithBindingsWithLambda` now passes; no new failures, no ambiguous-receiver diagnostics. |
| `:kotlin-main-kts-test:test` (`MainKtsJsr223Test`) | **0 / 3-fail** (pre-existing, unrelated) | All 3 fail with `Unresolved reference 'getBindings'` — confirmed via error text this is the same pre-existing issue flagged in the prior Q16-investigation iteration, not caused by this change. Root cause looks like Q6 (classpath-based script-definition discovery, KT-82551): `ScriptDefinitionsFromClasspathDiscoverySource` reflectively reloads `MainKtsScriptDefinition` from the `META-INF/kotlin/script/templates/*` classpath marker, bypassing the `hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }` override `KotlinJsr223ScriptEngineImpl` applies at runtime — so even the pre-existing single `ScriptContext` receiver never gets added for that rediscovered config. Left open, documented in Q16 as a follow-up, out of scope for this iteration. |
| `:kotlin-main-kts-test:test` (`MainKtsTest`, `CacheDirectoryDetectorTest`) | **23/0-fail + 9/0-fail** | Unaffected by this change — no regression. |
| `:kotlin-main-kts-test:test` (`MainKtsIT`) | **14/2-fail** | `testCachedReflection` / `testCacheWithFileLocation` fail (`expected:<1> but was:<0>`) — CLI-subprocess cache-hit-count smoke tests with no dependency on `propertiesFromContext.kt`; confirmed failing in isolation too, unrelated to this change's code path. |

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Dual implicit receiver (`ScriptContext` + `ScriptTemplateWithBindings`), idempotent list-based guard, new `Jsr223ScriptTemplateWithBindings` wrapper. |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | Un-disabled `testEvalInEvalWithBindingsWithLambda`; removed unused `Disabled` import. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q16 resolved; follow-up note on the separate main-kts issue. |
| `plugins/scripting/.ai/current/80-known-gotchas.md` | G10 marked FIXED. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry appended. |

## Key Learnings

- **Investigate before assuming a compiler-infra change is needed.** The prior same-day investigation iteration already proved the implicit-receiver pipeline was N-ary end-to-end; this iteration confirms that finding held up under an actual implementation + full regression run — zero FIR/IR/evaluator edits were needed, only host-side JSR-223 wiring.
- **A test suite that's "green" doesn't mean every consumer of the same production code is exercised.** `MainKtsJsr223Test`'s independent, pre-existing failure (present both before and after this change) shows the dedicated `jsr223-test` module's 23/23 doesn't cover the `main-kts` JSR-223 template — a different, still-open issue (likely entangled with Q6/KT-82551 classpath discovery) needs its own investigation.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional `target/90-open-questions.md` Q16).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: slightly over — the main-kts failure investigation (tracing into `ScriptDefinitionsFromClasspathDiscoverySource`) added scope beyond the core fix, though it stopped short of a full root-cause/fix (documented as a follow-up instead).
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [x] Migration-plan step strike-through — N/A: Q16 is a step-1 residual, not a whole step; step 1 stays "In progress" (Q15 typed-lambda-access design + `Jsr223BindingsConfigurator` extraction remain).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [ ] `current/70-tests.md` — not updated this iteration (no new test added, only un-disabled; test count/skip note in the Q16 section covers it).
- [x] `current/80-known-gotchas.md` G10 marked FIXED.
- [x] `target/90-open-questions.md` — Q16 resolved.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
