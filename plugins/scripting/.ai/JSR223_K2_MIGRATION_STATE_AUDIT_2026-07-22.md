# JSR-223 → K2 Migration — State & Branch Audit

> **Scope**: State of the JSR-223/K2 migration (with focus on the "stateless" remote-compilation variant) + audit of the code changed on branch `rr/ic/sk3`.
> **Produced**: 2026-07-22. **Method**: read `.ai/` doc set + iterations; reviewed `git diff 35b86eabca13..HEAD`.
> **Verdict**: Bindings (Option D) largely landed; stateless remote REPL reworked onto the compiler's *regular* pipeline and proven end-to-end via a portable on-daemon `ScriptEngine` example, but the reconstruction sidecar + engine are **prototype-grade**. Core `.ai` planning docs (`50-migration-plan.md`, `90-open-questions.md`, `current/60-jsr223.md`) are **stale** vs. the 2026-07-05 code state.

---

## 1. TL;DR

The JSR-223 → K2 migration is split into three workstreams. Status as of branch `rr/ic/sk3` (base `35b86eabca13`):

| Workstream | State | Confidence |
|---|---|---|
| **K2 JSR-223 bindings** (Option D — `prependSyntheticSnippets`) | **Landed** (step 1 + 1b). In-process K2 engine green (`:kotlin-scripting-jsr223-test` 23/23). A few design-blocked tests deferred (Q14/Q15/Q16/Q17). | High |
| **Stateless remote REPL compilation** | **Reworked onto the regular compile pipeline** (the earlier bespoke `K2ReplStatelessCompiler` / `SnippetArtifact` / BTA stack was **deleted** 2026-07-05). Proven end-to-end by a portable on-daemon `ScriptEngine` example (`:examples:scripting-jsr223-daemon` 13/13). Reconstruction sidecar + engine are **prototype-grade**. | Medium |
| **KT-83498** (full LightTree path for `K2ReplCompiler`) | **Not started.** Still hybrid PSI/LT; the daemon path is forced onto the deprecated `-Xuse-fir-lt=false` because of it. | — |

**Biggest process issue found**: the branch's *code* is well ahead of its *planning docs*. `target/50-migration-plan.md` (step 3), `target/90-open-questions.md` (Q5b/Q5d) and `current/60-jsr223.md` all still describe the deleted `K2ReplStatelessCompiler` / BTA / `SnippetArtifactHeader` design as the current landed state. These need reconciliation (see §6).

---

## 2. What changed on this branch (code audit)

Branch diff (excluding `.ai/`): **55 files, ~4.7k insertions, ~29 deletions**. Two functional clusters plus one real compiler bug fix.

### 2.1 Compiler bug fix — `KtScript.isReplSnippet` (`compiler/psi/psi-api/.../KtScript.kt`)

- **Before**: `greenStub?.isReplSnippet ?: (getCopyableUserData(REPL_SNIPPET_KEY) == true)` — a stub, when present, always won.
- **After**: `(getCopyableUserData(REPL_SNIPPET_KEY) == true) || (greenStub?.isReplSnippet == true)` — the runtime `markAsReplSnippet()` flag wins first.
- **Why it matters**: any *physical, on-disk* `.kts` file gets a stub built from its original (unmarked) parse, so the stub could never report `true`. This is exactly what blocked compiling a plain source-root file as a REPL snippet. The fix is small and generically useful — **flag to FIR/PSI-API owners** (per iteration note); it affects any future physical-file-based REPL-snippet consumer, not just this branch. Also touches the shared `FirReplSnippetResolveExtension.kt` (new `open fun getSnippetImports(...): List<FirImport>? = null`) — a public EP addition (see §5, rule #2).

### 2.2 Stateless REPL — regular-pipeline reconstruction (in `:kotlin-scripting-compiler`)

New "regular mode": a `.repl.kts` source, marked as a REPL snippet, compiles through the **unmodified** regular JVM frontend/backend (gated by `-Xallow-any-scripts-in-source-roots` + `repl-snippet-regular-mode=true`); priors are fed purely as `-cp <priorDirs>` + one `repl-snippet-prior-class=<ClassId>` per prior. No artifact blob, no bespoke codec on the compile path.

- `ScriptingConfigurationKeys.kt` (+48): 4 new keys — `REPL_SNIPPET_REGULAR_MODE`, `REPL_SNIPPET_PRIOR_CLASSES`, `REPL_SNIPPET_IMPLICIT_RECEIVERS`, `REPL_SNIPPET_FILE_EXTENSION`.
- `ScriptingCommandLineProcessor.kt` (+54), `FirScriptingCompilerExtensionRegistrar.kt` (+50), `pluginRegisrar.kt` (+34): when regular-mode is on, additionally register the FIR REPL EPs (`FirReplSnippetConfiguratorExtension` / `FirReplSnippetResolveExtension` / `Fir2IrReplSnippetConfiguratorExtension`) — previously only ever registered by `K2ReplCompiler`'s test-only in-process registrar — plus a dedicated `.repl.<ext>` `ScriptDefinition` so the emitted class gets a real `$$result` field.
- `ClasspathBackedFirReplHistoryProvider.kt` (175, new): reconstructs prior snippet `FirReplSnippetSymbol`s purely from an ordered `List<ClassId>` on the classpath, reading each prior's embedded `.kotlin_metadata` sidecar.
- `StatelessReplSnippetSupport.kt` (156, new): shared reconstruction helpers (`readEmbeddedSidecar`, `restampVisibility`, `findEvalSymbol`, the `ReconstructedFirReplSnippet` stub, `statelessReplDebug`).
- `SnippetArtifact.kt` (356) / `SnippetArtifactEmission.kt` (131): after the 2026-07-05 deletions, these retain only `SnippetArtifactSidecar` + `SnippetArtifactSidecarProtoCodec` + `REPL_SIDECAR_PLUGIN_ID` (the embedded-metadata payload) and `buildReplSidecarFromFir` (write side). The `SnippetArtifact`/`SnippetArtifactHeader`/`SnippetArtifactCodec`/`toArtifact` envelope was removed.
- `Fir2IrReplSnippetConfiguratorExtensionImpl.kt` (+119), `ReplSnippetLowering.kt` (+44), `ScriptConfigurationAttributes.kt` (+15): sidecar embedding via IR-attribute bridge (`replSidecarMetadataAttr`), embedding gate widened to recognize `ClasspathBackedFirReplHistoryProvider`.
- Test: `ReplSnippetRegularPipelineTest.kt` (297, new) — see §4.

### 2.3 JSR-223 direct on-daemon `ScriptEngine` (`libraries/examples/scripting/jsr223-daemon`, new module)

A BTA-free, portable example meant to be copied into the IntelliJ repo. It compiles snippets **out-of-process** on the standard compile daemon (via `KotlinCompilerRunnerUtils.newDaemonConnection` + a plain `CompileService.compile(...)`) and evaluates them **in-process** with an unmodified `K2ReplEvaluator`.

- `DaemonReplCompiler.kt` (589): a `ReplCompiler<CompiledSnippet>` — the only substitution over the stock JSR-223 engine plumbing. Caches the daemon connection lazily (`Lazy<CompileServiceSession>`), implements `AutoCloseable`, collects + surfaces daemon messages as warnings. Builds `-Xallow-any-scripts-in-source-roots -Xuse-fir-lt=false -P repl-snippet-regular-mode=true -P repl-snippet-prior-class=<ClassId>… -cp <priorDirs> -d <outDir>`; predicts each wrapper `ClassId` via public `NameUtils.getSnippetTargetClassName`.
- `KotlinJsr223DaemonScriptEngineImpl.kt` (271) / `KotlinJsr223DaemonScriptEngineFactory.kt` (74): extends `KotlinJsr223JvmScriptEngineBase<DaemonReplState>`; bindings via Option D `prependSyntheticSnippets` (client-side); implicit-receiver type names smuggled to the daemon as options.
- Shared-infra change (pure widening): `KotlinJsr223JvmScriptEngineBase.kt` `replCompiler`/`replEvaluator` loosened from concrete `K2ReplCompiler`/`K2ReplEvaluator` to their `ReplCompiler`/`ReplEvaluator` interfaces.
- Tests (3): `DaemonReplCompilerTest`, `KotlinJsr223DaemonScriptEngineTest` (11/11), `KotlinJsr223DaemonScriptEngineMainKtsTest` (1 `@Disabled`).

### 2.4 Build Tools API surface

`kotlin-build-tools-api.api` (+53), `JvmPlatformToolchain.kt` (+1), `kotlin-build-tools-impl/build.gradle.kts` (+7). Note: the earlier `CompileReplSnippetOperation` BTA REPL transport was **deleted** on 2026-07-05 (see §6); remaining BTA diff is the residual/other surface. Verify the `.api` dump matches the deletions before commit.

---

## 3. Current state per workstream

### 3.1 Bindings (Option D) — landed

- Mechanism: public refinement-DSL callback `prependSyntheticSnippets` (`libraries/scripting/common/.../api/replData.kt`) returns synthetic snippets compiled + evaluated before the user snippet; binding-diff + accessor generation live definition-side (`jvm-host/.../jsr223/propertiesFromContext.kt`). Compiler/harness stay generic. Generated setters use `bindings.put(...)` (not `bindings[k]=v`) to dodge the `@InlineOnly` codegen stub.
- Step 1b (`IR_EXTERNAL_DECLARATION_STUB`) fixed via `ReplSnippetExternalPackageParentPatcher` in `ReplSnippetLowering.kt`.
- Residual deferrals (design-blocked, intentionally muted, not regressions): Q14 (binding-name escaping), Q15/Q16 (custom `ScriptContext` / second implicit receiver), Q17 (synthetic-snippet null-binding type).

### 3.2 Stateless remote REPL — prototype on the regular pipeline

- The design **pivoted**: instead of a bespoke stateless compiler + serialized artifact envelope, snippets now ride the compiler's regular pipeline; "state" is just the classpath + prior `ClassId`s + each class's embedded `.kotlin_metadata` sidecar. The bespoke stack (`K2ReplStatelessCompiler`, `SnippetArtifactEvaluator`, `SnippetArtifactHeader`, `SnippetArtifactCodec`, `ArtifactBackedFirReplHistoryProvider`, the BTA `CompileReplSnippetOperation`, and the FIR-diagnostics stateless facade) was deleted 2026-07-05.
- Proven end-to-end only through the on-daemon example (13/13) and `ReplSnippetRegularPipelineTest` (regular-pipeline e2e + sidecar round-trip). This is a working proof, **not** a productized, publicly-exposed stateless API.

### 3.3 KT-83498 (LightTree for snippets) — not started; now a soft blocker

- `K2ReplCompiler` is still hybrid (PSI for `KtFileScriptSource`, LT otherwise). The new daemon/regular-mode path is *forced* onto `-Xuse-fir-lt=false` because the source-marking extension only runs on the PSI source-collection path. This makes KT-83498 more urgent — it now blocks a second consumer, and pins the design to a **deprecated** flag.

---

## 4. Test coverage

- `:kotlin-scripting-jsr223-test:test` — 23/23 (in-process K2 engine).
- `:kotlin-scripting-compiler:test` — 117/117 after the deletions.
- `:examples:scripting-jsr223-daemon:test` — 13/13 (`DaemonReplCompilerTest` + `KotlinJsr223DaemonScriptEngineTest` 11/11); `KotlinJsr223DaemonScriptEngineMainKtsTest.testWithImport` `@Disabled` (out-of-process path never runs `refineConfiguration` hooks, so `@file:Import` is unsupported).
- `ReplSnippetRegularPipelineTest` (3 `@Test`, none `@Ignore`d):
  - `testStatelessReplCompilesSnippetAgainstPriorArtifact` — asserts both snippets emit `.class` files, predicted wrapper `ClassId` present.
  - `testStatelessReplExecutesMultiSnippetSequence` — evaluates a 3-snippet chain (`x=42`, `y=x+1`, `x+y==85`) via a real `K2ReplEvaluator`.
  - `testSidecarProtoRoundtrip` — lossless encode/decode of a `SnippetArtifactSidecar` with all kinds/visibilities/imports.
  - ⚠ **Caveat**: the two pipeline tests **early-return (silent no-op) when `!isK2`** rather than being framework-skipped — a green run on a K1 config proves nothing.

---

## 5. Non-negotiable-rule compliance

| Rule | Status | Note |
|---|---|---|
| #1 No new K1 paths | ✅ | New code is K2/FIR only; no K1 frontend callers added. |
| #2 No new public EPs without ratification | ⚠ | `FirReplSnippetResolveExtension.getSnippetImports(...)` added to a public EP contract (compiler/fir); `prependSyntheticSnippets` added to the public refinement DSL. Both are additive/defaulted, but **need ratification/sign-off** per the rule. |
| #3 No reviving daemon REPL / `-Xrepl` / `cli-base/repl/*` | ✅ | The daemon engine deliberately rides the **regular** `CompileService.compile(...)` path — no REPL RMI method, no `-Xrepl`. |
| #4 No PSI-only K2 path | ✅ | Regular-pipeline path is source-type-agnostic; PSI marking confined to one extension + the `KtScript` getter. |
| #5 Don't tighten `K2ReplCompiler` PSI special-casing | ✅ | Not tightened; KT-83498 still open. |
| #6 No `intellij-community` deps in `plugins/scripting/*` | ✅ | None added. |
| #7 `libraries/scripting/intellij` public surface | ✅ | Untouched. |
| Configurator EPs stay PSI-agnostic (no `as? KtScript`) | ✅ | Confirmed across all FIR EP impls; operate on `FirReplSnippet`/`FirRegularClassSymbol`/`ClassId`. |
| Emitted snippet code avoids `@InlineOnly` ops | ✅ | State access generated as plain `Map.get`/`Map.put` `IrCall`s; bindings setter uses `.put(...)`. |

---

## 6. Documentation drift (must-fix before this is trustworthy)

The code state (2026-07-05) contradicts the planning docs. Reconcile:

1. **`target/50-migration-plan.md` step 3** (last verified 2026-06-29) — still says `K2ReplStatelessCompiler` drives the artifact and lists the BTA op / `SnippetArtifactHeader` / `repl-snippet-mode` params as landed. All deleted. Rewrite step 3 to the regular-pipeline design; strike the deleted sub-bullets.
2. **`target/90-open-questions.md` Q5b/Q5d** (last verified 2026-07-02f) — describe the protobuf `SnippetArtifactHeader` (`headerVersion = 1`, `sidecarVersion = 3`) and BTA daemon routing as the settled answer. Update: header/envelope removed; only `SnippetArtifactSidecar` (`CURRENT_VERSION = 4`) survives; BTA transport deleted/deferred. Q5c (perf) is still genuinely open.
3. **`current/60-jsr223.md`** (last verified 2026-05-16) — says bindings gap is open ("without bindings support") and "No code path exists today" for remote compilation. Both are now false. Update the "Known gaps" section.
4. **`AGENT_INSTRUCTIONS.md` Active Workstreams / Reference table** — no mention of `libraries/examples/scripting/jsr223-daemon` or the regular-pipeline stateless design; add them.
5. Run the **post-iteration checklist** (this branch spans many iterations but the mutable docs weren't re-synced after the 2026-07-05 deletions).

---

## 7. Remaining steps & recommendations

### Stateless REPL (to move from prototype → production)

1. **Stabilize the sidecar format.** `SnippetArtifactSidecar.CURRENT_VERSION = 4` is explicitly "unstable", decode enforces **exact-match** (`error(...)` on mismatch, `SnippetArtifact.kt:218`). Decide whether this is an internal-only wire (then document the "rebuild all priors on version change" contract) or a persisted/cross-version format (then make it forward-compatible). Field *numbers* are stable; the field *set* is not.
2. **Overload-safe reconstruction.** `ClasspathBackedFirReplHistoryProvider.materialize` matches declarations **by simple name only** (`associateBy { it.name }`); `MemberRef.descriptor` is always `null` and `returnTypeSignature` is a `ConeKotlinType.toString()` placeholder ("protobuf promotion will replace it"). Overloads collide. Populate real descriptors / structured signatures.
3. **Replace silent-failure paths with diagnostics.** Sidecar decode failure is swallowed (`catch (Throwable) → null`), a `ClassId` lookup miss is silently `continue`d, and a not-ready session returns partial history. These should surface as diagnostics, not vanish.
4. **Harden the `ReconstructedFirReplSnippet` stub** (`source` getter throws `UnsupportedOperationException`; several transform/accept methods are no-ops) — confirm no resolve path can reach the throwing members.
5. **Resolve the workarounds properly**: `Fir2IrReplSnippetConfiguratorExtensionImpl` `isReconstructedSnippetContainerFor` guard (avoids `NoClassDefFoundError: Wrapper$Wrapper`), and `ReplSnippetLowering` placeholder-`IrErrorCallExpression` self-receiver rewrite. Track the open `TODO`s: KT-72975 (caching), KT-74176 (file-only annotation copy), KT-84516, KT-77583 (LT `convertReplSnippet`).
6. **Perf (Q5c)** — never measured beyond single-snippet history; add a long-session benchmark and a caller-side FIR cache strategy before promotion.
7. **Make the two pipeline tests hard-skip on non-K2** (use a framework assumption/skip, not a silent `return`).
8. **Promote a public stateless API** in `libraries/scripting/common` (`@SinceKotlin`-stable) once the sidecar + reconstruction are solid, and re-implement the BTA transport on this architecture (explicitly deferred 2026-07-05).

### JSR-223 daemon engine (to productize the example)

1. Implement `Invocable` (`invokeFunction`/`invokeMethod`/`getInterface`) for JSR-223 compliance.
2. Run `refineConfiguration` hooks on the out-of-process path (unblocks `@file:Import` / `@file:DependsOn`; currently `@Disabled`).
3. Drop the hardcoded `-Xuse-fir-lt=false` once **KT-83498** lands (see below) — it pins the engine to a deprecated flag.
4. Migrate off deprecated internals (`internalScriptingRunSuspend`); provide an SPI-discoverable factory if general use is intended; review the `Thread.sleep(500)` Windows-file-deletion hack.

### Sequencing recommendation

- **Land KT-83498 next** (migration step 2). It is now a *shared* blocker: it unpins the daemon engine from `-Xuse-fir-lt=false` and unblocks PSI-free JSR-223. Consider the smaller standalone `isReplSnippetSource`-narrowing fix (G15) to unmute `MainKtsJsr223Test.testWithImport` if full KT-83498 slips.
- **Reconcile the docs (§6) before any further code** — the current drift makes the plan untrustworthy for the next agent/iteration.
- Bindings design-blocked items (Q14–Q17) can proceed independently.
- K1 cleanup chain (steps 4–11) still gated on stateless reaching a real transport, since at least one IntelliJ consumer depends on out-of-process JSR-223 compilation.

---

## 8. Confidence & method notes

- State claims are derived from the `.ai/` iterations (through 2026-07-05) cross-checked against the actual branch diff and two focused code audits (daemon engine + stateless reconstruction). Test-pass counts are quoted from the iteration records, **not** re-run in this session.
- Not independently re-verified here: the quoted Gradle test results; the exact `kotlin-build-tools-api.api` dump consistency after the BTA deletions (recommend a dump check before commit).
