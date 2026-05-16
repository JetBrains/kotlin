# JSR-223 K2 bindings — MainKtsJsr223Test: bypass classpath-based rediscovery in K2ReplCompiler — 2026-07-02d

## Overview

Follow-up to the 2026-07-02c Q16 iteration, which flagged (but did not fix) an independent `MainKtsJsr223Test` failure: all 3 tests failed with `Unresolved reference 'getBindings'` on the synthetic bindings snippet, for the `kotlin-main-kts-test` module only (the generic `jsr223-test` module suite was green). The prior investigation hypothesized the cause was classpath-based script-definition rediscovery (`ScriptDefinitionsFromClasspathDiscoverySource`) bypassing the JSR-223 host's `hostConfiguration` override. This iteration investigated with live instrumentation, found the *actual* root cause was different (and deeper) than that hypothesis, and implemented a fix.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. This is the "known follow-up" documented at the end of `target/90-open-questions.md` Q16.

## Investigation

Added temporary debug logging (removed before landing) at each layer of the configuration-lookup chain (`configureExposedJsr223Context`, `FirScriptDefinitionProviderService.getBaseConfiguration`/`getRefinedConfiguration`/`hostConfiguration`, `FirReplSnippetConfiguratorExtensionImpl.configure`, `ScriptDefinitionsFromClasspathDiscoverySource.loadScriptDefinition`) and ran `MainKtsJsr223Test.testSimpleEval` repeatedly to trace the actual call path.

Key findings, in order of discovery:

1. Classpath discovery **does** load `MainKtsScript` via `ScriptDefinitionsFromClasspathDiscoverySource` (confirmed by log). However, a stack trace captured at `configureExposedJsr223Context` showed a **separate** call path (`K2ReplCompiler.compileImpl` → `collectScriptsCompilationDependencies` → `CliScriptConfigurationsProvider.calculateRefinedConfiguration` → `refineScriptCompilationConfiguration` → `refineBeforeCompiling` → `MainKtsScriptDefinition`'s own `beforeCompiling` callback) where `getScriptContext` **did** resolve non-null — this call is used only for computing classpath dependencies and its result is discarded, not shared with FIR.
2. The actual FIR-side lookup (`FirReplSnippetConfiguratorExtensionImpl.configure` → `getOrLoadConfiguration` → `session.scriptDefinitionProviderService.getRefinedConfiguration`) resolved to a **different** configuration object (confirmed by identity comparison) with `implicitReceivers == null` — i.e. `configureExposedJsr223Context`'s early-return (no-`getScriptContext`) branch had fired for *this* configuration.
3. `FirScriptDefinitionProviderService.hostConfiguration` getter is `session.scriptCompilationComponent?.hostConfiguration ?: defaultHostConfiguration` — logging showed `fromComponent=false` for every call, i.e. `session.scriptCompilationComponent` (a `FirScriptCompilationComponent`) was **never registered** for `K2ReplCompiler`'s per-snippet FIR sessions.
4. `defaultHostConfiguration` comes from `FirScriptDefinitionProviderService.getFactory(compilerConfiguration)`'s fallback branch, which — only if `compilerConfiguration.scriptingHostConfiguration` was never set — builds a **fresh**, independent `defaultJvmScriptingHostConfiguration.with { configureScriptDefinitions(...) }`, re-triggering classpath discovery and populating its own `CliScriptDefinitionProvider` from whatever `SCRIPT_DEFINITIONS`/`SCRIPT_DEFINITIONS_SOURCES` happen to be registered on the compiler configuration at that point — **completely disconnected** from the REPL session's own JSR-223-wired `hostConfiguration`/`scriptCompilationConfiguration`.
5. `K2ReplCompiler.createCompilationState` (`plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt`) never sets `compilerConfiguration.scriptingHostConfiguration` at all — unlike `K2ScriptingCompilerEnvironment.createCompilerState` (a *different*, parallel bootstrap function that is **not actually called** by `K2ReplCompiler`) and unlike the one-shot `ScriptJvmK2CompilerImpl.compileImpl`, which explicitly registers `session.register(FirScriptCompilationComponent::class, FirScriptCompilationComponent(state.hostConfiguration, ...))` for its per-script session.
6. Added a debug marker directly inside the compile-time `ScriptDefinition.isScript` override used by `K2ScriptingCompilerEnvironment.createCompilerState`'s registered definition — it was **never called** during a `MainKtsJsr223Test` run, confirming that code path is genuinely dead for `K2ReplCompiler` sessions.
7. Independently important: even a definition that *is* registered and matched by extension (`ScriptDefinition.FromConfigurations`'s `isScript` checks `script.locationId`/`script.name` against `.$fileExtension`) would not match a synthetic snippet source named `...repl.kts` when the template's `fileExtension` is `main.kts` — this is why the generic `jsr223-test` suite (template `fileExtension = "kts"`, which `.repl.kts` *does* end with) was unaffected while `MainKtsScript` (`fileExtension = "main.kts"`) was.

Net conclusion: the failure was **not** primarily about classpath rediscovery per se — it was that `K2ReplCompiler`'s per-snippet FIR session had **no** functioning `ScriptCompilationConfigurationProvider` registered against its own `hostConfiguration` at all, so every `getBaseConfiguration` call fell through several layers (missing session component → freshly-rebuilt default host configuration → its own classpath discovery + registered-but-extension-mismatched definitions → the generic `ScriptTemplateWithArgs`-based default) to a configuration carrying zero JSR-223 wiring.

## Fix

- `K2ScriptingCompilerEnvironment.kt`: extracted `ReplSessionScriptDefinition` (`internal class`, extends `ScriptDefinition.FromConfigurations`) whose `isScript` always returns `true` — safe because a script-definition provider built for one REPL/host session only ever needs to resolve sources belonging to that one session's own configuration; there's no other candidate to disambiguate against. `createCompilerState` now uses it instead of the plain `ScriptDefinition.FromConfigurations`.
- `K2ReplCompiler.kt`: `createCompilationState` now builds and registers its own `CliScriptDefinitionProvider`/`ScriptCompilationConfigurationProviderOverDefinitionProvider`/`ScriptRefinedCompilationConfigurationCacheImpl` (mirroring the existing pattern in `K2ScriptingCompilerEnvironment.createCompilerState`), registering a `ReplSessionScriptDefinition` for this session's own `hostConfiguration`/`scriptCompilationConfiguration`, and sets `compilerConfiguration.scriptingHostConfiguration` to the resulting wrapped host configuration. `K2ReplCompilationState` now stores this wrapped host configuration (instead of the raw input parameter) so all downstream per-snippet FIR-session lookups resolve it directly instead of falling back to classpath-based rediscovery.

No FIR/IR/evaluator changes were needed.

## Test Results

| Suite | Before | After |
|---|---|---|
| `MainKtsJsr223Test` (`kotlin-main-kts-test`) | 0/3 pass | **2/3 pass** (`testSimpleEval`, `testWithDirectBindings`); `testWithImport` fails on unrelated `TODO("KT-77583")` (light-tree REPL-snippet support not implemented — migration step 2 / KT-83498) |
| `KotlinJsr223ScriptEngineIT` (`jsr223-test`) | 23/0/0 | **23/0/0** (unaffected) |
| `MainKtsTest` (`kotlin-main-kts-test`) | 23/0/1-skip | **23/0/1-skip** (unaffected) |
| `CacheDirectoryDetectorTest` | 9/0/0 | **9/0/0** (unaffected) |
| `MainKtsIT` | 14/2-fail (pre-existing, unrelated) | **14/2-fail** (same 2 pre-existing failures: `testCachedReflection`, `testCacheWithFileLocation`) |
| `CustomK2ReplTest` (`kotlin-scripting-compiler`) | not previously re-checked this session | **19/0/0** (regression check for the shared `K2ReplCompiler.createCompilationState` code path) |

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/scripting-compiler/src/.../impl/K2ScriptingCompilerEnvironment.kt` | New `internal class ReplSessionScriptDefinition` (always-matching `isScript`); `createCompilerState` uses it. |
| `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` | `createCompilationState` now registers its own `CliScriptDefinitionProvider`/`scriptCompilationConfigurationProvider`/cache against `compilerConfiguration.scriptingHostConfiguration`, using `ReplSessionScriptDefinition`; `K2ReplCompilationState` now stores the wrapped host configuration. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q16's "known follow-up" note updated to "resolved 2026-07-02d" with root cause + fix summary. |
| `plugins/scripting/.ai/current/80-known-gotchas.md` | New **G13** entry. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Addendum + index entry appended. |

## Key Learnings

- **A production stack trace beats a hypothesis.** The prior iteration's classpath-rediscovery hypothesis was plausible and partially correct (rediscovery does happen), but live tracing showed the actual defect was a missing session-component registration several layers up the call chain — the rediscovered definition was never even reachable for the failing source (extension mismatch), so "fixing" rediscovery alone would not have helped.
- **Two structurally similar bootstrap functions silently diverged.** `K2ScriptingCompilerEnvironment.createCompilerState` already had the correct wiring pattern (`CliScriptDefinitionProvider` + `scriptCompilationConfigurationProvider` + `compilerConfiguration.scriptingHostConfiguration`), but `K2ReplCompiler.createCompilationState` — the function actually used by JSR-223/REPL — never called it and never replicated the wiring. Comparing against the working one-shot compiler (`ScriptJvmK2CompilerImpl`, which *does* register a `FirScriptCompilationComponent`) was what pointed at the actual gap.
- **Extension-based script-definition matching is fundamentally unsound for REPL synthetic sources.** A REPL session's snippet source names follow a fixed internal convention (`...repl.kts`) unrelated to the user-facing template's file extension (`main.kts`, etc.) — matching by extension can silently miss the one-and-only definition that should always apply.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

## Post-iteration checklist

- [x] Migration-plan step strike-through — N/A: this is a step-1 residual fix, not a whole step; step 1 stays "In progress" (Q15 typed-lambda-access design + `Jsr223BindingsConfigurator` extraction remain, plus the newly-exposed `testWithImport` / KT-77583 gap which belongs to migration step 2).
- [x] `current/80-known-gotchas.md` — new G13 added.
- [x] `target/90-open-questions.md` — Q16 follow-up note updated to resolved.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
