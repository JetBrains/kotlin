# K1 REPL removal — daemon REPL, `-Xrepl`, `GenericReplCompiler`, `scripting-ide-services` — 2026-09-04

## Overview

Removed the legacy K1 REPL implementation from the compiler: the daemon REPL service, the `-Xrepl` CLI flag and shell seam, the `kotlin` launcher's REPL mode, the K1 REPL compiler core and terminal UI in `scripting-compiler`, the jvm-host legacy REPL wrappers, and the three `scripting-ide-services` modules. The daemon RMI interfaces are kept and now report an error instead of serving REPL.

## Workstream / Issue

Migration-plan steps 4, 5, 7, 8, 9; step 6 partially. Workstream: "K1 cleanup chain".

## Changes

- `compiler/daemon/src/.../KotlinRemoteReplService.kt`, `RemoteReplStateFacadeImpl.kt` — deleted. Reason: the daemon-side REPL implementation is the entry point step 4 removes.
- `compiler/daemon/src/.../CompileServiceImpl.kt` — the four REPL overrides return `CallResult.Error("REPL is not supported by the daemon anymore")`; `withValidRepl` / `withValidReplImpl` deleted; `getValidId` / `internalRng` moved here from the deleted service file (session leasing uses them). Reason: keep the RMI protocol intact while removing the implementation.
- `compiler/arguments/.../CommonCompilerArguments.kt` — `-Xrepl` gets `removedVersion = v2_5_0`, generating it into `RemovedCompilerArguments.kt`. Reason: the arguments framework's own removal mechanism, so the flag is rejected with the standard removed-argument diagnostic.
- `compiler/cli/cli-base/gen/.../CLIConfigurationKeys.kt` + `compiler/config/configuration-keys-generator/.../CLIConfigurationKeysContainer.kt` — `REPL_MODE` / `replMode` deleted. Reason: last reader went with the `-Xrepl` plumbing.
- `compiler/cli/cli-jvm/.../JvmScriptPipelinePhase.kt` — REPL branch replaced by `COMPILER_ARGUMENTS_ERROR "Arguments expected"`.
- `compiler/cli/cli-base/src/.../cli/common/extensions/{ShellExtension,ReplFactoryExtension}.kt` and their scripting impls `JvmCliReplShellExtension`, `JvmStandardReplFactoryExtension` — deleted, with the `ShellExtension` EP registration in `KotlinCoreEnvironment`.
- `compiler/cli/cli-runner/src/.../Main.kt`, `runners.kt` — `-repl` / `-Xrepl` option and `ReplRunner` deleted; a bare `kotlin` invocation now fails with "no command specified, see 'kotlin -help'" instead of starting a REPL.
- `compiler/cli/cli-base/src/.../cli/common/repl/` — reduced to `ReplApi.kt` + `ReplState.kt`, and those to the types the daemon RMI protocol needs; `LineId` moved into `ReplState.kt` from the deleted `BasicReplState.kt`.
- `plugins/scripting/scripting-compiler/src/.../plugin/` — `impl/KJvmReplCompilerBase.kt`, `impl/K1JvmIrCodegenFactory.kt` and the whole `repl/` package (incl. `JvmGeneratorExtensionsImpl.kt`) deleted; jline dropped from `build.gradle.kts`. No K1 script codegen remains in the plugin.
- `compiler/frontend/src/.../resolve/repl/ReplState.kt` + the `FileScopesCustomizer` / `KtFile.fileScopesCustomizer` hook in `resolve/lazy/FileScopeProvider.kt` (its only user), `compiler/util/src/.../utils/repl/ReplEscapeType.kt`, the `org.jetbrains.kotlin.resolve.repl` entry in `PackagesToDeprecate.txt` + its generated `package-info.java`, and `CLICompiler.SCRIPT_PLUGIN_REGISTRAR_NAME` with its `JvmFrontendPipelinePhase` filter — all deleted.
- `compiler/tests-integration/testData/repl/` — 50 K1 REPL fixtures deleted (`GenericReplTest.kt` / `ScriptGenTest.kt` were already absent from the tree).
- Kept: `ReplResultPropertyDescriptor` — `LazyScriptClassMemberScope` still exposes it as the script result field via `ScriptDescriptor.getResultValue()`; it goes with the K1 frontend bindings (step 11).
- `libraries/scripting/jvm-host/src/.../repl/legacyRepl{Compilation,Evaluation}.kt` — deleted.
- `plugins/scripting/scripting-ide-services{,-embeddable,-test}/` — deleted, with `settings.gradle.kts`, `kotlin-bom.pom`, `configureTestCaching.kt`, `testLifecycleTask.dump.txt` and `repo/artifacts-tests` reference poms.
- Tests: `libraries/scripting/jvm-host-test/test/.../ReplTest.kt`, the `@Disabled` `ResolveDependenciesTest.testReplResolveFunAndValFromClassloader`, `scripting-tests/.../repl/example/exampleRepl.kt` + the `runK2ExampleRepl` task, the daemon REPL cases in `CompilerDaemonTest`, and `compiler/testData/cli/jvm/replLaunchError.{args,out}` — deleted; `-Xrepl` lines dropped from the three `*ExtraHelp.out` fixtures; `CliTestGenerated` regenerated.
- `compiler/cli/cli-base/src/.../KotlinCoreEnvironment.kt` — restored the `CollectAdditionalSourcesExtension` / `ProcessSourcesBeforeCompilingExtension` / `ExtraImportsProviderExtension` EP registrations. Reason: they were removed together with `ShellExtension` but are not REPL-related; see Key Learnings.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jvm-host-test:test` | green (minus deleted `ReplTest`) | green | `ReplTest` was K1-only, deleted with the core |
| `:kotlin-scripting-jsr223-test:test` | green | green | |
| `:kotlin-main-kts-test:test` | green | green | |
| `:plugins:scripting:scripting-tests:test` | 10 fail | green | all 10 caused by the missing EP registrations |
| `:kotlin-scripting-compiler:test` | green | green | |
| `:examples:scripting-jsr223-daemon:test` | green | green | stateless K2 path, unaffected by the daemon REPL removal |
| `:compiler:tests-integration:test --tests "*CliTestGenerated*"` | 4 fail | green | the four `-Xrepl` fixtures |
| `:kotlin-daemon-tests:test` | 2 fail | green in isolation | `LastSessionDaemonTest` timing flakes under parallel load; leases via `leaseCompileSession`, not the REPL path |

## Files Modified

| File | Change |
|---|---|
| `compiler/daemon/src/.../CompileServiceImpl.kt` | REPL overrides return an error; repl helpers removed; id helpers moved in |
| `compiler/arguments/.../CommonCompilerArguments.kt` | `-Xrepl` marked removed in 2.5.0 |
| `compiler/cli/cli-jvm/.../JvmScriptPipelinePhase.kt`, `JvmConfigurationPipelinePhase.kt`, `JvmCliPipeline.kt` | REPL branches and `replMode` plumbing removed |
| `compiler/cli/src/.../AbstractConfigurationPhase.kt` | `replMode` assignment removed |
| `compiler/cli/cli-base/src/.../KotlinCoreEnvironment.kt` | `ShellExtension` EP registration removed; three scripting EPs restored |
| `compiler/cli/cli-base/src/.../cli/common/repl/{ReplApi,ReplState}.kt` | trimmed to the daemon protocol types; `LineId` rehomed |
| `compiler/cli/cli-runner/src/.../Main.kt`, `runners.kt` | launcher REPL mode removed |
| `plugins/scripting/scripting-compiler/{build.gradle.kts,.../pluginRegisrar.kt}` | jline dropped; REPL extension registration removed |
| `plugins/scripting/scripting-tests/build.gradle.kts` | `runK2ExampleRepl` task and its configuration removed |
| `settings.gradle.kts`, `libraries/tools/kotlin-bom/pom.xml`, `repo/...` | ide-services wiring removed |

## Key Learnings

- The four EP registrations under `// K1 extensions for scripting` in `KotlinCoreEnvironment` were removed as one block, but only `ShellExtension` is REPL-related. Dropping `CollectAdditionalSourcesExtension`, `ProcessSourcesBeforeCompilingExtension` and `ExtraImportsProviderExtension` silently disables `@file:Import` collection: the scripting registrar still registers the extensions, but with no extension point they never run. The symptom is remote from the cause — imported scripts resolve to a single file, and CLI-driven scripting tests fail on exit codes and golden output, with nothing pointing at extension registration.
- `-Xrepl` is retired by setting `removedVersion` in the arguments description rather than by deleting the entry: the generator then emits it into `RemovedCompilerArguments.kt`, and users get the standard removed-argument diagnostic instead of "unknown option".
- `cli-base/cli/common/repl/*` cannot be deleted while the daemon keeps its REPL RMI methods — `CompileService` and `ReplStateFacade` are typed in terms of `ReplCodeLine` / `ReplCheckResult` / `ReplCompileResult` / `ILineId`. Keeping the interfaces (the chosen end state) therefore pins a small protocol-only remnant of the package.
- `LastSessionDaemonTest` is timing-sensitive: it fails on a machine already running other Gradle daemons and passes in isolation. Check it alone before treating it as a regression.

## Resources & Cost

| Metric | Value |
|---|---|
| Sessions aggregated | n/a — Junie session, no JSONL |
| Time span | n/a |
| Cost (USD, model-aware) | n/a |
| Cache hit rate | n/a |
| Input tokens (non-cached) | n/a |
| Output tokens | n/a |
| Cache-creation tokens | n/a |
| Cache-read tokens | n/a |
| Model mix | n/a — Junie, session-fixed model |
| Subagent calls (total) | 3 |
| Gradle wall-time (sum across suites) | ~25 min |

### Subagent breakdown

  - `ide-services-remover` — deletion of the three `scripting-ide-services` modules and their build wiring.
  - `k1-repl-core-remover` — deletion of the K1 REPL core in `scripting-compiler` and its two test users.
  - `scripting-docs-updater` — `current/{40,45,70,90}` doc updates.

### Loadout-vs-actual

- Loadout matrix row used: Cross-module change (>3 files), ~10k budget.
- Actual model: Junie session-fixed.
- Budget hit / over / under: over — the change spans compiler CLI, daemon, scripting plugin, libraries and build files.
- Subagent dispatch followed: n/a under Junie (`cavecrew-*` unavailable); used `general_purpose` subagents with exclusive file scopes, and ran all Gradle from the main agent.

## Post-iteration checklist

- [x] Resources & Cost section populated (Loadout-vs-actual filled)
- [x] Migration-plan step strike-through
- [x] Active Workstreams updated in `AGENT_INSTRUCTIONS.md`
- [x] `current/90-legacy-inventory.md` disposition rows updated for any deleted artifact
- [x] `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` updated
- [ ] Any resolved Q* in `target/90-open-questions.md` flipped to `resolved` — none resolved by this pass
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
