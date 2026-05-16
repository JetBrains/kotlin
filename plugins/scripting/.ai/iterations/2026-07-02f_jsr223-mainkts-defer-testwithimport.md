# JSR-223 K2 bindings — defer `testWithImport`, record the `isReplSnippetSource` finding, mute the test — 2026-07-02f

## Overview

The previous session's investigation (2026-07-02) root-caused `MainKtsJsr223Test.testWithImport`'s failure — it hits `TODO("KT-77583")` in `LightTreeRawFirDeclarationBuilder.convertReplSnippet` not because light-tree REPL-snippet support is fundamentally missing for the imported scripts themselves, but because `K2ReplCompiler`'s session-wide `isReplSnippetSource { _, _ -> true }` predicate misclassifies the light-tree-compiled `@file:Import(...)`ed scripts as REPL snippets, routing them into the unimplemented branch instead of the already-working `convertScript` path. This session's task: **ignore this issue for now** — record the finding in the docs, and mute (`@Ignore`) the test instead of leaving it as an unexplained failure or attempting the fix.

## Workstream / Issue

JSR-223 K2 bindings (Option D), migration-plan step 1 / Q2 (`KT-83498`, LightTree path for `FirReplSnippet`) — a deferred-by-decision follow-up, not a fix.

## Decision

No code fix attempted. Per explicit user direction, this is set aside for now:

1. `libraries/tools/kotlin-main-kts-test/test/org/jetbrains/kotlin/mainKts/test/mainKtsJsr223Test.kt`: `testWithImport` annotated `@Ignore(...)` with an inline message summarizing the root cause (`isReplSnippetSource` misclassification, not a fundamental light-tree gap) and pointers to `Q2` / `G15`.
2. `plugins/scripting/.ai/current/80-known-gotchas.md`: new **G15** — `K2ReplCompiler`'s session-wide `isReplSnippetSource { _, _ -> true }` misclassifies light-tree-compiled REPL imports as snippets; explicitly marked "open, deferred by decision" so it isn't rediscovered later as "just `KT-83498` is missing" without checking whether narrowing the predicate alone would suffice.
3. `plugins/scripting/.ai/target/90-open-questions.md`: `Q2` gets a new paragraph recording the same finding + the explicit "deferred/ignored for now" decision, cross-linking G15.

## Verification

- Doc-only + one-line test-annotation change; no production code touched. No build/test run needed for this change itself (test-annotation change is trivially verifiable by inspection: `@Ignore` compiles and JUnit4 honors it, same pattern already used by `MainKtsIT`/`MainKtsTest` in the same module).
- Confirmed (from the immediately preceding session) that `testWithImport` is otherwise the *only* remaining failure across the full JSR-223/main-kts test surface: `KotlinJsr223ScriptEngineIT` 23/0/0, `MainKtsTest` 23/0/1-skip, `CacheDirectoryDetectorTest` 9/0/0, `MainKtsIT` 16/16/0.

## Key Learnings

- Sometimes the right move after a root-cause investigation is *not* to implement the identified fix — recording the finding precisely (so it isn't rediscovered from scratch) and explicitly deferring is a valid, and sometimes preferred, outcome. The gotcha entry's "Status: open, deferred by decision" phrasing (rather than just "open") is meant to prevent a future session from either (a) re-investigating from zero, or (b) assuming "deferred" means "still mysterious".
- Muting via `@Ignore` with an inline explanatory message (rather than a bare `@Ignore`) keeps the reason discoverable directly at the test, without requiring a jump to the docs — mirrors the `jsr223-test` module's convention of tagging disabled tests with a `BLOCKED-DESIGN-Qn` string in the disable reason.
