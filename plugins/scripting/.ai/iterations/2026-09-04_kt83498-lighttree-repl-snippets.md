# KT-83498 / KT-77583 — LightTree `FirReplSnippet` builder + parser-agnostic `K2ReplCompiler` — 2026-09-04

## Overview

Land the missing LightTree half of K2 REPL: implement `LightTreeRawFirDeclarationBuilder.convertReplSnippet` (was `TODO("KT-77583")`), make `K2ReplCompiler` build raw FIR through an injectable `convertToFir` lambda (default LT, mirroring `ScriptJvmK2CompilerImpl`), and remove the last PSI touch (`scriptSource.psi as? KtScript`) from `FirReplSnippetConfiguratorExtensionImpl` by carrying the snippet id in the refined `ScriptCompilationConfiguration` (`repl.currentLineId`).

## Workstream / Issue

KT-83498 (migration-plan step 2), prerequisite KT-77583. Design confirmed with the user 2026-09-04:
1. parser seam = `convertToFir` lambda on `K2ReplCompiler` (PSI only by injection);
2. snippet id via `repl.currentLineId` in the refined configuration;
3. `isReplSnippetSource { _, _ -> true }` deliberately left as is (G15 stays open);
4. LT builder = 1:1 structural port of the PSI builder, shared helpers lifted into `AbstractRawFirBuilder`.

Stages: (A) design docs — this entry, step 2 rewrite, Q2/G15 refresh; (B) LT `convertReplSnippet`; (C) `*.repl.kts` raw-builder fixtures under LT; (D) `K2ReplCompiler` + configurator; (E) final docs.

## Changes

- `compiler/fir/raw-fir/raw-fir.common/src/.../AbstractRawFirBuilder.kt` — new `protected fun convertReplSnippetImpl(...)` (snippet `object` + `$$eval` + primary constructor, `convertReplElement` rewriting to `FirReplDeclarationReference`/`FirReplPropertyInitializer`/`FirReplPropertyDelegate`, `isReplSnippetDeclaration`, `replSnippetDelegatedPropertyCopies`), `createReplEvalFunction`, `convertReplElement`; `bindFunctionTarget` / `replSnippetDeclarationSymbol` hooks moved here from `PsiRawFirBuilder`. Reason: one implementation for both parsers so the FIR shape cannot drift.
- `compiler/fir/raw-fir/psi2fir/src/.../PsiRawFirBuilder.kt` — `Visitor.convertReplSnippet` delegates to `convertReplSnippetImpl` (`extractReplElements` + `buildOrLazyBlock` passed in); `createEvalFunction`/`convertReplElement` and the two hooks removed. Behaviour-neutral (raw-builder PSI suite unchanged).
- `compiler/fir/raw-fir/light-tree2fir/src/.../LightTreeRawFirDeclarationBuilder.kt` — `convertReplSnippet` implemented (`TODO("KT-77583")` gone) with an LT `extractReplElements` over `SCRIPT_DECLARATION_TOKENS`; `convertPropertyDeclaration` gained an explicit `ownerRegularOrAnonymousObjectSymbol` parameter so snippet-level properties (incl. delegated ones) are owned by the snippet class exactly like in PSI.
- `compiler/fir/raw-fir/light-tree2fir/testFixtures/.../TestGeneratorForLightTree2Fir.kt`, `AbstractLightTree2FirConverterTestCase.kt`, regenerated `tests-gen/.../LightTree2FirConverterTestCaseGenerated.java` — `*.repl.kts` fixtures no longer excluded; a no-op test `FirReplSnippetConfiguratorExtension` (`isReplSnippetsSource = true`) is registered on the empty session for them (LT analogue of `markAsReplSnippet()`).
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — `convertToFir` constructor parameter (default `SourceCode::convertToFirViaLightTree`); `compileImpl` converts every source through it, checks `diagnosticsReporter.hasErrors` right after parsing (incomplete-code heuristic: all errors are `SYNTAX` at end of snippet text), derives the snippet class FQ name from `FirReplSnippet.snippetClass`, pre-seeds `scriptRefinedCompilationConfigurationsCache` with the per-snippet refined configuration carrying `repl.currentLineId(LineId(priority, 0, hash))`. Deleted: `getScriptKtFile`, `markAsReplSnippet()`, `PRIORITY_KEY`, `KtFileScriptSource` partition, `AnalyzerWithCompilerReport.reportSyntaxErrors`.
- `plugins/scripting/scripting-compiler/src/.../impl/replConfigurationKeys.kt` (new) / `impl/KJvmReplCompilerBase.kt` — `ReplScriptCompilationConfigurationKeys.currentLineId` moved out of the K1 file (same package, import path unchanged).
- `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetConfiguratorExtensionImpl.kt` — result field name from `configuration[repl.currentLineId]?.no`; `KtScript`/`ScriptPriorities` imports gone.
- `compiler/cli/src/.../fir/FirDiagnosticsCompilerResultsReporter.kt` — `reportByFile` builds a position finder for `KtPsiSourceFile`s when they carry non-PSI diagnostics. Reason: a `KtFileScriptSource` converted by LT lost every diagnostic location (the test facade passes `KtFileScriptSource`), which silently dropped all REPL diagnostics in `ReplViaApiDiagnosticsTest`.
- `plugins/scripting/scripting-tests/tests/.../repl/CustomK2ReplTest.kt` — new `testConvertToFirSeamAndResultFieldNumbering` (spy converter sees every source; `res1`/`res3` numbering via the refined configuration).
- `libraries/tools/kotlin-main-kts-test/test/.../mainKtsJsr223Test.kt` — `testWithImport` `@Disabled` reason refreshed to the observed multi-snippet-per-session failure (G15).
- Docs: `target/50-migration-plan.md` step 2 (design + landed status), `target/00-principles.md`, `target/10-compiler-target.md`, `current/10-compiler-representation.md`, `current/80-known-gotchas.md` G15, `target/90-open-questions.md` Q2 (resolved), `AGENT_INSTRUCTIONS.md` (rule #5, status line, workstreams), `ITERATION_RESULTS.md`.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `LightTree2FirConverterTestCaseGenerated` | 188 (REPL excluded) | **198 / 0 fail** | 10 `*.repl.kts` now generated; golden parity with PSI `.txt`, no `.lt.txt` |
| `RawFirBuilderTestCaseGenerated` (PSI) | 198 | **198 / 0 fail** | PSI refactor behaviour-neutral |
| `TreesCompareTest` | 2 | 2 / 0 fail | |
| `:plugins:scripting:scripting-tests` `repl.*` | 139 | **140 / 0 fail** | `ReplViaApiDiagnostics` 24/24, `ReplWithTestExtensionsDiagnostics` 24/24, `ReplViaApiEvaluation` 36, `ReplWithTestExtensionsCodegen` 36, `CustomK2ReplTest` 20 (+1 new) |
| `ReplSnippetRegularPipelineTest` / `ScriptingCompilerPluginTest` | 5 / 1 | 5 / 1 | regular-pipeline snippet mode unaffected |
| `:kotlin-scripting-jsr223-test:test` (`KotlinJsr223ScriptEngineIT`) | 23/0/0 | **23/0/0** | `res<N>` numbering unchanged (engine's `currentLineId` was and is superseded by the snippet count — see TODO in `compileImpl`) |
| `:kotlin-scripting-jvm-host-test` `ReplTest` | 18 | 18 / 0 fail | |
| `:kotlin-main-kts-test` `MainKtsJsr223Test` | 2 pass + 1 muted | 2 pass + 1 muted | `testWithImport` re-muted with the G15 reason (see below) |
| AA `:analysis:low-level-api-fir` (`FirSourceLikeLazyDeclarationResolve`, `SourceLikeGetOrBuildFir`, `ContextCollectorScript`) | — | 1243 / 1 flake | `testJavaFunctionWithImplicitTypeAndAnnotationsAndSubstitutedType` failed in the parallel run, passes in isolation (`--rerun`) — Java-interop fixture, unrelated |
| `:examples:scripting-jsr223-daemon:test` | 13 | not run | Gradle refuses: "Tests are not cacheable in :examples:scripting-jsr223-daemon — apply `test-inputs-check`" (pre-existing build-infra issue) |

## Files Modified

| File | Change |
|---|---|
| `compiler/fir/raw-fir/raw-fir.common/src/.../AbstractRawFirBuilder.kt` | shared REPL snippet construction + AA hooks |
| `compiler/fir/raw-fir/psi2fir/src/.../PsiRawFirBuilder.kt` | delegate to shared impl |
| `compiler/fir/raw-fir/light-tree2fir/src/.../LightTreeRawFirDeclarationBuilder.kt` | `convertReplSnippet` + `extractReplElements`; owner symbol param on `convertPropertyDeclaration` |
| `compiler/fir/raw-fir/light-tree2fir/testFixtures/.../TestGeneratorForLightTree2Fir.kt`, `AbstractLightTree2FirConverterTestCase.kt`, `tests-gen/.../LightTree2FirConverterTestCaseGenerated.java` | REPL fixtures under LT |
| `compiler/cli/src/.../FirDiagnosticsCompilerResultsReporter.kt` | positions for non-PSI diagnostics on `KtPsiSourceFile` |
| `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` | parser-agnostic `compileImpl` |
| `plugins/scripting/scripting-compiler/src/.../impl/replConfigurationKeys.kt` (new), `impl/KJvmReplCompilerBase.kt` | `currentLineId` key relocated |
| `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetConfiguratorExtensionImpl.kt` | PSI-free snippet id |
| `plugins/scripting/scripting-tests/tests/.../repl/CustomK2ReplTest.kt` | seam + numbering test |
| `libraries/tools/kotlin-main-kts-test/test/.../mainKtsJsr223Test.kt` | refreshed `@Disabled` reason |
| `plugins/scripting/.ai/*` | design + landed status (see Changes) |

## Key Learnings

- The LT builder already decides script-vs-snippet correctly via the EP (`AbstractRawFirBuilder.isReplSnippet` → `replSnippetConfigurators`); only the `convertReplSnippet` body was missing. The PSI builder, by contrast, ignores the EP and trusts `KtScript.isReplSnippet` (KT-84387).
- `FirReplSnippetConfiguratorExtensionImpl.configure` falls into the *script* `resultField` branch whenever the snippet id is absent — wiring LT without moving the id into the configuration would have produced wrongly-named result fields silently, not a crash.
- In the K2 REPL the FIR-side services never saw the per-snippet configuration: `FirScriptDefinitionProviderService.getRefinedConfiguration` refined the *state-level* base configuration itself because nothing pre-seeded `scriptRefinedCompilationConfigurationsCache` (unlike `ScriptJvmK2CompilerImpl`). Pre-seeding with the `CliScriptConfigurationsProvider`-refined per-snippet configuration is what makes `repl.currentLineId` (and `resultFieldPrefix("")` for synthetic snippets) visible to the configurators.
- **Follow-up (same day)**: the REPL refinement was switched from `CliScriptConfigurationsProvider` (K1, PSI) to `refineAllForK2` + `collectAndResolveScriptAnnotationsViaFir` with a dedicated `<raw-snippet>` dummy session (module data excluded from the REPL history via `addNewSnippetModuleData(isDummy = true)`); imports are collected with `collectScriptsCompilationDependenciesRecursively` and analysed before the snippet. Guards after the switch: scripting-tests REPL suites 145/145, `KotlinJsr223ScriptEngineIT` 23/23, jvm-host `ReplTest` 18/18, `MainKtsJsr223Test` 2 + 1 muted (`testWithImport` now fails with `Unresolved reference 'sharedVar'` — imports are found but compiled as isolated snippets, G15).
- `FirDiagnosticsCompilerResultsReporter.reportByFile` assumed "`KtPsiSourceFile` ⇒ all diagnostics are PSI diagnostics" and returned `null` locations otherwise. Any LT-based compiler fed a `KtFileScriptSource` hit this: diagnostics were reported without `sourcePath` and test handlers that filter by file name dropped them all — 16 `ReplViaApiDiagnostics` fixtures "lost" their expected diagnostics with no error anywhere. Check for this when a diagnostics golden suddenly goes empty.
- Several `FirReplSnippet`s in one FIR session are not supported by the REPL resolution (`FirPrivateToThisAccessChecker` sees an unresolved status on a declaration of a sibling snippet). This is exactly what `isReplSnippetSource { _, _ -> true }` + imports produced at that point. **Superseded the same day** by [2026-09-04b](2026-09-04b_repl-imports-as-preceding-snippets.md): the isolation was an artefact of *when* the history is populated (`updateResolved` after body resolve), not of `FirReplSnippet` per se — imports are now pre-registered as preceding snippets and the predicate stays.
- `SequentialCloseablePositionFinder` requires an `InputStreamReader`; `psiFile.text.byteInputStream().reader()` avoids depending on the PSI file having a backing `VirtualFile`.

## Resources & Cost

_(not collected — this iteration ran under Junie, where `.claude/scripts/iter-metrics.sh` has no session data; wall-clock ≈ 1.5 h, no subagents)_

| Metric | Value |
|---|---|
| Sessions aggregated | … |
| Time span | 2026-09-04 → … |
| Cost (USD, model-aware) | … |
| Cache hit rate | … |
| Input tokens (non-cached) | … |
| Output tokens | … |
| Cache-creation tokens | … |
| Cache-read tokens | … |
| Model mix | … |
| Subagent calls (total) | 0 |
| Gradle wall-time (sum across suites) | … |

### Subagent breakdown

  - …

### Loadout-vs-actual

- Loadout matrix row used: Migration-step execution
- Actual model: …
- Budget hit / over / under: …
- Subagent dispatch followed: n/a (single agent, no delegation needed)

## Post-iteration checklist

- [ ] Resources & Cost section populated (script run, Loadout-vs-actual filled)
- [x] Migration-plan step strike-through (`### 2. ~~Land KT-83498 …~~ — landed 2026-09-04`)
- [x] Active Workstreams updated in `AGENT_INSTRUCTIONS.md` if workstream completed
- [x] `current/90-legacy-inventory.md` disposition rows updated for any deleted artifact (nothing deleted)
- [x] `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` updated if surface changed (no CLI/daemon surface change; test surface change recorded in `current/10-compiler-representation.md`)
- [x] Any resolved Q* in `target/90-open-questions.md` flipped to `resolved` with link here (Q2)
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
