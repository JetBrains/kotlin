# Stateless K2 REPL — raw prototype (migration step 3 / Q5a + Q5b) — 2026-05-27

## Overview

First raw prototype of stateless K2 REPL compilation landed as an additive sibling of `K2ReplCompiler`. Snippet *N* compiles given only an ordered list of `SnippetArtifact`s (classfiles + JSON sidecar) for snippets 1..N-1 — no live `FirSession`, `K2ReplCompilationState`, or `FirReplHistoryProviderImpl` carried between calls. Three new tests in `:kotlin-scripting-compiler:test` pass; the two regression guards (`:kotlin-scripting-jvm-host-test:test`, `:plugins:scripting:scripting-tests:test`) are green. **Q5a closed** (reconstruction feasibility — happy-path proven), **Q5b** progresses to "JSON locked for prototype, protobuf-in-metadata planned for promotion".

## Workstream / Issue

Migration plan step **3** (Stateless remote REPL compilation prototype). Closes the empirical part of **Q5a** and locks **Q5b** at "paired JSON for prototype". Plan file: [`.junie/plans/k2-stateless-repl-prototype-step1.md`](../../../.junie/plans/k2-stateless-repl-prototype-step1.md) (steps 1–5 all marked `✓`).

## Changes

- `plugins/scripting/scripting-compiler/src/.../impl/SnippetArtifact.kt` — **new**. `internal data class SnippetArtifact(classFiles, sidecar: ByteArray)` + `SnippetArtifactSidecar` (versioned, with `MemberRef` / `ImportEntry` nested types) + hand-rolled `SnippetArtifactJsonCodec` (no new dependency; ~100 LOC). Reason: portable per-snippet representation; field set tuned to what `.kotlin_metadata` does not already carry (`isReplSnippetDeclaration` tagging, file-level imports, `replStateObjectFqName`, `isSynthetic`).
- `plugins/scripting/scripting-compiler/src/.../impl/SnippetArtifactEmission.kt` — **new**. `buildSnippetArtifactFromCompile(firSnippet, session, generationState, scriptCompilationConfiguration, hostConfiguration, historyIndex)` — collects classfiles from `generationState.factory.asList()` and synthesises the sidecar from the just-compiled `FirReplSnippet` (`snippetClass.declarations.filter { it.isReplSnippetDeclaration == true }` → `MemberRef`s; `firProvider.getFirReplSnippetContainerFile(firSnippet)?.imports` → `ImportEntry`s).
- `plugins/scripting/scripting-compiler/src/.../services/ArtifactBackedFirReplHistoryProvider.kt` — **new**. `internal class ArtifactBackedFirReplHistoryProvider(priorSnippets, sourceSession) : FirReplHistoryProvider()`. On first `getSnippets()` walks `priorSnippets`, looks up each wrapper class via `sourceSession().symbolProvider.getClassLikeSymbolByClassId(...)`, constructs a fresh `FirReplSnippetSymbol`, binds a stub `ReconstructedFirReplSnippet : FirReplSnippet()` to it (so `symbol.moduleData` works in the resolve extension), and tags deserialized declarations named in `sidecar.replSnippetDeclarations` with `isReplSnippetDeclaration = true` + `originalReplSnippetSymbol = reconstructedSymbol`. Gated debug logging via `System.getProperty("kotlin.scripting.repl.stateless.debug") == "true"` tagged `[STATELESS_REPL]`.
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplStatelessCompiler.kt` — **new**. Orchestrator: validates `stateObjectFqName` consistency across `priorSnippets` and against caller's `hostConfiguration` (hard-fail diagnostic on mismatch); writes prior `classFiles` into `Files.createTempDirectory("k2-repl-stateless-")` and feeds it via `JvmClasspathRoot`; overrides host configuration so `repl.firReplHistoryProvider` is the artifact-backed provider; installs two `K2ReplCompilationState` capture hooks (`sourceSessionReadyObserver` + `snippetCompilationObserver`); drives a fresh `K2ReplCompiler.compile(snippet)`; on success calls `buildSnippetArtifactFromCompile`; cleans up the temp dir in `finally`.
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — **two new internal `var` hooks on `K2ReplCompilationState`**:
  - `sourceSessionReadyObserver: ((FirSession) -> Unit)?` — fired in `compileImpl` right after `createSourceSession`, **before** `runResolution`, so the artifact-backed history provider can resolve `getClassLikeSymbolByClassId` against the live source session in time for `FirReplSnippetResolveExtensionImpl.getSnippetScope`.
  - `snippetCompilationObserver: ((FirReplSnippet, FirSession, GenerationState) -> Unit)?` — fired after a successful per-snippet compile, so the stateless path can capture the inputs needed by `buildSnippetArtifactFromCompile`. Reason: capture the just-compiled FIR snippet + generation state without restructuring the stateful `compileImpl` body. Both are `null` by default, so the stateful path is unaffected.
- `plugins/scripting/scripting-compiler/tests/.../K2ReplStatelessCompilerTest.kt` — **new**. Three `@Test` methods:
  - `testStatelessReplCompilesSnippetAgainstPriorArtifact` — stateless `val x = 42` → `SnippetArtifact1`; stateless `x + 1` against `[artifact1]` → `SnippetArtifact2` with `historyIndex == 1`, non-empty classfiles, wrapper class name encoding `s2`.
  - `testSidecarJsonRoundtrip` — encode → decode equality for a representative sidecar including all four `MemberRef.Kind` values and import aliases, both `isSynthetic` polarities.
  - `testStateObjectFqNameMismatchIsRejected` — assert `ResultWithDiagnostics.Failure` whose diagnostic message names both fqs.
- `.junie/plans/k2-stateless-repl-prototype-step1.md` — steps 1–5 all marked `✓` (current/in-progress markers cleared as each step landed).

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-compiler:test` (`*K2ReplStatelessCompilerTest*`) | 0 tests (new class) | 3 PASS / 0 FAIL | All three new tests green. |
| `:kotlin-scripting-compiler:test` (full) | green | green | No regressions. |
| `:kotlin-scripting-jvm-host-test:test` | green | green | Regression guard for `K2ReplCompiler` / `K2ReplEvaluator` public surface. |
| `:plugins:scripting:scripting-tests:test` | green | green | Regression guard for stateful path that shares helpers. |
| `:kotlin-scripting-jsr223-test:test` | not run | not run | Out of scope; no surface change touches the JSR-223 path. |
| `:kotlin-main-kts-test:test` | not run | not run | Out of scope; no surface change touches main-kts. |
| `:compiler:fir:fir2ir:test --tests "*FirScriptCodegenTestGenerated*"` | not run | not run | Out of scope; prototype is purely additive and lives in `plugins/scripting/scripting-compiler`. |

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/impl/SnippetArtifact.kt` | **new** — `SnippetArtifact`, `SnippetArtifactSidecar`, `SnippetArtifactJsonCodec`, `decodeSidecar` helper. |
| `plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/impl/SnippetArtifactEmission.kt` | **new** — `buildSnippetArtifactFromCompile`. |
| `plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/services/ArtifactBackedFirReplHistoryProvider.kt` | **new** — `ArtifactBackedFirReplHistoryProvider` + private `ReconstructedFirReplSnippet` stub + `findEvalSymbol` helper. |
| `plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/impl/K2ReplStatelessCompiler.kt` | **new** — orchestrator. |
| `plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/impl/K2ReplCompiler.kt` | Added `sourceSessionReadyObserver` + `snippetCompilationObserver` capture hooks on `K2ReplCompilationState`; invocation sites inside `compileImpl`. No behaviour change when hooks are `null` (always for the stateful path). |
| `plugins/scripting/scripting-compiler/tests/org/jetbrains/kotlin/scripting/compiler/test/K2ReplStatelessCompilerTest.kt` | **new** — three tests. |
| `.junie/plans/k2-stateless-repl-prototype-step1.md` | Step status markers flipped to `✓` as each step landed. |

## Key Learnings

- **`FirReplSnippetSymbol` requires a bound `FirReplSnippet`** even when the resolve-extension only walks `snippetClassSymbol.declarationSymbols`. Without a `symbol.bind(snippet)` call, `symbol.moduleData` throws "Fir is not initialized" the moment `FirReplSnippetResolveExtensionImpl.getImportsFromHistory` (or any other code reading `symbol.moduleData`) runs. Solution: a minimal `ReconstructedFirReplSnippet : FirReplSnippet()` stub bound to the synthesised symbol, with `moduleData`, `snippetClass`, `evalFunctionSymbol` populated and `source` deliberately set to throw if any future consumer reads it.
- **Two capture hooks beat one when the orchestrator needs both the live `FirSession` and the post-compile artifacts.** A naive single `snippetCompilationObserver` is too late — by then `runResolution` already happened and the artifact-backed history provider would have been queried with no source session available. Splitting capture into early (`sourceSessionReadyObserver`, fired immediately after `createSourceSession`) and late (`snippetCompilationObserver`, fired after codegen) keeps the stateful `compileImpl` untouched while giving the stateless path exactly the points it needs.
- **`originalReplSnippetSymbol` write to deserialized library declarations is session-local in practice.** The stateless API naturally enforces one `FirSession` per call, so the write is contained and does not need a per-session attribute store. Documented as an invariant on `ArtifactBackedFirReplHistoryProvider` rather than implemented in shared infrastructure.
- **`getFir2IrLazyClass` on a deserialized prior-snippet class works** without any special-casing — the happy-path test compiles `x + 1` against the snippet-1 wrapper class loaded purely from classfiles + sidecar, which exercises `Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet`'s `getFir2IrLazyClass(classSymbol.fir)` on the deserialized class. The cross-snippet `REPL_FROM_OTHER_SNIPPET` synthesised declarations get a usable `IrClass` parent. Closes Q5a empirically.
- **Gradle does not propagate `-Dsystem.property` to forked test JVMs by default**, so the instrumentation gate (`-Dkotlin.scripting.repl.stateless.debug=true`) does not produce log lines in a vanilla `./gradlew :kotlin-scripting-compiler:test -D...` run. The empirical Q5a answer is still implicit in the happy-path test passing (which fails the moment `getFir2IrLazyClass` returns an unusable parent). A future iteration that wants explicit log capture should configure `tasks.test.systemProperties` or use `Test.jvmArgumentProviders`.
- **Hand-rolled JSON codec (`SnippetArtifactJsonCodec`) at ~100 LOC is cheaper than adding kotlinx.serialization** to `:kotlin-scripting-compiler`'s dependency graph for a throwaway format. The roundtrip test catches any encoder/decoder drift; once the field set stabilises, the codec is the only thing that needs to be replaced when migrating to protobuf-in-metadata (step 3 of the original proposal).
- **Temp-dir classpath indirection for prior snippets is fine for the prototype.** `Files.createTempDirectory("k2-repl-stateless-")` + per-classfile write + `JvmClasspathRoot` is one fs write per prior snippet per call — trivial overhead — and lets the prototype reuse `ReplModuleDataProvider.addNewLibraryModuleDataIfNeeded` unchanged. An in-memory `VirtualFile` overlay is a planned refinement, not a blocker.

## Resources & Cost

Run `.claude/scripts/iter-metrics.sh` separately and fill the table below; metrics are not collected by the agent during this run.

| Metric | Value |
|---|---|
| Sessions aggregated | _(fill via iter-metrics.sh)_ |
| Time span | _(fill via iter-metrics.sh)_ |
| Cost (USD, model-aware) | _(fill via iter-metrics.sh)_ |
| Cache hit rate | _(fill via iter-metrics.sh)_ |
| Input tokens (non-cached) | _(fill via iter-metrics.sh)_ |
| Output tokens | _(fill via iter-metrics.sh)_ |
| Cache-creation tokens | _(fill via iter-metrics.sh)_ |
| Cache-read tokens | _(fill via iter-metrics.sh)_ |
| Model mix | _(fill via iter-metrics.sh)_ |
| Subagent calls (total) | 0 |
| Gradle wall-time (sum across suites) | ~3 min (`:kotlin-scripting-compiler:compileKotlin` + `compileTestKotlin` + targeted-test runs + 3 regression-suite reruns; no `:dist` rebuild required for the additive prototype) |

### Subagent breakdown

  - None used in this iteration. Per `cavecrew` decision-guide rules, in-line work was correct: tight investigate-edit-verify loop over ~5 new files in a single module.

### Loadout-vs-actual

- Loadout matrix row used: Migration-step execution (medium budget tier, Sonnet/Opus depending on session size).
- Actual model: claude-opus-4-7 (per session start banner).
- Budget hit / over / under: hit — additive prototype, no `:dist` rebuild required; one round of test-result triage when the first test run failed with `Fir is not initialized` (root cause traced and fixed inside one edit cycle by binding `ReconstructedFirReplSnippet` to the synthesised symbol). Within +/-30% of the medium-tier expectation.
- Subagent dispatch followed: yes — task size (5 new files in one module) did not justify investigator/builder delegation.
- No over-budget intervention needed.

## Post-iteration checklist

(See `AGENT_INSTRUCTIONS.md` "Post-iteration checklist" — confirm each.)

- [ ] Resources & Cost section populated (script run, Loadout-vs-actual filled) — _stub above; needs `iter-metrics.sh` run after submit._
- [x] Migration-plan step strike-through — step 3 status updated to "Prototype landed 2026-05-27".
- [x] `target/90-open-questions.md` — Q5a flipped to **resolved**, Q5b to **prototype-locked (JSON)**, Q5e cross-ref updated.
- [x] Active Workstreams updated — `ITERATION_RESULTS.md` workstream-state row for "Stateless remote REPL compilation prototype" flipped to "In progress (raw prototype landed 2026-05-27)".
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
- [ ] `current/30-api-layer.md` — no surface change (no public API added); left untouched.
- [ ] `current/70-tests.md` — no JSR-223 / scripting-tests matrix change; the new test class lives under `:kotlin-scripting-compiler:test` and is documented inline in this iteration entry rather than in the JSR-223 per-test matrix.
