# JSR-223 K2 bindings — binding lifecycle (Q10c removal + Q10d type-change) — 2026-07-01

## Overview

Closed the two binding-lifecycle open questions for in-process JSR-223 on K2 (migration step 1 / Option D): **Q10d** (a binding rebound to a value of a *different type*) and **Q10c** (a *removed* binding). Both stemmed from the synthetic-binding generator emitting each typed accessor exactly once and never revisiting it: the stale `var x: <oldType>` accessor persisted, so a retyped binding failed to compile / `ClassCastException`-ed and a removed binding NPEd with the cryptic `null cannot be cast to non-null type ...`. `generateBindingSnippetIfNeeded` now **diffs** the accumulated exposed set against the current bindings and re-emits accordingly.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Resolves `target/90-open-questions.md` Q10c + Q10d.

## Root cause

`generateBindingSnippetIfNeeded` (compile-side `prependSyntheticSnippets` handler) accumulated an `exposedBindings: Map<String, KotlinType>` config that is threaded across evals (the engine writes the compiled snippet's `compilationConfiguration` back into its field — `KotlinJsr223ScriptEngineImpl.compile`, line ~130). The emit loop skipped `if (knownBindings.containsKey(k)) continue`, so a name seen once was never re-emitted:

- **Q10d** — rebinding `z` (Int) as a String left the earlier `var z: Int` (with an `as Int` getter) as the most-recent declaration; a later `z.length` is `Unresolved reference 'length' on receiver of type 'Int'` (or, when the shapes align, a `ClassCastException` at the getter).
- **Q10c** — removing `z` left `var z: Int` with `get() = bindings["z"] as Int`; `bindings["z"]` is now `null` → `NullPointerException: null cannot be cast to non-null type kotlin.Int`.

## Design decision

- **Q10d → re-emit (shadow).** Emit a fresh typed accessor for any binding that is *new* or whose `KotlinType` changed (equality is by type name + nullability, so it survives config threading where `KotlinType.fromClass` is `@Transient`). The new accessor shadows the stale one in subsequent snippets.
- **Q10c → shadowing marker (throws).** For a name that was exposed before but is no longer present (removed, or absent in the current eval's context), emit a shadowing accessor that **keeps the previous declared type** (so existing user code still type-checks) but whose getter throws `NoSuchElementException("JSR-223 binding \"x\" is no longer available")` — replacing the cryptic cast NPE with an actionable message. Re-adding the binding emits a fresh typed accessor (it is "new" relative to the recomputed set) which shadows the throwing marker again.

The authoritative exposed set (`exposedBindings`) is now recomputed to the current bindings each round (drops removed names, updates retyped ones), so the next snippet diffs against an accurate baseline.

## Changes

- `libraries/scripting/jvm-host/src/kotlin/script/experimental/jvmhost/jsr223/propertiesFromContext.kt` — replaced the "emit-once" loop with: (1) build `currentBindings` (name → inferred `KotlinType`, applying the existing identifier / parseable-type filters); (2) Q10d — emit a typed accessor for every `currentBindings` entry whose type differs from `knownBindings` (`knownBindings[name] != type`); (3) Q10c — for `knownBindings.keys - currentBindings.keys`, emit a throwing shadow keeping the previous type; (4) write `exposedBindings = currentBindings`.
- `libraries/scripting/jsr223-test/test/kotlin/script/experimental/jsr223/test/KotlinJsr223ScriptEngineIT.kt` — new `testRebindWithChangedType` (bind Int → use → rebind String → use) and `testRebindRemoval` (bind → use → remove → assert clear "no longer available" error → re-add → use); added `import org.junit.jupiter.api.assertThrows`.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jsr223-test:test` | 21 / 0-fail / 2-skip (new tests fail on baseline: `Unresolved reference 'length'` for type-change; `NPE: null cannot be cast to non-null type kotlin.Int` for removal) | **23 / 0-fail / 2-skip** | `testRebindWithChangedType` + `testRebindRemoval` PASS; all prior rows still PASS. Remaining 2 skips = Q14 / Q16 (design sign-off). |
| `:kotlin-scripting-jvm-host-test:test` | green | green | regression check for the changed module (`jvm-host`). |

Pre-fix reproduction confirmed both bugs exactly (see the baseline column) before the fix.

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Diff-based (re)emission: re-emit on new/retyped binding (Q10d); throwing shadow on removed/absent binding (Q10c); recompute `exposedBindings`. |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | New `testRebindWithChangedType` + `testRebindRemoval`; `assertThrows` import. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q10c + Q10d → resolved; Q10 umbrella + header "Last verified" bumped. |
| `plugins/scripting/.ai/current/70-tests.md` | Two new matrix rows; step-1 acceptance note + header bumped (23 / 2-skip / 0-fail). |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry + JSR-223 workstream row updated. |

## Key Learnings

- `exposedBindings` is a **cross-eval accumulator** (threaded via the compiled snippet's config), so a per-snippet generator that emits a name once permanently freezes that name's declared type. Any binding-shape change (type or presence) must be handled by diffing the accumulator against the live bindings and re-emitting a shadowing declaration.
- REPL redeclaration/shadowing works across synthetic snippets: a later `var z: String` (or a same-typed throwing shadow) correctly shadows the earlier `var z: Int` — no "conflicting declarations".
- `KotlinType` equality is `typeName` + `isNullable` only (`fromClass` is `@Transient`), which is exactly what makes the type-change diff robust after the config has been threaded/serialized.
- Custom-`Bindings` evals legitimately make some engine-scope names absent; the Q10c handling treats that as "not available in this context" (throwing shadow), and re-emits on return to a context where the name is present — which keeps the existing `testEvalWithContext*` behavior intact.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional `target/90-open-questions.md` Q10, `current/70-tests.md`).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: on budget (~9k row) — one repro build + one fix/verify build + one module regression build; no wrong-hypothesis detours.
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through — N/A: Q10c/d are step-1 residuals, not a whole step; step 1 stays "In progress" (Q14/Q15/Q16 design sign-off + `Jsr223BindingsConfigurator` extraction remain; classloader-reflection postponed).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [x] `current/70-tests.md` updated (matrix rows + step-1 acceptance note + header).
- [x] `target/90-open-questions.md` — Q10c + Q10d flipped to resolved.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
