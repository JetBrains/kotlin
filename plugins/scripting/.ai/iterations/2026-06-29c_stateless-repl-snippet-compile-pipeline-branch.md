# Stateless K2 REPL — snippet compile-pipeline branch (consumes the snippet params) (Q5d) — 2026-06-29

## Overview

Landed the **compile-pipeline branch** that *consumes* the stateless-REPL snippet parameters introduced last iteration (`2026-06-29b`). When `ScriptingConfigurationKeys.REPL_SNIPPET_COMPILATION_MODE` is set, the scripting plugin's regular compile entry (`ScriptEvaluationExtension` → `AbstractScriptEvaluationExtension.eval`) now branches into a new compile-only consumer instead of evaluating a script: it decodes the ordered prior-snippet artifacts named by `REPL_SNIPPET_PRIOR_ARTIFACTS`, drives `K2ReplStatelessCompiler`, and writes the produced `SnippetArtifact` (encoded with `SnippetArtifactCodec`) to `REPL_SNIPPET_ARTIFACT_OUTPUT`. No evaluation, no daemon REPL transport — the *same* invocation works from the CLI (`-expression`/`-script` + plugin `-P` options) and from a plain `CompileService.compile(...)` (the daemon forwards plugin args verbatim).

This completes the first of the Q5d daemon-execution follow-ups (the consumer half). BTA `compileWithDaemon` routing (so `ExecutionPolicy.WithDaemon` becomes a real path) and enabling the daemon-variant `ReplSnippetCompilationTest` cases remain open.

## Workstream / Issue

Migration step 3 (Stateless remote REPL compilation prototype) — Q5d (transport: daemon execution, consumer half). Round 11.

## Changes

- `plugins/scripting/scripting-compiler/src/.../AbstractScriptEvaluationExtension.kt`:
  - New top-level `internal fun compileReplSnippet(snippet: SourceCode, configuration: CompilerConfiguration): ExitCode` — the consumer. Reads the three snippet config keys, builds a per-call `ScriptCompilationConfiguration` whose classpath is `configuration.jvmClasspathRoots` (the CLI/daemon `-cp`), runs `internalScriptingRunSuspend { K2ReplStatelessCompiler().compile(priors, snippet, config, hostConfig) }`, reports every `ScriptDiagnostic` via `CompilerConfiguration.report`, and on a clean compile writes `SnippetArtifactCodec.encode(artifact)` to the output file. A clean compile returns `ExitCode.OK`; any error diagnostic returns `ExitCode.COMPILATION_ERROR` and writes **no** artifact (a written output always implies a clean snippet compile — best-effort artifacts are deliberately not persisted on this path).
  - Wired the branch into the private `eval(...)` right after the snippet `SourceCode` is resolved (before `createEnvironment`), guarded by `configuration.getBoolean(REPL_SNIPPET_COMPILATION_MODE)`. The stateless compiler builds its own isolated compilation context, so the script-eval environment/definition resolution is bypassed for snippet mode.
- `plugins/scripting/scripting-compiler/tests/.../ScriptingCompilerPluginTest.kt`:
  - `testReplSnippetCompilationPipelineBranch` — direct test of the consumer: snippet 1 (`val x = 42`) → `OK` + decodable artifact whose sidecar declares `x`; snippet 2 (`x + 1`) against snippet 1 → `OK` (proves the prior artifact is consumed and cross-snippet resolution works), `historyIndex == 1`; error path (unresolved reference) → `COMPILATION_ERROR` and no artifact written.
  - `testReplSnippetCompilationViaCli` — end-to-end through the real `K2JVMCompiler().exec(...)` with `-expression "val x = 42"` + `-P plugin:kotlin.scripting:repl-snippet-mode=true` + `repl-snippet-artifact-output=…`. Proves the `eval` → `compileReplSnippet` wiring, `-P` parsing (the plugin is auto-discovered from the classpath), classpath threading, and artifact production all work through the regular compiler entry.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-compiler:test --tests "*ScriptingCompilerPluginTest"` | 4 pass / 0 fail | **6 pass / 0 fail** | +2 new tests (consumer + CLI) |
| `:plugins:scripting:scripting-tests:test --tests "*ReplStatelessDiagnosticsTestGenerated"` | 24 / 0 | 24 / 0 | regression guard (recompiled against the production change) |
| `:plugins:scripting:scripting-tests:test --tests "*ReplViaApiDiagnosticsTestGenerated"` | 24 / 0 | 24 / 0 | regression guard |

## Files Modified

| File | Change |
|---|---|
| `.../AbstractScriptEvaluationExtension.kt` | +`compileReplSnippet` consumer; +snippet-mode branch in `eval` |
| `.../ScriptingCompilerPluginTest.kt` | +2 tests (direct consumer + end-to-end CLI) |
| `.ai/iterations/2026-06-29c_stateless-repl-snippet-compile-pipeline-branch.md` | this log |
| `.ai/ITERATION_RESULTS.md` | header + index entry |
| `.ai/target/50-migration-plan.md` | step 3 follow-up: consumer landed |
| `.ai/target/90-open-questions.md` | Q5d: consumer landed; remaining follow-ups trimmed |

## Key Learnings

- The Kotlin compiler decides script-vs-regular pipeline (`JvmCliPipeline.scriptingModeEnabled`) from `K2JVMCompilerArguments` *before* plugin options are parsed, so the snippet-mode flag (a plugin `-P` option) cannot itself flip the pipeline. The reachable seam is therefore the existing `ScriptEvaluationExtension` path (entered via `-script`/`-expression`), where the plugin already takes over compilation; snippet mode is a compile-only branch inside it. This is consistent with "rides the regular compile entry, switched by plugin params" without re-adding any REPL-specific transport.
- The snippet *source name* matters for stateless reconstruction: two snippets with the same synthetic name collide (`-expression` always names the source `script.kts`, so a two-`-expression` sequence fails to resolve the prior). Distinct names (as in the direct test, and as a real file-per-snippet caller would use) work. The end-to-end CLI test is therefore a single-snippet proof; cross-snippet prior consumption is proven by the direct test with distinct names.
- `K2JVMCompiler().exec` auto-discovers the scripting plugin's `CompilerPluginRegistrar` **and** `CommandLineProcessor` from the classpath even without `-Xplugin`, so `-P plugin:kotlin.scripting:…` options are honoured in-process.

## Resources & Cost

Not collected this iteration (metrics script not run in this environment).

## Post-iteration checklist

- [x] Migration-plan step 3 follow-up updated (consumer branch landed)
- [ ] Migration-plan step strike-through — N/A (step 3 still in progress)
- [ ] Active Workstreams updated in `AGENT_INSTRUCTIONS.md` — N/A (workstream still in progress)
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted)
- [ ] `target/30-embedding-target.md` — N/A (no new daemon/BTA surface change; consumer rides the already-documented regular-compile path)
- [x] Q5d in `target/90-open-questions.md` updated with the consumer landing + remaining follow-ups
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
