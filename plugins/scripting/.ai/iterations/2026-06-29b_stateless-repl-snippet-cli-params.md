# Stateless K2 REPL — daemon execution direction + snippet compilation parameter surface (Q5d) — 2026-06-29

## Overview

Settled the "BTA daemon execution" direction for the stateless K2 REPL and landed its first increment. Daemon execution will **not** add a REPL method to `CompileService` (that contradicts non-negotiable rule #3 and migration step 4, which deletes the legacy daemon REPL surface). Instead, a snippet is compiled on the **regular compilation path** — like an ordinary source — switched into snippet mode by new scripting-plugin parameters that feed prior-snippet state and name the output artifact. Because they are plain plugin args, the *same* invocation works from the CLI **and** from a regular `CompileService.compile(...)` call (the daemon forwards plugin args verbatim), with no daemon-protocol change. This iteration lands the parameter surface (CLI options + config keys + parser + test).

## Workstream / Issue

Migration step 3 (Stateless remote REPL compilation prototype) — Q5d (transport: daemon execution). Round 10.

## Changes

- `plugins/scripting/scripting-compiler-impl/src/.../configuration/ScriptingConfigurationKeys.kt` — added `REPL_SNIPPET_COMPILATION_MODE: Boolean`, `REPL_SNIPPET_PRIOR_ARTIFACTS: List<File>`, `REPL_SNIPPET_ARTIFACT_OUTPUT: File`. Reason: the parameter contract that switches a regular compile into stateless snippet mode and feeds/receives artifacts.
- `plugins/scripting/scripting-compiler/src/.../ScriptingCommandLineProcessor.kt` — added CLI options `repl-snippet-mode` (true/false), `repl-snippet-prior-artifact` (repeatable, snippet order), `repl-snippet-artifact-output`, registered them in `pluginOptions`, and wired `processOption` to populate the new config keys. Reason: same params must be reachable via `kotlinc` plugin args and via the daemon (which forwards plugin args verbatim).
- `plugins/scripting/scripting-compiler/tests/.../ScriptingCompilerPluginTest.kt` — added `testReplSnippetCompilationOptionsParsing` driving `processOption` for the three options and asserting the resulting config-key values (including ordered repeatable priors). Reason: lock the parsing contract.
- `plugins/scripting/.ai/target/90-open-questions.md` — Q5d: recorded the settled daemon-execution direction + landed surface + remaining follow-ups.
- `plugins/scripting/.ai/target/50-migration-plan.md` — step 3 follow-ups: added the daemon-execution decision bullet; updated the "Last verified" header.
- `plugins/scripting/.ai/target/30-embedding-target.md` — Daemon section note + BTA row updated to state stateless snippet compile rides the regular compile path (no daemon REPL RMI); bumped "Last verified".

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-compiler:test --tests "*ScriptingCompilerPluginTest.testReplSnippetCompilationOptionsParsing"` | n/a (new) | 1 pass / 0 fail | new parsing test |
| `:plugins:scripting:scripting-tests:test --tests "*ReplStatelessDiagnosticsTestGenerated"` | 24 pass / 0 fail | 24 pass / 0 fail | regression guard (recompiled against the production change) |
| `:plugins:scripting:scripting-tests:test --tests "*ReplViaApiDiagnosticsTestGenerated"` | 24 pass / 0 fail | 24 pass / 0 fail | regression guard |

## Files Modified

| File | Change |
|---|---|
| `.../configuration/ScriptingConfigurationKeys.kt` | +3 snippet config keys |
| `.../ScriptingCommandLineProcessor.kt` | +3 CLI options + parsing |
| `.../ScriptingCompilerPluginTest.kt` | +1 parsing test |
| `.ai/target/90-open-questions.md` | Q5d decision + follow-ups |
| `.ai/target/50-migration-plan.md` | step 3 daemon-decision bullet |
| `.ai/target/30-embedding-target.md` | daemon/BTA rows + dates |

## Key Learnings

- The Kotlin compile daemon's `CompileService` has **no generic operation channel** — only the CLI-args `compile()` (exit-code, file-based) and the legacy REPL methods that step 4 deletes. Riding compiler-plugin options (forwarded verbatim by the daemon) is therefore the only way to honour `ExecutionPolicy.WithDaemon` for a snippet compile *without* a new daemon RMI method.
- `K2ReplStatelessCompiler` already accepts everything snippet mode needs (prior `SnippetArtifact`s, host config, message collector); the missing piece for the regular-compile path is purely a *consumer* that reads these new config keys, drives it, and writes the produced artifact — deliberately deferred (see follow-ups).
- The additive option surface does not touch any existing compile path: both diagnostics corpora recompiled against the production change and stayed 24/24.

## Resources & Cost

Not collected this iteration (metrics script not run in this environment).

## Post-iteration checklist

- [x] Migration-plan step 3 follow-up updated with the daemon-execution decision
- [ ] Migration-plan step strike-through — N/A (step 3 still in progress)
- [ ] Active Workstreams updated in `AGENT_INSTRUCTIONS.md` — N/A (workstream still in progress)
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted)
- [x] `target/30-embedding-target.md` updated (daemon/BTA surface clarified)
- [x] Q5d in `target/90-open-questions.md` updated with the decision + follow-ups
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
