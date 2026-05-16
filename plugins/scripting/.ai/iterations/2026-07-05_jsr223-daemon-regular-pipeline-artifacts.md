# Iteration: jsr223-daemon-regular-pipeline-artifacts

## Task

Two review complaints on the prior "regular output" iteration (fix #8):
1. "You're still using a custom decoder for directory" — `SnippetArtifactDirectoryCodec` is a bespoke directory+header format; even though the `.class` files inside are regular, a consumer still needs a private decoder to know which class to load.
2. "You're changing the script evaluation extension, that should not be involved in the compilation at all" — `AbstractScriptEvaluationExtension.kt`'s `compileReplSnippet` (compile-only) had no business living in a file that's supposed to own script *evaluation*.

Follow-up instruction, once the redesign plan was approved: "Do not bother with BTA yet. Comment or remove parts on the BTA side that block the implementation. Continue on the next steps to make daemon part working via the new approach."

## Approach

Per the approved proposal (`.junie/plans/jsr223-daemon-regular-pipeline-artifacts.md`):

1. **Revert the compile-only detour.** `AbstractScriptEvaluationExtension.kt` and `ScriptingProcessSourcesBeforeCompilingExtension.kt` restored to their pre-existing shape; `compileReplSnippet`/`REPL_SNIPPET_COMPILATION_MODE`-interception removed entirely. `SnippetArtifactDirectoryCodec` deleted.
2. **BTA (deferred, per instruction).** `CompileReplSnippetOperationImpl`'s daemon-driven compile relies on the now-removed `compileReplSnippet` branch and is therefore currently non-functional; rather than rework it, its two integration tests (`ReplSnippetCompilationTest.smokeTestStatelessReplSnippetCompilation`/`replSnippetCompilationSurfacesErrors`) are `@Disabled` with an explanation, and the operation's KDoc documents the situation. BTA's own rework onto the regular pipeline is future work.
3. **New frontend infrastructure**, added to `plugins/scripting/scripting-compiler`:
   - `ScriptingProcessSourcesBeforeCompilingExtension` gains a small `markAsReplSnippet()` call for `.repl.kts`-named sources, gated on a new `REPL_SNIPPET_REGULAR_MODE` flag — the source then flows on unmodified into the regular pipeline.
   - `ScriptingCommandLineProcessor`/`ScriptingConfigurationKeys` gain `repl-snippet-regular-mode` (enable the whole mechanism) and `repl-snippet-prior-class` (repeated `ClassId`, ordered).
   - `FirScriptingCompilerExtensionRegistrar`, when the mode is on, additionally registers `FirReplSnippetConfiguratorExtension`/`FirReplSnippetResolveExtension`/`Fir2IrReplSnippetConfiguratorExtension` (previously *only* ever registered by `K2ReplCompiler`'s own test-only in-process registrar) with a REPL host-configuration built inline (`isReplSnippetSource`/`firReplHistoryProvider`).
   - New `ClasspathBackedFirReplHistoryProvider` (`services/`) resolves prior snippets purely from an ordered `List<ClassId>`, reusing shared reconstruction helpers extracted from `ArtifactBackedFirReplHistoryProvider` into a new `StatelessReplSnippetSupport.kt` (`readEmbeddedSidecar`, `restampVisibility`, `findEvalSymbol`, `ReconstructedFirReplSnippet`, `statelessReplDebug`).
   - `ScriptingK2CompilerPluginRegistrar` (in `pluginRegisrar.kt`), when the mode is on, additionally registers a `.repl.kts`-matching `ScriptDefinition` (reusing `ScriptTemplateWithArgs`, same base as the standard `.kts` definition) so the emitted class gets a real `$$result` field.
   - `Fir2IrReplSnippetConfiguratorExtensionImpl`'s sidecar-embedding check widened to recognize both history-provider implementations.
4. **`DaemonReplCompiler` reworked** (`libraries/examples/scripting/jsr223-daemon`): `buildSnippetCompilerArguments` now passes `-Xallow-any-scripts-in-source-roots -Xuse-fir-lt=false -P repl-snippet-regular-mode=true -P repl-snippet-prior-class=<ClassId> (per prior) -cp <priorDirs> -d <outDir>` — no artifact-output/prior-artifact options at all. `decodeCompiledSnippet` predicts each snippet's wrapper `ClassId` from its own source file name via the public `NameUtils.getSnippetTargetClassName` (new `snippetClassId` helper) instead of reading any header; the result field is always the fixed `$$result` name (the `ScriptCompilationConfiguration.resultField` default, untouched by the new `.repl.kts` `ScriptDefinition`).

## Real compiler bug found and fixed

Debugging why a marked `.repl.kts` source still compiled as a plain script (no `$$eval`) traced through: `markAsReplSnippet()` ran correctly → `PsiRawFirBuilder.isReplSnippet` (which, per a `TODO(KT-84387)` comment, bypasses the extension mechanism entirely as "a requirement from AA") reads `KtScript.isReplSnippet` directly → **that getter's implementation was `greenStub?.isReplSnippet ?: copyableUserData`**. Since `greenStub?.isReplSnippet` returns a non-null `Boolean` (not `null`) whenever a stub exists, the elvis operator never falls through to the copyable user data — so for any *physical, on-disk* file (which gets a real stub, unlike `K2ReplCompiler`'s stub-free string-backed PSI), the stub's necessarily-`false` value (computed from the file's original, unmarked parse) permanently overrides `markAsReplSnippet()`.

Fixed in `compiler/psi/psi-api/.../KtScript.kt` by checking the copyable user data *first* (OR-ing both sources): `(getCopyableUserData(REPL_SNIPPET_KEY) == true) || (greenStub?.isReplSnippet == true)`.

Confirmed via a fast debug loop (direct `dist/kotlinc` invocation + `javap`, bypassing the daemon/Gradle-test round trip) that after this fix: a marked `.repl.kts` source compiles to a class with `$$eval`/`INSTANCE`/(when applicable) `$$result`, a subsequent snippet correctly resolves the prior's declaration through nothing but `-cp <priorDir> -P repl-snippet-prior-class=<classId>`.

## Verification

- `:kotlin-scripting-compiler:compileKotlin`/`compileTestKotlin`, `:compiler:build-tools:kotlin-build-tools-impl:compileKotlin`, `:compiler:build-tools:kotlin-build-tools-api-tests:compileTestCompilerPluginsKotlin`, `:examples:scripting-jsr223-daemon:compileKotlin`/`compileTestKotlin`: all compile cleanly.
- `:examples:scripting-jsr223-daemon:test`: **13/13 pass** (`DaemonReplCompilerTest` 2/2, `KotlinJsr223DaemonScriptEngineTest` 11/11) — fully re-driven through the new regular pipeline, no bespoke codec anywhere in the loop.
- `:kotlin-scripting-compiler:test` (full suite, forced rerun): **BUILD SUCCESSFUL** — no regression from the shared `ArtifactBackedFirReplHistoryProvider` refactor, the new FIR extension registration, or the `KtScript.isReplSnippet` fix.
- `:kotlin-scripting-jsr223-test:test`: **BUILD SUCCESSFUL** — no regression for the in-process K2 JSR-223 engine.
- `:compiler:build-tools:kotlin-build-tools-api-tests:testCompilerPlugins --tests "*ReplSnippetCompilationTest*"`: **BUILD SUCCESSFUL** — the two disabled tests skip cleanly, no other regression in that suite.

## Notes / follow-ups

- BTA's `CompileReplSnippetOperationImpl` is explicitly left non-functional for now (documented in its own KDoc and in the disabled test file) — a future iteration should either rework it onto the same regular pipeline or restore a dedicated compile-only path for it specifically.
- `NameUtils.getSnippetTargetClassName`-based prediction and the `KtScript.isReplSnippet` fix are both small, generically useful pieces of infrastructure (not daemon-example-specific) — worth flagging to the wider FIR/PSI-API owners, especially the `KtScript.isReplSnippet` fix, since it affects any future physical-file-based REPL-snippet consumer, not just this module.
- Per prior-session convention, no git commit was made — changes span `compiler/psi/psi-api/**`, `plugins/scripting/scripting-compiler/**`, `plugins/scripting/scripting-compiler-impl/**`, `compiler/build-tools/kotlin-build-tools-impl/**`, `compiler/build-tools/kotlin-build-tools-api-tests/**`, `libraries/examples/scripting/jsr223-daemon/**`, and `plugins/scripting/.ai/**`. Ready for commit review.

## Addendum (2026-07-05, later same day): BTA changes removed outright, not just disabled

Follow-up instruction: *"Please cleanup BTA-changes - we'll reimplement them later on the same architecture as JSR223 over daemon. And clear correspondingly unused pieces too."* Rather than leave `CompileReplSnippetOperationImpl` around in a non-functional, `@Disabled`-tested state (as the base iteration above did), the BTA REPL-snippet transport is now **deleted outright** — it will be rebuilt from scratch on the regular-pipeline architecture (per the base iteration above) when that work is picked back up.

**Deleted:**

- The whole `libraries/scripting/jsr223-bta` module (project `:kotlin-scripting-jsr223-bta`) — `KotlinJsr223BtaScriptEngineImpl`/`Factory`, `BtaReplSnippetSession`, `KotlinJsr223BtaScriptEngineTest`, `build.gradle.kts` — plus its `settings.gradle.kts` `include`/`projectDir` entries and its `notCacheableTestProjects` exemption in `common-configuration.gradle.kts`.
- `CompileReplSnippetOperation`/`ReplSnippetCompilationResult` (public BTA API, `kotlin-build-tools-api`), `CompileReplSnippetOperationImpl` (`kotlin-build-tools-impl`), and `ReplSnippetCompilationTest` (`kotlin-build-tools-api-tests`) — along with the `compileReplSnippetOperationBuilder`/`compileReplSnippetOperation` wiring in `JvmPlatformToolchain.kt`, `JvmPlatformToolchainImpl.kt`, `KotlinWrapperPre2_4_0.kt`, and `KotlinToolchainsV1Adapter.kt`.
- `SnippetArtifactCodec` (`SnippetArtifact.kt`, `:kotlin-scripting-compiler`) and its private `JsonParser` helper — the wire-envelope codec that existed *only* to serve BTA's `ByteArray`-only transport contract; `SnippetArtifactHeader`/`SnippetArtifactSidecar`/`SnippetArtifactHeaderProtoCodec`/`SnippetArtifactSidecarProtoCodec` are untouched (still used by `ArtifactBackedFirReplHistoryProvider`'s embedded-metadata read, which the in-process K2 REPL stateless-compilation test facade still exercises).
- The now-orphaned BTA-only CLI options `repl-snippet-mode`/`repl-snippet-prior-artifact`/`repl-snippet-artifact-output`/`repl-snippet-name` (`ScriptingCommandLineProcessor.kt`) and their backing config keys `REPL_SNIPPET_COMPILATION_MODE`/`REPL_SNIPPET_PRIOR_ARTIFACTS`/`REPL_SNIPPET_ARTIFACT_OUTPUT`/`REPL_SNIPPET_NAME` (`ScriptingConfigurationKeys.kt`) — these were only ever consumed by the now-deleted `compileReplSnippet`/`CompileReplSnippetOperationImpl`; `repl-snippet-regular-mode`/`repl-snippet-prior-class` (the regular-pipeline options this iteration's base work added) are untouched.
- `K2ReplStatelessCompilerTest`'s two `SnippetArtifactCodec`-exercising tests (`testStatelessReplReconstructsDeclarationsFromEmbeddedMetadataAcrossWire`, `testSnippetArtifactCodecRoundtrip`) and `ScriptingCompilerPluginTest`'s BTA/CLI/subprocess-oriented tests that drove the now-deleted `compileReplSnippet`/`SnippetArtifactCodec`/`SnippetArtifactDirectoryCodec` surface (`testReplSnippetCompilationPipelineBranch`, `testReplSnippetCompilationPipelineBranchRegularOutput`, `testReplSnippetCompilationViaCli`, `testReplSnippetCompilationViaKotlincSubprocess`, `testReplSnippetCompilationOptionsParsing`) — all superseded by the base iteration's own regular-pipeline coverage.
- Dangling KDoc/comment references to the removed module/classes in `libraries/examples/scripting/jsr223-daemon/**` (build script, engine impl/factory, tests) and the `kotlin-script-runtime` embedding comment in `kotlin-build-tools-impl/build.gradle.kts`.

**Kept:** the historical design-record sections in `target/40-jsr223-target.md` ("Reusable BTA-backed `ScriptEngine`") and the dated iteration files describing the original BTA work — annotated with a short "Removed" note rather than rewritten, so the design rationale for a future reimplementation stays discoverable.

### Verification

- `:examples:scripting-jsr223-daemon:test`: **13/13 pass, 0 failures**, unaffected (this module never depended on BTA).
- `:kotlin-scripting-compiler:test` (full suite, forced rerun): green after removing `SnippetArtifactCodec` and its two dependent tests — no other test referenced it.
- `:compiler:build-tools:kotlin-build-tools-api-tests`/`kotlin-build-tools-impl`/`kotlin-build-tools-compat`/`kotlin-build-tools-api`: compile cleanly with the BTA REPL-snippet surface fully removed.

## Addendum (2026-07-05, later still): `K2ReplStatelessCompilerTest`'s two e2e tests rewritten to drive `K2JVMCompiler` directly

Follow-up question: with BTA gone, `K2ReplStatelessCompiler`/`SnippetArtifactEvaluator` are only reachable from tests — `K2ReplStatelessCompilerTest` (its own unit test) and `FirReplStatelessCompilerFacade` (the FIR-diagnostics-corpus stateless-mode test double). Since production code (`DaemonReplCompiler`) had already moved to the regular pipeline, the two remaining *end-to-end* tests in `K2ReplStatelessCompilerTest` — `testStatelessReplCompilesSnippetAgainstPriorArtifact`/`testStatelessReplExecutesMultiSnippetSequence` — were the only place still exercising `K2ReplStatelessCompiler`+`SnippetArtifactEvaluator`'s bespoke in-memory artifact/replay path, which no longer matches how any real caller compiles/evaluates a snippet.

**Changes:**

- New private `RegularPipelineReplCompiler` test helper (in `K2ReplStatelessCompilerTest.kt`): compiles a `.repl.kts` source by invoking `K2JVMCompiler` directly (via the existing `runWithK2JVMCompiler` test helper) with the exact same flags `DaemonReplCompiler` sends to the daemon (`-Xallow-any-scripts-in-source-roots -Xuse-fir-lt=false -P repl-snippet-regular-mode=true -P repl-snippet-prior-class=<ClassId> (per prior) -cp <priorDirs> -d <outDir>`), predicting each snippet's `ClassId` via `NameUtils.getSnippetTargetClassName` and wrapping the `-d` output in a `KJvmCompiledScript`/`KJvmCompiledModuleFromClassPath` — i.e. the in-process, no-daemon counterpart of `DaemonReplCompiler`.
- `testStatelessReplCompilesSnippetAgainstPriorArtifact` now asserts on the emitted `.class` files of two snippets compiled this way (no header/artifact object exists anymore).
- `testStatelessReplExecutesMultiSnippetSequence` now runs a real `K2ReplEvaluator` (not `SnippetArtifactEvaluator`'s reflective replay) against a `LinkedSnippet` chain built incrementally by `RegularPipelineReplCompiler.compile`, compiling+evaluating each of 3 snippets one at a time exactly as `KotlinJsr223JvmScriptEngineBase` drives `DaemonReplCompiler`/`K2ReplEvaluator` in production — proving cross-snippet declarations (`x`, `y`) and the final expression result (`x + y == 85`) both work on the regular pipeline end to end.
- Deleted `SnippetArtifactEvaluator.kt` outright (no longer referenced anywhere); trimmed its now-dangling KDoc mentions from `SnippetArtifactHeader`'s doc comment in `SnippetArtifact.kt`.
- `K2ReplStatelessCompiler` itself is now exercised only by `testStateObjectFqNameMismatchIsRejected` (its own pre-compile validation logic) in this test file, plus `FirReplStatelessCompilerFacade`'s stateless-mode FIR-diagnostics coverage — both left untouched, since they test the class's own behavior rather than standing in for a production caller.

### Verification

- `:kotlin-scripting-compiler:compileKotlin`/`compileTestKotlin`: compile cleanly.
- `:kotlin-scripting-compiler:test` (full suite): **119/119 pass, 0 failures/errors**, including all 5 `K2ReplStatelessCompilerTest` cases.
- `:examples:scripting-jsr223-daemon:test`: **13/13 pass**; `:kotlin-scripting-jsr223-test:test`: **23/23 pass** — no regression, unaffected by this test-only change.

## Addendum (2026-07-05, final): `K2ReplStatelessCompiler` and its remaining direct tests removed outright

Follow-up instruction, in direct response to "why is `K2ReplStatelessCompiler` not deleted yet — are its tests still testing something useful?": *"please remove it and it's direct tests too."* Since `K2ReplStatelessCompiler` had zero production callers left (BTA already removed, `DaemonReplCompiler` already on the regular pipeline), it — and everything that existed solely to serve it — is now deleted.

**Deleted:**

- `K2ReplStatelessCompiler.kt`, `ArtifactBackedFirReplHistoryProvider.kt` (its artifact-backed history provider), `SnippetArtifactEvaluator.kt` (already gone from a prior addendum).
- `FirReplStatelessCompilerFacade.kt` and `AbstractReplStatelessDiagnosticsTest` (`AbstractReplTestBaseClasses.kt`) — the FIR-diagnostics-corpus stateless-mode test double that drove `K2ReplStatelessCompiler`/`ArtifactBackedFirReplHistoryProvider` — plus its `TestGenerator.kt` registration and the generated `ReplStatelessDiagnosticsTestGenerated.java`.
- `testHeaderProtoRoundtrip`/`testStateObjectFqNameMismatchIsRejected` (`K2ReplStatelessCompilerTest`'s own direct tests of `K2ReplStatelessCompiler`/`SnippetArtifactHeader`) and the `compileStateless` helper.
- `SnippetArtifactHeader`/`SnippetArtifactHeaderProtoCodec`/`toArtifact`/`decodeHeader` and the `SnippetArtifact` data class itself from `SnippetArtifact.kt` — the out-of-band header/artifact envelope that existed solely for `K2ReplStatelessCompiler`'s own orchestration; `SnippetArtifactSidecar`/`SnippetArtifactSidecarProtoCodec`/`REPL_SIDECAR_PLUGIN_ID` are untouched (still the sole reconstruction payload `ClasspathBackedFirReplHistoryProvider` reads).
- `buildSnippetArtifactFromCompile`/`buildReplHeaderFromFir` from `SnippetArtifactEmission.kt` (only `buildReplSidecarFromFir` remains, still used by the write side).
- The now-dead best-effort/observer machinery in `K2ReplCompiler.kt` that existed only to let `K2ReplStatelessCompiler` capture a partial artifact under FIR errors: `K2ReplCompilationState.snippetCompilationObserver`/`sourceSessionReadyObserver`, the `bestEffortBackend` branch (including the throwaway-`DiagnosticsCollectorImpl`/`ReplSnippetsToClassesLowering` detour and the try/catch around IR conversion), and the standalone `elideErrorBodiedEvalFunctions` pre-pass — `compileImpl` is back to a plain build-FIR → check-errors → convert-to-IR → codegen → check-errors flow.
- Fixed the one real compile break this surfaced: `Fir2IrReplSnippetConfiguratorExtensionImpl.stashReplSidecarMetadataIfStateless`'s `historyProvider !is ArtifactBackedFirReplHistoryProvider` type check (now just `!is ClasspathBackedFirReplHistoryProvider`).
- Swept every remaining dangling KDoc/comment reference to `ArtifactBackedFirReplHistoryProvider`/`SnippetArtifactHeader` across `ReplSnippetLowering.kt`, `ClasspathBackedFirReplHistoryProvider.kt`, `Fir2IrReplSnippetConfiguratorExtensionImpl.kt`, `StatelessReplSnippetSupport.kt`.
- Renamed the now-misleadingly-named `K2ReplStatelessCompilerTest.kt`/`K2ReplStatelessCompilerTest` (it no longer tests `K2ReplStatelessCompiler` at all — only the regular pipeline + the sidecar proto round-trip) to `ReplSnippetRegularPipelineTest.kt`/`ReplSnippetRegularPipelineTest`.

**Kept:** `K2ReplStatelessCompilerTest`'s (now `ReplSnippetRegularPipelineTest`'s) two regular-pipeline end-to-end tests and its sidecar proto round-trip test — none of them exercise `K2ReplStatelessCompiler`.

### Verification

- `:kotlin-scripting-compiler:compileKotlin`/`compileTestKotlin`: compile cleanly.
- `:plugins:scripting:scripting-tests:compileTestFixturesKotlin`: compiles cleanly (confirms removing `AbstractReplStatelessDiagnosticsTest`/`TestGenerator.kt` entry doesn't break the shared test-infrastructure module).
- `:kotlin-scripting-compiler:test` (full suite, forced rerun): **117/117 pass, 0 failures/errors**.
- `:examples:scripting-jsr223-daemon:test`: **13/13 pass**; `:kotlin-scripting-jsr223-test:test`: **23/23 pass** — no regression.
- Repo-wide search confirms zero remaining references to `K2ReplStatelessCompiler`/`ArtifactBackedFirReplHistoryProvider`/`SnippetArtifactHeader`/`SnippetArtifactEvaluator`/`FirReplStatelessCompilerFacade`/`AbstractReplStatelessDiagnosticsTest` anywhere in `compiler/`, `libraries/`, or `plugins/scripting/` source code.
