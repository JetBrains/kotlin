# JSR-223 K2 bindings — backtick + delegated-property binding-name fix (Q14 refinement / G12) — 2026-07-02

## Overview

Continued investigation into the 2026-07-01e marker-encoding prototype: the user correctly suspected it was over-engineered, since Kotlin generally allows back-tick quoting for almost any identifier. Root-caused the *real* reason the prototype avoided backticks: a narrow K2 REPL/script-snippet parser bug (**G12**) where a backtick-quoted property with a hand-written `get()`/`set()` body fails to parse ("Property getter or setter expected") only when the same live-REPL snippet also calls `getBindings(...)` (an implicit-receiver call) — reproducible only through the real incremental JSR-223/REPL pipeline, not a one-shot `.kts` compile. Fixed by declaring those properties with a generated delegate (`by __Jsr223BindingDelegate(...)`) instead of a hardcoded accessor, which sidesteps the bug entirely (a delegate expression is consumed by `parsePropertyDelegateOrAssignment()` before the misfiring accessor-parsing code runs). This lets `encodeBindingNameToKotlinIdentifier` go back to plain backtick-quoting for every name except the JVM-hard-invalid subset (`. ; [ ] / < > : \`, backtick, newline), which still needs the `__<mnemonic>__` marker regardless of accessor style.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Refines `target/90-open-questions.md` Q14 (previously resolved 2026-07-01e as a marker-only prototype); adds new `current/80-known-gotchas.md` G12 and refines G8.

## Investigation

- Confirmed empirically (via `mcp_idea_generate_psi_tree` and standalone `dist/kotlinc -script` compiles) that Kotlin's grammar has no general problem with backtick-quoted properties containing space / `$` / non-ASCII / "dangerous" characters, including alongside a `get()`/`set()` body and a preceding call — a reduced standalone `.kts` reproduction of the failing shape compiled cleanly.
- The failure only reproduces through the *real* JSR-223/REPL pipeline (`K2ReplCompiler` / `KJvmReplCompilerBase`, `isReplSnippetSource` set), not a one-shot script compile — narrowing the trigger to something specific to live, incremental REPL-session state that a plain compile doesn't have. The exact extra state was not pinned down further (would need debugger instrumentation of the live compiler at the parser's `errorUntil` call site); this is now recorded as the open remainder of G12.
- The parser-level mechanism: `parseProperty(DeclarationParsingMode.SCRIPT_TOPLEVEL)` (`compiler/psi/parser/src/.../parsing/KotlinParsing.java`, mirrored in the K2 light-tree `KotlinParsing.kt`) parses accessor blocks via a newline/semicolon heuristic, the same statement-boundary ambiguity class that `DeclarationParsingMode.LOCAL` normally avoids by disabling accessors entirely; `SCRIPT_TOPLEVEL` is the one statement-sequence mode that still allows them.
- Key insight enabling the fix: a **delegated property** (`by ...`) is parsed by `parsePropertyDelegateOrAssignment()`, which runs and returns `true` *before* the accessor-parsing block (`if (mode.accessorsAllowed) { ... }`) is reached — so a backtick-quoted delegated property never exercises the buggy code path at all, regardless of what triggers it.

## Design decision

- **Character classification narrowed.** `NEEDS_MARKER_ENCODING_CHARS` now covers only characters that can't appear in a Kotlin declaration name under *any* quoting: the JVM-hard-invalid member-name chars (`. ; [ ] / < > : \`), plus backtick (can't nest) and raw newlines. Every other non-plain-identifier name (space, `$`, non-ASCII, JVM-"dangerous" `? * " | %`) is backtick-quoted **verbatim** — restoring the original K1 spelling for those names.
- **Delegated property for backtick-quoted names.** `generateBindingSnippetIfNeeded` emits a single `__Jsr223BindingDelegate<T>(bindings, key, removed = false)` class once per synthetic-snippet chain (alongside the existing `eval()` helpers). It implements `getValue`/`setValue` with the same unchecked-cast / `NoSuchElementException`-on-removal semantics the old hardcoded accessors had. `renderBindingProperty(...)` picks accessor style based on whether the encoded name is backtick-quoted: `by __Jsr223BindingDelegate<T>(bindings, "key")` for backtick names, plain `get()`/`set()` for everything else (unchanged, never part of the bug).
- **Marker encoding kept, scope narrowed.** Names containing a JVM-hard-invalid character are still marker-encoded exactly as before (unaffected by this change) — those characters can't survive in *any* Kotlin declaration name, delegate or not.

## Changes

- `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`:
  - Replaced the ASCII-only classification in `encodeBindingNameToKotlinIdentifier` with `NEEDS_MARKER_ENCODING_CHARS` (JVM-hard-invalid + backtick + newline only); everything else falls through to a verbatim backtick-quoted identifier.
  - Added the generated `__Jsr223BindingDelegate<T>` class (emitted once, next to the `eval()` helpers).
  - Added `renderBindingProperty(encodedName, renderedType, safeKey, removed)` — a single helper used by both the new/changed-binding loop and the removed-binding loop, picking `by __Jsr223BindingDelegate<...>(...)` for backtick-quoted names and the existing hardcoded `get()`/`set()` for plain/marker-encoded names.
  - Rewrote doc comments to describe the real cause (G12 REPL parser bug) instead of the previous "everything non-ASCII needs marker encoding" framing.
- `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt`: updated `testEvalWithContextNamesWithSymbols` assertions — names containing a JVM-hard-invalid character keep their marker spelling (`a__dot__b`, `c__colon__d`, `e__semicolon__f`, `i__lt__j`, `k__gt__l`, `m__lbracket__n`, `o__rbracket__p`, `q__slash__r`, `s__backslash__t`); every other name is referenced backtick-quoted (`` `☺` ``, `` `g$h` ``, `` `u v` ``, `` ` ` ``, `` `    ` ``).

## Test Results

| Suite | Result | Notes |
|---|---|---|
| `:kotlin-scripting-jsr223-test:test` (`KotlinJsr223ScriptEngineIT`) | **23 / 0-fail / 1-skip** | `testEvalWithContextNamesWithSymbols` PASS (0.653s), confirmed via the JUnit XML report (not just a green Gradle exit code) — genuine proof the delegate sidesteps G12 in the real pipeline. Only remaining skip is the pre-existing Q16 `@Disabled`. |
| `:kotlin-main-kts-test:test` | 10 pre-existing failures, **unrelated** | Verified by stashing this session's 2 changed files and re-running — the same failures (`testResolveJunit`, `testThreadContextClassLoader`, ...) reproduce identically without this fix; they are environment/network/launcher-process issues (e.g. Maven dependency resolution, external `kotlinc` subprocess invocation), not JSR-223 binding-related. |

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Narrowed marker-encoding scope to JVM-hard-invalid chars; added `__Jsr223BindingDelegate` + `renderBindingProperty`; rewrote doc comments. |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | Rewrote `testEvalWithContextNamesWithSymbols` assertions for the mixed marker/backtick spellings. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q14 refined 2026-07-02 (kept 2026-07-01e prototype description as history); header bumped. |
| `plugins/scripting/.ai/current/80-known-gotchas.md` | G8 refined; new **G12** (REPL parser bug + delegate workaround); header bumped. |
| `plugins/scripting/.ai/current/70-tests.md` | Test row + acceptance-sentence updated for the refined design; header bumped. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry + JSR-223 workstream row updated. |

## Key Learnings

- **Kotlin's backtick-quoting really is as permissive as the user expected** — the earlier session's full-marker-encoding fix was solving the *wrong* problem for most characters. The actual blocker was a narrow, feature-specific REPL/script-snippet parser bug (G12), not a general Kotlin identifier-syntax limitation.
- **Delegated properties (`by ...`) are parsed through a completely different code path than accessor blocks** (`parsePropertyDelegateOrAssignment()` vs. the `mode.accessorsAllowed` accessor-component loop) — when an accessor-block parsing heuristic misfires in a specific context, switching to a delegate is a clean way to route around it entirely, not just a workaround for *this* character set.
- **A "seemed-suspicious" fix is sometimes exactly right for the wrong reason** — the previous marker-only design was correct that *something* needed to change, but its justification (an incorrectly broad claim about Kotlin identifier limits) was wrong; the real fix only needed to be narrower in scope once the actual trigger (G12) was understood.
- **Pre-existing unrelated test failures should be verified, not assumed** — stashing the two changed files and re-running the failing `kotlin-main-kts-test` suite confirmed those 10 failures are baseline/environment issues, not a regression from this change.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional `target/90-open-questions.md` Q14, `current/80-known-gotchas.md` G8).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: on budget — one targeted fix/verify build (`:kotlin-scripting-jsr223-test:test`), one rerun to rule out caching, one regression check (`:kotlin-main-kts-test:test`) + one baseline re-check (stash/pop) to rule out a false-positive regression signal.
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through — N/A: Q14 is a step-1 residual, not a whole step; step 1 stays "In progress" (Q15/Q16 design sign-off + `Jsr223BindingsConfigurator` extraction remain).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [x] `current/70-tests.md` updated (row + acceptance note + header).
- [x] `current/80-known-gotchas.md` G8 refined + new G12.
- [x] `target/90-open-questions.md` — Q14 refined.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
