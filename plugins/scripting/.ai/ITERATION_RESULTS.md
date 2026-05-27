# Scripting/REPL: Iteration Results Log

> **When to consult**: at iteration start (review recent work) and at iteration end (append entry). Template is in [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md). Per-entry detail lives in [`iterations/`](iterations/).
> **Cache lifetime**: stable prefix (status table + workstream table). The index below is append-only — new entries add lines, no existing line is rewritten.
> **Last verified**: 2026-05-27 (step 3 diagnostics-coverage round 2 landed)

**Current status**: Step 1 (JSR-223 K2 bindings) partially landed 2026-05-17; step 1 follow-ups + codegen-stub investigation completed 2026-05-18; **step 1b fix landed 2026-05-18 (round 3)**. **17/21** `KotlinJsr223ScriptEngineIT` passing (net +5 over prior 12), 1 `BLOCKED-DESIGN-Q17` test `@Disabled` (synthetic-snippet null-binding type bug surfaced by the codegen fix), 3 remaining failures are pre-existing step-1 follow-up / design questions (Q14/Q15/Q16). Step 1b production fix = REPL-scoped EPPL-equivalent post-pass inside `ReplSnippetsToClassesLowering` (reparents `IrExternalPackageFragment` callees onto a synthesised JVM file-class facade). Q13 / Q13a / Q13b closed; G1 / G2 / G11 marked FIXED. **Step 3 raw prototype landed 2026-05-27** — stateless K2 REPL compilation as additive sibling of `K2ReplCompiler` (`K2ReplStatelessCompiler` + `SnippetArtifact` + `ArtifactBackedFirReplHistoryProvider`); 3 new unit tests green; Q5a closed, Q5b prototype-locked at paired JSON. **Diagnostics-corpus coverage round 2 landed 2026-05-27** — new `AbstractReplStatelessDiagnosticsTest` mirrors the via-API path: 19/23 generated tests pass; 4 named scenario gaps identified; sidecar bumped 1→2 with `isImplicit` field (Q10b — sidecar-tag direction locked); `parentDisposable` parameter added for in-process callers.

## Workstream state

| Workstream | State |
|---|---|
| KT-83498 — Full LightTree path for `K2ReplCompiler` (migration step 2) | Not started |
| JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback, migration step 1) | In progress (partial — 2026-05-17; 3 step-1 follow-ups remain) |
| K2 REPL `IR_EXTERNAL_DECLARATION_STUB` fix (migration step 1b — Q13) | **Landed 2026-05-18** (round 3). REPL-scoped EPPL-equivalent post-pass added to `ReplSnippetsToClassesLowering` (reparents `IrExternalPackageFragment` callees onto a synthesised JVM file-class facade). JSR-223 suite: 12→17 PASS (net +5). 5 of 6 BLOCKED-CODEGEN tests flipped to PASS; 1 (`testEvalWithContextDirect`) flipped to new BLOCKED-DESIGN-Q17 (different bug surfaced by the fix). Q13 / Q13a / Q13b closed. Follow-ups: non-JSR-223 regression fixture, jvm-host-test/main-kts-test/scripting-tests/fir2ir suites not re-run (validation gap noted in iteration entry). |
| Stateless remote REPL compilation prototype (migration step 3) | **Raw prototype landed 2026-05-27; diagnostics-corpus coverage round 2 landed 2026-05-27** — `K2ReplStatelessCompiler` + `SnippetArtifact` + `ArtifactBackedFirReplHistoryProvider` in `:kotlin-scripting-compiler` (now non-`internal` for cross-module test access). 3 unit tests green (extended to cover `isImplicit` polarity). **New `AbstractReplStatelessDiagnosticsTest` mirroring `AbstractReplViaApiDiagnosticsTest`**: 19/23 generated tests pass; 4 stateless-specific scenario gaps identified and documented (`import_visible_in_next_snippet`, `sealed_hierarchies`, `function_returns_anonymous_object`, `property_visibility`). Sidecar bumped 1→2 with `isImplicit: Boolean` (Q10b — sidecar-tag direction locked); read surface via `ArtifactBackedFirReplHistoryProvider.isImplicit(symbol)`. `K2ReplStatelessCompiler.compile(...)` gained `parentDisposable: Disposable? = null` for in-process / hosted-test lifecycles. Q5a closed; Q5b locked at paired JSON with 2 newly-named field-set candidates (function return-type signature; declaration visibility) before protobuf promotion. Follow-ups: read-side fixes for `import_visible_in_next_snippet` + `sealed_hierarchies`; sidecar field additions for the two visibility/typing scenarios; then protobuf-in-metadata. |
| K1 cleanup chain (steps 4 → 5 → 6 → 7 → 8 → 11) | Not started |
| `scripting-ide-{services,common}` deletion (steps 9, 10) | Not started |
| Classpath-discovery SPI decision (KT-82551, step 13) | Not started |
| Compiler-side test cleanup (step 12) | Not started |

See [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md) for non-negotiables, dispatch matrix, and post-iteration checklist. See [`target/50-migration-plan.md`](target/50-migration-plan.md) for ordered, independently-mergeable steps.

## Iteration index (most recent on top)

- 2026-05-27 — [Stateless K2 REPL — diagnostics-corpus coverage + Q10b sidecar tagging](iterations/2026-05-27_stateless-repl-diagnostics-coverage.md) — migration step 3 round 2 — new `AbstractReplStatelessDiagnosticsTest` mirrors `AbstractReplViaApiDiagnosticsTest`, drives every snippet through `K2ReplStatelessCompiler` with accumulated `SnippetArtifact` history. **19/23 generated tests pass** on first run; 4 stateless-specific scenario gaps named (`import_visible_in_next_snippet`, `sealed_hierarchies`, `function_returns_anonymous_object`, `property_visibility`) — two need read-side wiring on JSON, two need new sidecar fields (return-type signature; declaration visibility). Sidecar version bumped 1→2 with `isImplicit: Boolean` (**Q10b sidecar-tag direction locked**); `ArtifactBackedFirReplHistoryProvider` gains `isImplicit(symbol)` / `findSidecarFor(symbol)` read surface. `K2ReplStatelessCompiler.compile(...)` gains `parentDisposable: Disposable? = null` for in-process / hosted-test callers. `internal` modifiers dropped on the prototype's public surface (`SnippetArtifact`, `SnippetArtifactSidecar`, `K2ReplStatelessCompiler`, codec, helpers) — matches `K2ReplCompiler` precedent.
- 2026-05-27 — [Stateless K2 REPL — raw prototype (migration step 3 / Q5a + Q5b)](iterations/2026-05-27_stateless-repl-prototype.md) — migration step 3 (Stateless remote REPL compilation prototype) — first raw prototype landed as additive sibling of `K2ReplCompiler`: new `K2ReplStatelessCompiler` + `SnippetArtifact` + paired JSON sidecar + `ArtifactBackedFirReplHistoryProvider` reconstructs `FirReplSnippetSymbol`s from artifacts and tags deserialized decls with `isReplSnippetDeclaration` + `originalReplSnippetSymbol`. Two new internal `K2ReplCompilationState` capture hooks (`sourceSessionReadyObserver` + `snippetCompilationObserver`) — stateful path unaffected. 3 new tests in `:kotlin-scripting-compiler:test` green; regression guards (`:kotlin-scripting-jvm-host-test:test`, `:plugins:scripting:scripting-tests:test`) green. Q5a (reconstruction feasibility) closed empirically; Q5b locked at paired JSON for the prototype.

Append one line per iteration: `- YYYY-MM-DD — [Title](iterations/YYYY-MM-DD_slug.md) — workstream / KT-XXXXX — one-line summary`.

- 2026-05-18 — [Step 1b fix landed — `IR_EXTERNAL_DECLARATION_STUB` (G11 umbrella)](iterations/2026-05-18_step1b-fix-landed.md) — migration step 1b (round 3) — production fix landed: REPL-scoped EPPL-equivalent post-pass in `ReplSnippetsToClassesLowering` reparents `IrExternalPackageFragment` callees onto a synthesised JVM file-class facade via `createJvmFileFacadeClass` + `classNameOverride`. JSR-223 suite 12→17 PASS (net +5). 5 of 6 BLOCKED-CODEGEN tests flipped to PASS; 1 surfaced new BLOCKED-DESIGN-Q17 (synthetic-snippet null-binding type). Q13 / Q13a / Q13b closed; G1 / G2 / G11 marked FIXED.
- 2026-05-18 — [Step 1b — `IR_EXTERNAL_DECLARATION_STUB` root-cause refinement](iterations/2026-05-18_step1b-rootcause-refinement.md) — migration step 1b (round 2) — captured actual codegen stack for `testResolveFromContextStandard`; proximate failure is on a *plain external Kotlin top-level `val`* (`<get-shouldBeVisibleFromRepl>`) — neither `@InlineOnly` nor `[fake_override]`. G1 / G2 reframed as special cases of umbrella G11 (external decl parented on `IrExternalPackageFragment`). Fix direction shifted to `Fir2IrDeclarationStorage.findIrParent` file-class facade or REPL-scoped lowering phase. Docs only — no production change.
- 2026-05-18 — [JSR-223 K2 — `IR_EXTERNAL_DECLARATION_STUB` investigation + process docs](iterations/2026-05-18_codegen-stub-investigation.md) — migration steps 1 + 1b — process gaps from prior iteration addressed (new `current/80-known-gotchas.md` + JSR-223 `BLOCKED-BY` matrix + Q13–Q16 + Junie investigation budget); codegen STUB root-cause traced to G1 (`Fir2IrDeclarationStorage.getIrFunctionSymbol().isExternalParent` + lazy-body resolve gap) and G2 (`Fir2IrReplSnippetConfiguratorExtensionImpl.getStateObject()` rehydration loses `FromOtherReplSnippet`); 6 `BLOCKED-CODEGEN-Q13` tests `@Disabled`. **Superseded for root-cause framing by the 2026-05-18 round-2 iteration above.**
- 2026-05-17 — [process-audit] [First baseline audit](iterations/audit_2026-05-17.md) — 4 broken cross-ref paths fixed; Q10a resolved; PROCESS_AUDIT.md Section 2.10 grep fixed; loadout matrix footnotes added for Opus/subagent rules.
- 2026-05-18 — [JSR-223 K2 bindings — step 1 follow-ups round 1](iterations/2026-05-18_jsr223-followup-1.md) — migration step 1 (JSR-223 K2 bindings) — testEvalWithError fixed; lastScriptContext threading + eval() helpers + per-eval bindings field; 12/21 passing, 4 step-1 follow-ups blocked by pre-existing K2 REPL codegen bug.
- 2026-05-17 — [JSR-223 K2 bindings — partial landing](iterations/2026-05-17_bindings-partial.md) — migration step 1 (JSR-223 K2 bindings) — chain-walk eval + classloader-dep extraction + `@InlineOnly` workaround; 11/21 passing, 5 step-1 follow-ups + 4 pre-existing K2 codegen bugs remain.

## Archive cadence

Archive this log when **any** of these triggers:
- 20+ iteration entries since last archive
- 500+ lines in this file
- 30+ days since last archive

Procedure:
1. Move dated iteration files older than the cutoff from `iterations/` → `archive/iterations_YYYY_MM_DD/`.
2. Strike their lines in this index; leave a one-line pointer `- (Archive YYYY-MM-DD) → archive/iterations_YYYY_MM_DD/`.
3. The latest 5 iteration index lines always stay inline for context continuity.

## tmp/ retention

Scratch files under `.ai/tmp/` are not git-tracked beyond the current iteration. Files older than 7 days are deletable without review. Anything worth keeping moves into a numbered doc under `current/` or `target/`.

## Open items carried forward

- See [`target/90-open-questions.md`](target/90-open-questions.md) for Q* with triage fields (Status / Owner / YT / Target doc / Last touched).
- See [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) for the full disposition list of legacy artifacts (REMOVE / MIGRATE / KEEP-FOR-NOW / KEEP).
- KT-83498 (LightTree path for `K2ReplCompiler`) — blocks fully-PSI-free K2 REPL; sequencing-independent of other workstreams.
