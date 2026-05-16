# Stateless K2 REPL — BTA daemon routing (Q5d daemon execution) — 2026-06-29

## Overview

Made `CompileReplSnippetOperation` honour `ExecutionPolicy.WithDaemon`: a stateless REPL snippet is now compiled **on the Kotlin compile daemon**, via the *regular* `CompileService.compile(...)` call (no REPL-specific RMI added to `CompileService`, which migration step 4 strips). The snippet rides a plain `-expression` compile switched into snippet mode by the scripting-plugin `-P` options landed in `2026-06-29b/c`; priors and the produced artifact are exchanged through files, and the daemon's compiler messages are captured back into the structured `ReplSnippetCompilationResult`. This is the last of the Q5d transport follow-ups (the op previously rejected everything but `InProcess`).

## Workstream / Issue

Migration step 3 (Stateless remote REPL compilation prototype) — Q5d (transport: **daemon execution**). Round 12.

## Changes

- `compiler/build-tools/kotlin-build-tools-impl/.../jvm/operations/CompileReplSnippetOperationImpl.kt` — replaced the `check(executionPolicy is InProcess)` guard with a branch: `InProcess` keeps the direct `K2ReplStatelessCompiler` path (extracted into `executeInProcess`), `WithDaemon` routes through a new `executeWithDaemon`. The daemon route: writes priors to temp files + a temp output file, builds raw CLI args (`-cp`, `-Xplugin=<services jar>`, `-expression <source>`, `-P plugin:kotlin.scripting:repl-snippet-mode/-name/-prior-artifact/-artifact-output`, `-Xsuppress-version-warnings`), connects via `KotlinCompilerRunnerUtils.newDaemonConnection` (honouring the `DaemonExecutionPolicyImpl` options — run-dir/logs/jvm-args/shutdown-delay, mirroring `BaseCompilationOperationImpl.compileWithDaemon`), runs `daemon.compile(...)`, and on `ExitCode.OK` + a written output file returns `Success(bytes, diagnostics)`, else `Failure(diagnostics)`. Threaded `buildIdToSessionFlagFile` into the op for daemon session reuse. Reason: literal daemon execution that obeys the settled "ride the regular compile path" direction.
  - New `createScriptingPluginServicesJar(...)` — synthesises a tiny `-Xplugin` jar declaring the **relocated** `ScriptingK2CompilerPluginRegistrar` + `ScriptingCommandLineProcessor` (package derived at runtime from the bundled `K2ReplStatelessCompiler`). Reason: the shaded `kotlin-build-tools-impl` jar deliberately strips the scripting plugin's `CompilerPluginRegistrar`/`CommandLineProcessor` service files, so the daemon's regular compiler can't auto-discover the plugin; the in-process path sidesteps this by calling `K2ReplStatelessCompiler` directly. `ServiceLoaderLite` reads the declaration from the `-Xplugin` jar and loads the class from the plugin classloader's parent (the compiler classpath, where the relocated class already lives — `PluginCliParser.createClassLoader` parents to the compiler classloader).
  - New private `ReplSnippetDiagnosticCollector : MessageCollector` — captures the daemon-reported compiler messages into `ReplSnippetDiagnostic`s (so the daemon path returns the same structured result as in-process) while forwarding them to the `KotlinLogger`.
- `compiler/build-tools/kotlin-build-tools-impl/.../jvm/JvmPlatformToolchainImpl.kt` — pass `buildIdToSessionFlagFile` to the op builder.
- `plugins/scripting/scripting-compiler-impl/.../configuration/ScriptingConfigurationKeys.kt` — new `REPL_SNIPPET_NAME` key. Reason: stateless reconstruction keys snippets by source name, but a `-expression` source is always named `script.kts`; an explicit name lets a multi-snippet daemon/CLI sequence keep distinct, stable names so priors resolve.
- `plugins/scripting/scripting-compiler/.../ScriptingCommandLineProcessor.kt` — new `repl-snippet-name` CLI option + `processOption` wiring.
- `plugins/scripting/scripting-compiler/.../AbstractScriptEvaluationExtension.kt` — `compileReplSnippet` re-wraps the snippet source with `REPL_SNIPPET_NAME` (when set & different) so the name survives the `-expression` path.
- `.../kotlin-build-tools-api-tests/.../ReplSnippetCompilationTest.kt` — removed the in-process-only `assumeInProcess`, so both `@DefaultStrategyAgnosticCompilationTest` variants (in-process **and** daemon) now run for the multi-snippet smoke test and the structured-failure test.
- `plugins/scripting/scripting-compiler/tests/.../ScriptingCompilerPluginTest.kt` — extended `testReplSnippetCompilationOptionsParsing` to lock the new `repl-snippet-name` option.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:...:kotlin-build-tools-api-tests:testCompilerPlugins --tests "*ReplSnippetCompilationTest"` | 2 pass (in-process) / 2 daemon-skipped | **4 pass / 0 fail** | the 2 daemon variants now run + pass (multi-snippet cross-snippet over the daemon; structured `Failure` with `noSuchSymbol`) |
| `:...:kotlin-build-tools-api-tests:testCompilerPlugins` (full) | BUILD SUCCESSFUL | **BUILD SUCCESSFUL** | no sibling regression |
| `:kotlin-scripting-compiler:test --tests "*ScriptingCompilerPluginTest"` | 6 pass | **7 pass** (parsing test extended) | validates `repl-snippet-name` + `compileReplSnippet` |
| `:plugins:scripting:scripting-tests:test --tests "*ReplStatelessDiagnosticsTestGenerated"` | 24 / 0 | 24 / 0 | regression guard |
| `:plugins:scripting:scripting-tests:test --tests "*ReplViaApiDiagnosticsTestGenerated"` | 24 / 0 | 24 / 0 | regression guard |

## Files Modified

| File | Change |
|---|---|
| `.../jvm/operations/CompileReplSnippetOperationImpl.kt` | +`WithDaemon` branch (`executeWithDaemon`, raw CLI args, `-Xplugin` services jar, capturing collector); in-process extracted; +`buildIdToSessionFlagFile` |
| `.../jvm/JvmPlatformToolchainImpl.kt` | pass `buildIdToSessionFlagFile` to the op |
| `.../configuration/ScriptingConfigurationKeys.kt` | +`REPL_SNIPPET_NAME` |
| `.../ScriptingCommandLineProcessor.kt` | +`repl-snippet-name` option + wiring |
| `.../AbstractScriptEvaluationExtension.kt` | `compileReplSnippet` consumes `REPL_SNIPPET_NAME` |
| `.../ReplSnippetCompilationTest.kt` | drop `assumeInProcess` → daemon variants run |
| `.../ScriptingCompilerPluginTest.kt` | parsing test covers `repl-snippet-name` |
| `.ai/iterations/2026-06-29d_stateless-repl-bta-daemon-routing.md` | this log |
| `.ai/ITERATION_RESULTS.md` | header + index entry |
| `.ai/target/90-open-questions.md` | Q5d daemon execution → landed |
| `.ai/target/50-migration-plan.md` | step 3 follow-up: daemon routing landed |
| `.ai/target/30-embedding-target.md` | daemon row: stateless snippet compile is real via regular compile path |

## Key Learnings

- The shaded `kotlin-build-tools-impl` jar **strips** the scripting plugin's `CompilerPluginRegistrar` + `CommandLineProcessor` `META-INF/services` files (`build.gradle.kts` `DontIncludeResourceTransformer`), so the daemon's regular compiler does **not** auto-discover the relocated scripting plugin. The in-process op never noticed because it calls `K2ReplStatelessCompiler` directly. The fix is a synthesized `-Xplugin` jar containing only those two service files (pointing to the relocated class names): `PluginCliParser.createClassLoader` parents the plugin classloader to the compiler classloader, and `ServiceLoaderLite.loadImplementations` reads the service declaration from the `-Xplugin` jar's own root while loading the impl class through the (parent-delegating) classloader — so the declaration and the class can live in different jars.
- The relocated package is derivable at runtime: `K2ReplStatelessCompiler::class.java.package.name.removeSuffix(".impl")` yields `<relocation-prefix>.compiler.plugin`, where the registrar/processor live. No need to hardcode the relocation prefix.
- `CompileService` has only path-based `CompilerId` (no content digest), but the daemon self-invalidates when a classpath jar's mtime is newer than the running daemon — so a rebuilt impl jar reliably spawns a fresh daemon (important when iterating on daemon-executed plugin code).
- `daemon.compile(...)` returns only an exit code; structured diagnostics must be reconstructed from the messages the daemon streams to the `CompilerServicesFacadeBase` — a custom `MessageCollector` handed to `BasicCompilerServicesWithResultsFacadeServer` captures them.
- Snippet *source name* must be distinct per snippet for stateless reconstruction; `-expression` always names the source `script.kts`, so the daemon path needs the explicit `repl-snippet-name` option to keep a multi-snippet sequence resolvable.

## Resources & Cost

Not collected this iteration (metrics script not run in this environment).

## Post-iteration checklist

- [x] Migration-plan step 3 follow-up updated (daemon routing landed)
- [ ] Migration-plan step strike-through — N/A (step 3 still in progress)
- [ ] Active Workstreams updated in `AGENT_INSTRUCTIONS.md` — N/A (workstream still in progress)
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted)
- [x] `target/30-embedding-target.md` updated (daemon row — stateless snippet compile is now a real daemon path)
- [x] Q5d in `target/90-open-questions.md` updated (daemon execution landed; remaining: in-process embedding)
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
