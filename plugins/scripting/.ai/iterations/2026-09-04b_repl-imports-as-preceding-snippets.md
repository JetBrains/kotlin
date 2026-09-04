# REPL `@file:Import`s as non-isolated preceding snippets (G15 resolution) — 2026-09-04

## Overview

`K2ReplCompiler` compiles the `@file:Import`-ed scripts of a REPL snippet as `FirReplSnippet`s in the same FIR session (session-wide `isReplSnippetSource { _, _ -> true }`, kept by decision), but they were *isolated*: the REPL history was only populated in `updateResolved` at the end of body resolve, so sibling snippets resolved supertypes / implicit types against an empty history (`Unresolved reference 'sharedVar'`), and Fir2Ir tried to build `Fir2IrLazyClass` copies of same-module declarations. This iteration makes the imports behave as the snippets *preceding* the importing one ("Option A"): pre-registered in the history before resolution, strictly ordered, not consuming snippet numbers, converted as regular same-module IR, and evaluated (once each) from the importing snippet's `$$eval`.

## Workstream / Issue

G15 (`current/80-known-gotchas.md`) / migration-plan step 2 follow-up (KT-83498). Decision context: "Option C" (imports as `FirScript`, predicate narrowing) rejected because imported code must have the same access to the REPL state as snippet code; the user asked for Option A with no snippet numbers consumed and strict ordering.

## Changes

- `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetResolveExtensionImpl.kt` — new `FirReplHistoryProviderWithImports { putImportedSnippet }` + `FirReplHistoryProvider.putImportedSnippetOrSnippet` helper; `FirReplHistoryProviderImpl` implements it (imported snippets excluded from `getSnippetCount()`); `getSnippetScope` / `getImportsFromHistory` iterate only the history entries **preceding** the current snippet (`takeWhile { it != currentSnippet.symbol }`). Reason: visibility before body resolve, strict ordering, stable `res<N>` numbering. (The old `currentSnippet == snippet` check compared a `FirReplSnippet` with a `FirReplSnippetSymbol` — always false.)
- `.../services/ClasspathBackedFirReplHistoryProvider.kt` — implements `FirReplHistoryProviderWithImports`; `putSnippet` idempotent; `getSnippetCount()` excludes the live imported snippets.
- `.../impl/K2ReplCompiler.kt` — `compileImpl` registers the `FirReplSnippet` of each imported source (`newSources`, dependency order) via `putImportedSnippetOrSnippet` between raw-FIR building and `runResolution`.
- `.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt` — `prepareSnippet` drops the same-batch snippets (`declarationStorage.getCachedIrReplSnippet(fir) != null`) from the "from other snippet" lazy-copy machinery; computes `IrReplSnippet.importedSnippetsAttr` (transitive imports of the top-level snippet, dependency order, deduplicated; snippets that are themselves imported get none) from `resolvedImportScripts` of the refined configuration matched by source path against the history's same-batch snippets.
- `.../irLowerings/ScriptConfigurationAttributes.kt` — new `IrReplSnippet.importedSnippetsAttr`.
- `.../irLowerings/ReplSnippetLowering.kt` — `ReplSnippetToClassTransformer.visitMemberAccess`: a placeholder dispatch receiver on a member of another same-batch snippet class becomes `IrGetObjectValue(<that snippet object>)` (no `ReplState` lookup); new `chainImportedSnippetsEvaluation` pass inserts `Imported.INSTANCE.$$eval(<own implicit receivers>)` calls at the beginning of the importing snippet's `$$eval` (after all `$$eval` signatures are final).
- `libraries/tools/kotlin-main-kts-test/test/.../mainKtsJsr223Test.kt` — `testWithImport` un-muted.
- `plugins/scripting/scripting-tests/tests/.../repl/CustomK2ReplTest.kt` — new `testImportedScriptsAsPrecedingSnippets` (two-level imports, cross-import visibility, later-snippet access, `res<N>` numbering unaffected, single evaluation of a script imported both directly and transitively, strict ordering failure).
- Docs: `current/80-known-gotchas.md` G15 (resolved), `target/90-open-questions.md` Q2, `target/50-migration-plan.md` step 2 status, `target/10-compiler-target.md`, `AGENT_INSTRUCTIONS.md`, `ITERATION_RESULTS.md`, previous iteration entry cross-reference.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-main-kts-test:test --tests '*Jsr223*'` | 2 pass / 1 muted | 3 pass | `testWithImport` un-muted: `Hi from common`, `Hi from middle`, `5` |
| `:plugins:scripting:scripting-tests:test --tests '*Repl*'` (incl. `CustomK2ReplTest`, diagnostics/evaluation/codegen REPL goldens, `ReplSnippetRegularPipelineTest`) | 145 pass | 146 pass | +`testImportedScriptsAsPrecedingSnippets` |
| `:kotlin-scripting-jsr223-test:test` | 23 pass | 23 pass | |
| `:kotlin-scripting-jvm-host-test:test --tests '*ReplTest*'` | 18 pass | 18 pass | |
| Total of the run | — | 190 pass / 0 fail | |

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetResolveExtensionImpl.kt` | imports-aware history provider, strict ordering |
| `plugins/scripting/scripting-compiler/src/.../services/ClasspathBackedFirReplHistoryProvider.kt` | imports-aware, idempotent `putSnippet` |
| `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` | pre-register imported snippets before resolution |
| `plugins/scripting/scripting-compiler/src/.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt` | same-batch handling, `importedSnippetsAttr` |
| `plugins/scripting/scripting-compiler/src/.../irLowerings/ScriptConfigurationAttributes.kt` | new attribute |
| `plugins/scripting/scripting-compiler/src/.../irLowerings/ReplSnippetLowering.kt` | sibling receiver patch, `$$eval` chaining |
| `plugins/scripting/scripting-tests/tests/.../repl/CustomK2ReplTest.kt` | new test |
| `libraries/tools/kotlin-main-kts-test/test/.../mainKtsJsr223Test.kt` | un-muted |
| `plugins/scripting/.ai/*` | G15/Q2/step 2 status |

## Key Learnings

- Snippet isolation in one FIR session is an artefact of *when* `FirReplHistoryProvider.putSnippet` is called (end of body resolve), not a property of `FirReplSnippet`: raw snippets registered up front are perfectly usable through `FirReplHistoryScope` — implicit types are computed lazily by the regular return-type calculator since everything is in the same session.
- `getSnippetScope` excludes "the current snippet" by identity; since the history contains symbols and the parameter is the snippet, the old comparison never matched. With strict `takeWhile` ordering this also became the mechanism for "earlier imports don't see later ones".
- The cross-snippet Fir2Ir machinery (`createAndCacheEarlierSnippetClass`, `REPL_FROM_OTHER_SNIPPET` lazy copies, `ReplState` map lookups) is for declarations from *other modules*; same-module siblings already have real IR (all class headers are created before any file is visited), so the only thing needed is to patch the placeholder dispatch receiver emitted by `CallAndReferenceGenerator.putReceivers` (`originalReplSnippetSymbol != null`).
- `IrCallImpl(start, end, type, symbol)` sizes `arguments` from the callee's *current* parameter list — chain calls to sibling `$$eval`s only after every snippet's `evalFun.parameters` has been rewritten by `finalizeReplSnippetClass` (hence the separate second pass).
- A resolved-but-failed snippet still consumes a snippet number (`updateResolved` runs regardless of diagnostics) — pre-existing behaviour, visible in the new test as `res1`/`res2` after a failed `s0`.
- Known limitation carried forward: re-importing the same script from a later snippet recompiles/re-evaluates it as a fresh snippet (same class name, new classloader); a cross-snippet dedup by `uniqueLocationId` would need a state-level "already imported" registry and a way to reference the old class (the `ReplState` path) instead of the fresh object.

## Resources & Cost

_(not collected — ran under Junie; wall-clock ≈ 1 h, no subagents)_

| Metric | Value |
|---|---|
| Sessions aggregated | … |
| Time span | 2026-09-04 → 2026-09-04 |
| Cost (USD, model-aware) | … |
| Cache hit rate | … |
| Input tokens (non-cached) | … |
| Output tokens | … |
| Cache-creation tokens | … |
| Cache-read tokens | … |
| Model mix | … |
| Subagent calls (total) | 0 |
| Gradle wall-time (sum across suites) | ≈ 8 min |

### Subagent breakdown

  - none

### Loadout-vs-actual

- Loadout matrix row used: n/a (Junie session)
- Actual model: n/a
- Budget hit / over / under: n/a
- Subagent dispatch followed: n/a
- If "no" or "over": n/a

## Post-iteration checklist

- [x] Resources & Cost section populated (metrics unavailable under Junie — noted)
- [x] Migration-plan step status updated (step 2 "Not done" line: G15 struck through)
- [x] Active Workstreams updated in `AGENT_INSTRUCTIONS.md`
- [ ] `current/90-legacy-inventory.md` — no artifact deleted
- [ ] `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` — surface unchanged
- [x] Q2 note in `target/90-open-questions.md` updated
- [x] One-line index entry appended to `ITERATION_RESULTS.md`
