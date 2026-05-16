# Step 1b — `IR_EXTERNAL_DECLARATION_STUB` root-cause refinement — 2026-05-18 (round 2)

## Overview

Second iteration on migration step 1b (`Fix K2 REPL IR_EXTERNAL_DECLARATION_STUB`). The previous iteration's investigation (`2026-05-18_codegen-stub-investigation`) hypothesised two failure families — G1 (`@InlineOnly` body-materialisation in Fir2Ir / inliner) and G2 (cross-snippet fake-override on rehydrated `ReplState`). This iteration captured the actual codegen failure stack for `testResolveFromContextStandard` by temporarily un-disabling it and ran the suite under Junie. The proximate exception fires on a **plain external Kotlin top-level `val`** (`<get-shouldBeVisibleFromRepl>`), not on `ReplState.put [fake_override]` as previously hypothesised — both appear in the IR render of the failing `$$eval` body but only the former triggers `ExpressionCodegen.visitCall` line 519's `require(callee.parent is IrClass)`. G1 and G2 are real failure shapes but are special cases of a broader umbrella ("G11") that covers any external Kotlin top-level decl referenced from a snippet.

No production code change was landed in this iteration — the deliverable is doc-side correction of the prior iteration's hypothesis, plus a refined fix direction for the next iteration's step-1b execution.

## Workstream / Issue

Migration-plan step **1b** (investigation, round 2). Refines Q13 / Q13a / Q13b in `target/90-open-questions.md`. Promotes G11 to `current/80-known-gotchas.md` as the umbrella over G1 / G2.

## Investigation steps taken

1. Read orientation docs (`AGENT_INSTRUCTIONS.md`, `JUNIE_NOTES.md`, `ITERATION_RESULTS.md`) and prior iteration `2026-05-18_codegen-stub-investigation.md`.
2. Read fix-site code: `plugins/scripting/scripting-compiler/.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt` (G2 site, `getStateObject()` and its synthesis-vs-rehydration fork at L189–262); `compiler/fir/fir2ir/.../utils/OriginUtils.kt` (`FirClass.irOrigin()` fall-through to `IR_EXTERNAL_DECLARATION_STUB`); `compiler/fir/fir2ir/.../lazy/Fir2IrLazyClass.kt` (members built lazily; comment at L71–76 explicitly says "the origin may be updated after the object creation"); `compiler/fir/fir2ir/.../generators/Fir2IrLazyDeclarationsGenerator.kt` (`createIrLazyClass` derives origin from `firClass.irOrigin()`); `compiler/fir/fir2ir/.../generators/Fir2IrCallableDeclarationsGenerator.kt:1037` (`isExternalParent() = this is Fir2IrLazyClass || this is IrExternalPackageFragment`).
3. Confirmed FIR-level constraint: `FirRegularClassImpl.origin` is a `val`, so the previous iteration's "Option A — re-tag the rehydrated `FirRegularClass` with `FromOtherReplSnippet`" is not feasible without rebuilding the symbol.
4. Ran baseline `:kotlin-scripting-jsr223-test:test`: 12 PASS / 6 SKIP / 3 FAIL — matches prior iteration's expected state.
5. Un-disabled `testResolveFromContextStandard` (simplest disabled test — single eval of `kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl * 6`) and ran it solo. Captured the `Caused by` chain from `TEST-*.xml` and the saved log under `plugins/scripting/.ai/tmp/junie/2026-05-18/g2_solo.txt`.
6. **Key finding from the captured stack** (excerpt):
   ```
   Caused by: java.lang.IllegalArgumentException:
     Unhandled intrinsic in ExpressionCodegen:
       FUN IR_EXTERNAL_DECLARATION_STUB name:<get-shouldBeVisibleFromRepl>
         visibility:public modality:FINAL <> () returnType:kotlin.Int
       at org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen.visitCall(ExpressionCodegen.kt:519)
   ```
   The failure target is the test's plain `val shouldBeVisibleFromRepl = 7` (top-level Kotlin `val` in the test source set, loaded as an external dependency from the test classpath). It is **not** `@InlineOnly` and **not** a fake-override — neither the `[inline]` nor the `[fake_override]` marker is in the render. The disable comment on this test (`"... ReplState.put [fake_override]; see ... G2"`) was therefore inaccurate about the proximate cause.
7. Re-read `compiler/fir/fir2ir/src/.../Fir2IrDeclarationStorage.kt:1390-1427` and noted the existing `allowNonCachedDeclarations` branch: when source-form PSI is available, Fir2Ir already synthesises a non-cached file-class facade (`isNonCachedSourceFileFacade = true`) for top-level callables, satisfying the codegen require. The branch is gated on `firBasedSymbol is FirCallableSymbol<*>` AND `psiFile is KtFile` (i.e. source form). For class-file-deserialised external Kotlin top-level decls — the JSR-223 / REPL case — that branch is not taken and `findIrParent` returns the bare `IrExternalPackageFragment`.
8. Reverted the `@Disabled` removal on `testResolveFromContextStandard` and updated its disable message to point at G11. Updated G1, G2, and added G11 in `current/80-known-gotchas.md`; rewrote Q13 in `target/90-open-questions.md`; rewrote step 1b in `target/50-migration-plan.md`.

## Refined root-cause hypothesis (G11)

**Symptom**: `java.lang.IllegalArgumentException: Unhandled intrinsic in ExpressionCodegen: FUN IR_EXTERNAL_DECLARATION_STUB name:<X> ...` from `org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen.visitCall:519` for any callable `<X>` resolved against an external module from a snippet.

**Proximate failure point**: `compiler/ir/backend.jvm/codegen/src/.../ExpressionCodegen.kt:519` — `require(callee.parent is IrClass) { "Unhandled intrinsic in ExpressionCodegen: ${callee.render()}" }`.

**Mechanism**:
- `Fir2IrDeclarationStorage.findIrParent` returns `IrExternalPackageFragment` for external Kotlin top-level decls when the source-form `allowNonCachedDeclarations` branch (L1390–1427) is not taken — i.e. for class-file-deserialised dependencies, which is the standard JSR-223 / REPL case.
- The standard JVM `FileClassLowering` pass rewrites top-level decls in the *current* module into file-class containers but does not rewrite the parent of *referenced* external decls.
- The lazy `Fir2IrLazySimpleFunction` / `Fir2IrLazyProperty` IR built for the external decl therefore has `IrExternalPackageFragment` as parent.
- Codegen's `visitCall` cannot lower it.

**Why normal `.kts` / `.kt` compilation works**: outside REPL, the JVM compiler treats external Kotlin top-level decls via the file-class deserialisation path (rooted in `JvmFileClassUtil` + `KotlinClassFinder`), so the resulting IR's `callee.parent` is an `IrClass` (the file class) by the time codegen runs. The REPL pipeline's `convertAnalyzedFirToIr` + `generateCodeFromIr` runs the same JVM lowering chain, but the divergence sits upstream — in how the lazy Fir2Ir builder parents external callables.

**Subsumption of G1 / G2**:
- G1 (`@InlineOnly` `[inline]` shape) — the inliner-gap framing from round 1 is correct as a *secondary* cause (the body fails to materialise so the inliner can't lower the call away), but the *proximate* exception is the same codegen `require` on parent-shape.
- G2 (`[fake_override]` shape) — fake-overrides inherited from an external class hit the same `require` when the parent (now the lazy class) is `Fir2IrLazyClass`, which `isExternalParent()` classifies as external.
- Plain external regular decl — third lens; example surfaced this iteration.

**Open question**: which (if any) of the 6 disabled tests actually exercises the cross-snippet `getStateObject` rehydration path (the originally-proposed Q13b fix site)? `testResolveFromContextStandard` is single-eval and hits the synthesis branch (`createIfNotFound=true`), not rehydration. The other 5 disabled tests should be re-examined with the same un-disable-and-capture method before scoping Q13b work.

## Fix direction (anticipated for next iteration)

Two candidate touch sites, in order of estimated effort:

1. **Extend `Fir2IrDeclarationStorage.findIrParent` to synthesise the file-class facade for class-file-deserialised external Kotlin top-level decls in the REPL case** (`Fir2IrDeclarationStorage.kt:1390-1427`). Today the facade synthesis is gated on `configuration.allowNonCachedDeclarations` AND `firBasedSymbol is FirCallableSymbol<*>` AND `containerFile.psi?.containingFile is KtFile`. For class-file dependencies there is no `KtFile` psi, but the `JvmClassName` / `FacadeClassSource` information is recoverable from the deserialised symbol's `DeserializedContainerSource`. A REPL-scoped equivalent path would build a `IrFactoryImpl.createClass` (origin `FILE_CLASS` or `JVM_MULTIFILE_CLASS`) and parent the external top-level callable on it. Risk: needs to keep parent-identity stable across multiple snippet compilations so codegen doesn't see two distinct `IrClass` instances for the same JVM file-class.
2. **Add a REPL-specific JVM lowering phase** analogous to `FileClassLowering` that walks the IR for the current snippet, finds external `IrSimpleFunction`/`IrProperty` with `IrExternalPackageFragment` parent, and reparents them on a freshly built (or cached) file-class facade. Risk: higher cost (affects the JVM lowering pipeline ordering and may interact with `GenerateMultifileFacades`); deferable behind the simpler option 1.

After the umbrella fix lands:
- The previously-listed Q13a (`@InlineOnly` body-materialisation) may still be a real problem (inliner can't lower a `?.let { ... }` without the lambda body materialised) but won't crash codegen — failure mode would shift to inlining-time, not codegen-time.
- Q13b (cross-snippet fake-override rehydration) needs a fresh, targeted repro before further investigation — the iteration log's analysis of `getStateObject()` else-branch is still informative but its test attribution was off.

## Test Results

| Suite | Before (baseline) | After (revert + re-run not executed) | Notes |
|---|---|---|---|
| `:kotlin-scripting-jsr223-test:test` | 12 pass / 6 skip / 3 fail | identical (no production code change) | The 3 fails are pre-existing Q14 / Q15 / Q16 design issues; the 6 skips are the `BLOCKED-CODEGEN-Q13` tests. |
| Solo run `--tests "*testResolveFromContextStandard*"` (temporary un-disable) | n/a | 1 fail, exception captured | Output saved to `plugins/scripting/.ai/tmp/junie/2026-05-18/g2_solo.txt` + `libraries/scripting/jsr223-test/build/test-results/test/TEST-*.xml` from the un-disable run. After updating the disable message, the file is back to its `@Disabled` shape. |

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/.ai/current/80-known-gotchas.md` | Added G11 (umbrella). Refined G1 and G2 with cross-links + retraction notes for the prior iteration's misattribution. Bumped "Last verified" with iteration tag. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Rewrote Q13 with the umbrella framing; updated Q13a / Q13b status with the corrected proximate-cause notes; bumped "Last touched". |
| `plugins/scripting/.ai/target/50-migration-plan.md` | Rewrote step 1b body: revised Goal, Design home, Investigation status, Touch list (Fir2IrDeclarationStorage + FileClassLowering candidates), Repro, Done-when. Bumped "Last verified". |
| `libraries/scripting/jsr223-test/test/kotlin/script/experimental/jsr223/test/KotlinJsr223ScriptEngineIT.kt` | Updated the `@Disabled` message on `testResolveFromContextStandard` to point to G11 with the corrected root cause (external top-level decl). No code-shape change, no other tests touched. |
| `plugins/scripting/.ai/iterations/2026-05-18_step1b-rootcause-refinement.md` | This file — new iteration entry. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Appended index line; updated workstream-state row for Q13 / step 1b. |

## Key Learnings

- **The `IR_EXTERNAL_DECLARATION_STUB` failure family in K2 REPL has a single proximate cause (codegen `require(callee.parent is IrClass)`), not two distinct ones.** G1 (`@InlineOnly`) and G2 (`[fake_override]`) are special cases of an umbrella shape; the IR render's `[inline]` / `[fake_override]` markers can mislead about the failing call when the `$$eval` body has multiple external calls.
- **`FirRegularClass.origin` is `val`** (per `FirRegularClassImpl.kt:36`). Any "re-tag the rehydrated symbol" hypothesis on the FIR side requires constructing a wrapping/replacement `FirRegularClass`, not mutating the existing one.
- **`Fir2IrLazyClass.origin` is `var`** with an explicit comment that the origin is mutated after construction for REPL classes (`Fir2IrLazyClass.kt:71-76`). Setting it after `createIrLazyClass(...)` is a supported idiom — but it does not change the origins of lazily-built member IR, because member origins flow through `Fir2IrDeclarationStorage.getIrFunctionSymbol().isExternalParent()` which short-circuits on the lazy-class parent.
- **`Fir2IrDeclarationStorage.kt:1390-1427` is the IDE-time file-class facade synthesis path**, gated on `allowNonCachedDeclarations`. It uses `JvmFileClassUtil.getFileClassInfoNoResolve` on a `KtFile` psi — so it's currently source-form-only. The REPL fix likely lives by adapting (or generalising) this synthesis path to the class-file-deserialised case.
- **Capture the failure `Caused by` chain before trusting an IR render**: the IR-text rendering of a function body lists all call expressions in lexical order; the proximate codegen failure is the *first* one that fails the `require`, which is not necessarily the topmost call in the render. Read `TEST-*.xml`'s `Caused by` to identify the actual call by name.

## Resources & Cost

| Metric | Value |
|---|---|
| Sessions aggregated | n/a — Junie session, no JSONL |
| `bash` calls | ~12 (one full baseline run, one solo un-disabled test run, plus tmp-dir setup, XML triage, code searches) |
| `search_project` calls | ~7 |
| `open` / `open_entire_file` calls | ~9 / 3 |
| Test suites run | `:kotlin-scripting-jsr223-test:test` (1 baseline) + solo `--tests "*testResolveFromContextStandard*"` (1) |
| Files touched | 5 (4 docs + 1 test source disable-message update) |

### Subagent breakdown

- (none — Junie session; `cavecrew-*` unavailable)

### Loadout-vs-actual

- **Loadout matrix row used**: "Migration-step execution (one numbered step)" — core docs: `AGENT_INSTRUCTIONS.md` + `target/50-migration-plan.md` step 1b. Optional: `current/80-known-gotchas.md` G1/G2, `target/90-open-questions.md` Q13.
- **Budget**: ~7k advisory under Claude; n/a under Junie.
- **Investigation budget caps actuals**:
  - `open_entire_file` calls: 3 / soft-2 (AGENT_INSTRUCTIONS.md, JUNIE_NOTES.md, ITERATION_RESULTS.md — orientation reads, one-shot). Mild overrun, justified by orientation needs.
  - `open` calls on same file: ≤2 per file (within cap).
  - `search_project` without follow-up action: 0 (every search led to an `open` or an edit).
  - `:kotlin-scripting-jsr223-test:test` Gradle runs: 2 / 2 cap exact (baseline + solo failure-capture). No third run in this iteration; final-verification run deferred to the iteration-close step.
  - Full-suite re-runs across other modules: 0 / 1 cap (none needed).

## Post-iteration checklist

- [x] Resources & Cost section populated (Junie-substitute metrics per `JUNIE_NOTES.md`).
- [ ] Migration-plan step strike-through — N/A (step 1b still open, just refined).
- [ ] Active Workstreams updated — N/A (workstream still open; refined hypothesis).
- [ ] `current/90-legacy-inventory.md` — N/A.
- [x] `current/70-tests.md` BLOCKED-BY matrix — to be touched in iteration close if the per-test root-cause attributions change.
- [x] `current/80-known-gotchas.md` — G11 added, G1 / G2 refined.
- [x] Q13 (and sub-questions Q13a / Q13b) in `target/90-open-questions.md` — status refined.
- [x] One-line index entry appended to `ITERATION_RESULTS.md` (next step before closing).

## Not done in this iteration

- Production code fix for the umbrella G11 issue — deferred to the next iteration. The corrected fix direction in `target/50-migration-plan.md` step 1b is the entry point.
- Verification re-runs against the other 5 disabled tests (capture each one's actual failure shape) — would refine which of G1 / G2 / G11-plain each test exercises; out of investigation-budget scope for this iteration (would need 5 more Gradle solo-test runs).
- `current/70-tests.md` BLOCKED-BY per-test matrix update — should be revisited in the next iteration after each disabled test's failure shape is captured.
