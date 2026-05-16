# Target — Open Questions

> **When to consult**: before committing to a design answer or claiming a Q* as a task. Q1–Q12 are referenceable IDs; sub-questions Q5a–e, Q10a–f are individually delegate-able.
> **Cache lifetime**: mutable-per-iteration
> **Last verified**: 2026-07-02f (Q2 — `MainKtsJsr223Test.testWithImport` root cause pinned to `K2ReplCompiler`'s session-wide `isReplSnippetSource` predicate (G15); explicitly deferred by decision, test muted rather than fixed. Q14 refined — backtick-quote + delegated property replaces marker-only encoding for most special characters)

Items needing brainstorm before they can be acted on.

## Triage fields

Each Q (and sub-question, where present) carries:

- **Status**: open | in-design | blocked | resolved
- **Owner**: @handle or "unassigned"
- **YT**: KT-XXXXX or "—"
- **Target doc**: relative link where the resolution lands
- **Last touched**: YYYY-MM-DD

## Q1. ~~LightTree path for `FirScript`~~ — resolved

- Status: resolved
- Owner: —
- YT: —
- Target doc: [`../current/10-compiler-representation.md`](../current/10-compiler-representation.md)
- Last touched: 2026-05-16

**Resolved**: scripts already use LT exclusively on the K2 path. See `ScriptJvmK2CompilerImpl` + `convertToFirViaLightTree` + `LightTreeRawFirDeclarationBuilder.buildScript()`. No work needed.

## Q2. LightTree path for `FirReplSnippet` — KT-83498

- Status: in-design (canonical home moved to [`50-migration-plan.md`](50-migration-plan.md) step 2)
- Owner: unassigned
- YT: KT-83498
- Target doc: [`50-migration-plan.md`](50-migration-plan.md#2-land-kt-83498--full-lighttree-path-for-k2replcompiler)
- Last touched: 2026-07-02f

Tracked as migration-plan step 2. Sub-questions (priority, shape — `convertToFir` lambda vs hardwired LT) are recorded inline in step 2 "Design notes".

**`MainKtsJsr223Test.testWithImport` — deferred, not fixed (2026-07-02f)**: this test fails on exactly the `TODO("KT-77583")` gap in `LightTreeRawFirDeclarationBuilder.convertReplSnippet` as soon as a REPL snippet does `@file:Import(...)`. A prior investigation ([session, see `current/80-known-gotchas.md` **G15**](../current/80-known-gotchas.md#g15-k2replcompilers-session-wide-isreplsnippetsource---true--misclassifies-light-tree-compiled-repl-imports-as-snippets--deferred-2026-07-02f-test-muted-not-fixed)) found the failure is triggered for the *wrong* reason: the imported `.main.kts` files are ordinary scripts (not REPL snippets) and are light-tree-compiled (the root snippet is PSI-compiled), but `K2ReplCompiler.createCompilationState` registers `isReplSnippetSource { _, _ -> true }` **session-wide**, so the light-tree builder misclassifies the import as a snippet and routes it into the unimplemented `convertReplSnippet` branch instead of the already-working `convertScript` branch. This means the test could plausibly be fixed **without** waiting for `KT-83498` in full, by narrowing that predicate to the actual root/main snippet source. **Decision: explicitly deferred/ignored for now** — `testWithImport` is `@Ignore`d in `mainKtsJsr223Test.kt` with a reference to this note and to G15, rather than left as an unexplained failure. Revisit either as part of landing `KT-83498` proper, or as a smaller standalone `isReplSnippetSource`-narrowing fix if prioritized before then.

## Q3. ~~`scripting-ide-services` — delete or salvage?~~ — resolved

- Status: resolved
- Owner: —
- YT: —
- Target doc: [`50-migration-plan.md`](50-migration-plan.md#9-delete-scripting-ide-services--companions)
- Last touched: 2026-05-16

**Resolved**: delete confirmed. Future reimplementation possible in a different form, definitely without K1.

## Q4. ~~`scripting-ide-common` — what stays?~~ — resolved

- Status: resolved
- Owner: —
- YT: —
- Target doc: [`50-migration-plan.md`](50-migration-plan.md#10-delete-scripting-ide-common)
- Last touched: 2026-05-16

**Resolved**: delete entirely confirmed. Future reimplementation possible in a different form, definitely without K1.

## Q5. JSR-223 remote compilation — stateless design

- Status: in-design (umbrella; Q5a resolved 2026-05-27, Q5b **full cut landed 2026-06-30c**, Q5c/d/e per-sub)
- Owner: unassigned
- YT: — (umbrella)
- Target doc: [`40-jsr223-target.md#remote-out-of-process-compilation`](40-jsr223-target.md), [`50-migration-plan.md#3-design--prototype-stateless-remote-repl-compilation`](50-migration-plan.md)
- Last touched: 2026-06-30

**Settled**: stateless snippet compilation (snippet artifacts = class files + sidecar metadata). At least one IntelliJ consumer relies on out-of-process JSR-223 compilation today.

| Sub | Question | Status | Owner | YT | Last touched |
|---|---|---|---|---|---|
| Q5a | Reconstruction feasibility: can `FirReplSnippetSymbol` + `FirReplSnippetResolveExtension.getSnippetScope` be implemented over symbols rebuilt from on-disk class metadata + sidecar? | **resolved — happy-path proven 2026-05-27** ([iteration](../iterations/2026-05-27_stateless-repl-prototype.md)) | unassigned | — | 2026-05-27 |
| Q5b | Sidecar format (JSON / proto / hand-rolled binary) + versioning strategy | **metadata-embedding landed 2026-06-30b** ([iteration](../iterations/2026-06-30b_stateless-repl-metadata-embedding.md)). First the **protobuf cut** ([iteration](../iterations/2026-06-30_stateless-repl-protobuf-sidecar-and-subprocess.md)): JSON `SnippetArtifactJsonCodec` → hand-rolled **protobuf-wire** `SnippetArtifactSidecarProtoCodec` (stable field numbers, forward-compatible; `sidecarVersion = 3`; no `.proto`-gen wiring / no protobuf-runtime API dep). Then the **`.kotlin_metadata` embedding**: the residual frontend-derivable sidecar (declarations + visibilities + imports + state-object FQ name + name) is written into the wrapper class's `.kotlin_metadata` via the generic `ProtoBuf.CompilerPluginData` channel (`addCustomMetadataExtension` write / `firDeclaration.compilerPluginMetadata` read, keyed by `REPL_SIDECAR_PLUGIN_ID`) — **no `.proto` change, no metadata-version bump**. The prior round's write-path-timing deferral was resolved with an **IR-attribute bridge** (`IrReplSnippet.replSidecarMetadataAttr`: `prepareSnippet` assembles+stashes, `finalizeReplSnippetClass` embeds), gated to stateless mode. Read side prefers the embedded copy (falsifiably proven by `testStatelessReplReconstructsFromEmbeddedMetadataWhenStandaloneStripped`); standalone blob kept (additive) for the `classId` lookup + config-only flags. Follow-up: cut the standalone blob (needs out-of-band class-id delivery + embedded-path `isImplicit`) | unassigned | — | 2026-06-30 |
| Q5c | Performance: O(N²) FIR reconstruction risk for long sessions; caller-side caching strategy? | open — not measured in the raw prototype (single-snippet history); revisit when promoting | unassigned | — | 2026-05-27 |
| Q5d | Transport: BTA `CompileReplSnippetOperation` vs direct in-process embedding (post IntelliJ-platform-dep cleanup) — probably both eventually | **BTA half landed 2026-05-28c** ([iteration](../iterations/2026-05-28c_stateless-repl-bta-transport.md)) — `CompileReplSnippetOperation` API/Impl + `SnippetArtifactCodec` single-root JSON envelope (framing/envelope/granularity decisions recorded). **End-to-end smoke test landed 2026-06-25b** ([iteration](../iterations/2026-06-25b_stateless-repl-bta-smoke-test.md)) — `ReplSnippetCompilationTest` drives the op through `KotlinToolchains.loadImplementation`; surfaced + fixed a latent `kotlin-build-tools-compat` build break + a missing-`kotlin-script-runtime` packaging gap (follow-up #1 done). **Structured failure surface landed 2026-06-29** ([iteration](../iterations/2026-06-29_stateless-repl-bta-structured-failure.md)) — op retyped `BuildOperation<ByteArray>` → `BuildOperation<ReplSnippetCompilationResult>` (sealed `Success(artifact, diagnostics)` / `Failure(diagnostics)` + `ReplSnippetDiagnostic`); a plain compile failure is now a structured `Failure` (no thrown `RuntimeException`), diagnostics also streamed to the `KotlinLogger` via `COMPILER_MESSAGE_RENDERER`; api dump regenerated (follow-up #2 done). **Daemon execution landed 2026-06-29d** ([iteration](../iterations/2026-06-29d_stateless-repl-bta-daemon-routing.md)) — `WithDaemon` compiles the snippet on the daemon via the regular `CompileService.compile(...)` path (see the daemon-direction notes below). Still open: direct in-process embedding pending IntelliJ-platform-dep cleanup | unassigned | — | 2026-06-29 |
| Q5e | Migration window: K1 daemon bridge breaks before stateless lands; IntelliJ consumer pin to a Kotlin version during transition? | unblocked by 2026-05-27 prototype; still pending public API + transport (Q5d) | unassigned | — | 2026-05-27 |

**Q5b full cut (landed 2026-06-30c, [iteration](../iterations/2026-06-30c_stateless-repl-full-cut.md))**: the standalone sidecar blob is **removed**. `SnippetArtifact` now carries only a minimal out-of-band `SnippetArtifactHeader` (wrapper class id + snippet name + state-object FQ name + emitted result-field name + `isImplicit`; hand-rolled `SnippetArtifactHeaderProtoCodec`, `headerVersion = 1`; `SnippetArtifactCodec` envelope bumped to v2, key `sidecar` → `header`). All reconstruction data (declarations + visibilities + imports) is read **only** from the wrapper class's embedded `.kotlin_metadata` — `ArtifactBackedFirReplHistoryProvider` uses the header solely for the `classId` lookup + `isImplicit`, with no standalone fallback. The one gap was best-effort **error** snippets: plugin `IrGenerationExtension`s (which do snippet→class + embed) are skipped by `convertToIrAndActualizeForJvm` when the FIR reporter has errors, so those snippets emitted a wrapper class with no embedded sidecar. Fixed by running `ReplSnippetsToClassesLowering` explicitly in `K2ReplCompiler`'s best-effort branch (a safe no-op when the extension already ran). Proven by `testStatelessReplReconstructsDeclarationsFromEmbeddedMetadataAcrossWire` + `testHeaderProtoRoundtrip`; guards 24/24 + 24/24, `K2ReplStatelessCompilerTest` 7/7, `ScriptingCompilerPluginTest` 7/7, BTA `ReplSnippetCompilationTest` 6/0-fail/3-skip. Supersedes the "standalone blob kept (additive)" follow-up in the Q5b row above.

**Q5d daemon-execution direction (settled 2026-06-29, [iteration](../iterations/2026-06-29b_stateless-repl-snippet-cli-params.md))**: daemon execution will **not** add a REPL method to `CompileService` — that contradicts non-negotiable rule #3 and migration step 4, which deletes the legacy daemon REPL surface. Instead, stateless snippet compilation rides the **regular compilation path**: a snippet is compiled like an ordinary `.kt` source plus extra parameters that feed prior-snippet state and switch the compiler into snippet mode. Because the parameters are plain scripting-plugin options, the same invocation works from the CLI **and** from a regular `CompileService.compile(...)` call (the daemon forwards plugin args verbatim) — no daemon-protocol change, no REPL-specific transport.

- **Landed 2026-06-29b (parameter surface)**: scripting-plugin CLI options `repl-snippet-mode` (true/false), `repl-snippet-prior-artifact` (repeatable, snippet order), `repl-snippet-artifact-output`; matching `ScriptingConfigurationKeys.REPL_SNIPPET_COMPILATION_MODE` / `REPL_SNIPPET_PRIOR_ARTIFACTS` / `REPL_SNIPPET_ARTIFACT_OUTPUT`; `ScriptingCommandLineProcessor` parsing + a `processOption` test.
- **Landed 2026-06-29c (compile-pipeline branch — consumer)** ([iteration](../iterations/2026-06-29c_stateless-repl-snippet-compile-pipeline-branch.md)): the scripting plugin's regular compile entry (`ScriptEvaluationExtension` → `AbstractScriptEvaluationExtension.eval`) now branches into `compileReplSnippet(...)` when `REPL_SNIPPET_COMPILATION_MODE` is set — decode the prior artifacts (`SnippetArtifactCodec`), drive `K2ReplStatelessCompiler` with the CLI `-cp` as the snippet classpath, write the produced artifact to `REPL_SNIPPET_ARTIFACT_OUTPUT` (compile-only, no eval). Clean compile → `ExitCode.OK` + artifact; any error → `COMPILATION_ERROR`, no artifact written. Tests: `testReplSnippetCompilationPipelineBranch` (direct: keys→artifact, prior consumption, error path) + `testReplSnippetCompilationViaCli` (end-to-end through `K2JVMCompiler().exec` with `-expression` + `-P` options); diagnostics guards 24/24 + 24/24 unaffected.
- **Landed 2026-06-29d (daemon routing)** ([iteration](../iterations/2026-06-29d_stateless-repl-bta-daemon-routing.md)): `CompileReplSnippetOperationImpl` now honours `ExecutionPolicy.WithDaemon` — it compiles the snippet on the daemon through the regular `CompileService.compile(...)` call (`-expression` + the scripting `-P` options above), exchanging priors/output through files and mapping the daemon's exit code + captured compiler messages into the structured `ReplSnippetCompilationResult`. Two enabling pieces: a synthesized `-Xplugin` services jar re-registers the *relocated* scripting plugin (the shaded `kotlin-build-tools-impl` jar strips its `CompilerPluginRegistrar`/`CommandLineProcessor` service files), and a new `repl-snippet-name` option / `REPL_SNIPPET_NAME` key keeps a `-expression` snippet (always named `script.kts`) distinctly named so multi-snippet priors resolve. The daemon-variant `ReplSnippetCompilationTest` cases now run (4/4 incl. in-process + daemon).
- **Out-of-process-only finalised (2026-06-30)** ([iteration](../iterations/2026-06-30_stateless-repl-protobuf-sidecar-and-subprocess.md)): the BTA op rejects `ExecutionPolicy.InProcess` with an explanatory `UnsupportedOperationException` (out-of-process by design; in-process consumers call `K2ReplStatelessCompiler` directly). Coverage now spans core (`K2ReplStatelessCompilerTest`), regular-compile consumer (`ScriptingCompilerPluginTest`), daemon transport (`ReplSnippetCompilationTest`), and a genuine separate-process `kotlinc` run (`testReplSnippetCompilationViaKotlincSubprocess`).
- **No longer open**: direct in-process embedding is intentionally **not** provided (supersedes the earlier "pending IntelliJ-platform-dep cleanup" note in the Q5d row above).

## Q6. Classpath-based script definition discovery (KT-82551)

- Status: in-design (default: un-deprecate + document, plan SPI replacement separately)
- Owner: unassigned
- YT: KT-82551
- Target doc: [`30-embedding-target.md`](30-embedding-target.md#script-definition-discovery)
- Last touched: 2026-05-16

`META-INF/kotlin/script/templates/*.classname` markers are deprecated, but no successor exists. `kotlin-main-kts` and third-party defs depend on it.

| Option | Description |
|---|---|
| Un-deprecate, document as the SPI | Cheapest; accepts current contract |
| Replace with `ServiceLoader<ScriptDefinitionContributor>` SPI | Modern Java SPI; requires definition modules to adapt |
| Keep deprecated forever | Status quo; bad signal |

## Q7. ~~`libraries/scripting/intellij` — move or delete?~~ — resolved

- Status: resolved (KEEP)
- Owner: —
- YT: —
- Target doc: [`20-api-target.md`](20-api-target.md)
- Last touched: 2026-05-16

**Resolved**: KEEP. Used by IntelliJ plugin authors to wire custom-scripts support. Not a candidate for removal or relocation.

## Q8. `IrScript` schema: drop K1-only fields

- Status: open (gated on whole-compiler K1 retirement)
- Owner: unassigned
- YT: —
- Target doc: [`10-compiler-target.md`](10-compiler-target.md#ir)
- Last touched: 2026-05-16

`providedProperties`, `providedPropertiesParameters` are unused on K2. After K1 frontend retires, regen `IrScript` without them.

Side question: should K2 actually unify provided properties + explicit call parameters (current K2 behavior) or split them back out for clarity? Argues for keeping the field but document its K2 semantics.

## Q9. ~~Single configurator extension vs split~~ — resolved

- Status: resolved (KEEP split)
- Owner: —
- YT: —
- Target doc: [`10-compiler-target.md`](10-compiler-target.md)
- Last touched: 2026-05-16

`FirScript*` and `FirReplSnippet*` have separate but parallel sets of configurator / resolution / Fir2Ir extensions (6 EPs total). The script and snippet shapes diverge enough to keep split. No work.

## Q10. K2 binding semantics in REPL — settled, sub-questions remain

- Status: in-design (umbrella; sub-questions tracked below)
- Owner: unassigned
- YT: — (umbrella)
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-07-01

**Settled**: pursue **Option D** in [`40-jsr223-target.md`](40-jsr223-target.md) — implicit-snippets refinement-DSL callback + a JSR-223 binding configurator that emits a delegating-property snippet on binding diffs.

| Sub | Question | Status | Owner | YT | Last touched |
|---|---|---|---|---|---|
| Q10a | DSL naming: settled as `prependSyntheticSnippets`. See [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md) | resolved | unassigned | — | 2026-05-17 |
| Q10b | Implicit-snippet tagging in `FirReplHistoryProvider`: needs an EP "implicit" tag, or caller-side bookkeeping? | **in-design — sidecar-tag direction locked 2026-05-27** ([iteration](../iterations/2026-05-27_stateless-repl-diagnostics-coverage.md)): `SnippetArtifactSidecar.isImplicit: Boolean` (sidecar v2), read surface via `ArtifactBackedFirReplHistoryProvider.isImplicit(symbol)`. Closes outright once a concrete `prependSyntheticSnippets` consumer exercises the read path. | unassigned | — | 2026-05-27 |
| Q10c | Removal semantics: when a binding name is removed, what does the next snippet emit? Shadowing marker vs delegate-throws-at-access | **resolved — landed 2026-07-01d** ([iteration](../iterations/2026-07-01d_jsr223-binding-lifecycle.md)): **shadowing marker** chosen. A removed (or current-context-absent) binding emits a shadowing accessor that keeps the previous declared type (so existing user code still type-checks) but **throws** `NoSuchElementException("JSR-223 binding \"x\" is no longer available")` at access, replacing the cryptic `null cannot be cast to non-null type ...` NPE from the stale getter. Re-adding the binding emits a fresh typed accessor that shadows it again. Repro/guard: `KotlinJsr223ScriptEngineIT.testRebindRemoval`. | unassigned | — | 2026-07-01 |
| Q10d | Type stability: if a binding's runtime type changes, re-emit new delegating property (shadow old) vs fail? Probably re-emit; confirm | **resolved — landed 2026-07-01d** ([iteration](../iterations/2026-07-01d_jsr223-binding-lifecycle.md)): **re-emit** confirmed. `generateBindingSnippetIfNeeded` diffs the accumulated `exposedBindings` against the current bindings by `KotlinType` (type-name + nullability) and emits a fresh typed accessor for every new-or-retyped name, shadowing the stale one; the old `var x: Int` (whose `as Int` getter would otherwise fail to compile / ClassCastException against the new value) no longer resolves. Repro/guard: `KotlinJsr223ScriptEngineIT.testRebindWithChangedType`. | unassigned | — | 2026-07-01 |
| Q10e | Bootstrap timing: canonical `bindings` accessor emitted once on first non-empty `Bindings`; clear+rebind edge case | open — decide during impl | unassigned | — | 2026-05-16 |
| Q10f | Composability with other handlers: registration order, sorted by priority key, or undefined? | open | unassigned | — | 2026-05-16 |

## Q11. Public stability of `JvmScriptCompiler.createLegacy()` etc.

- Status: open (owner needed)
- Owner: unassigned
- YT: —
- Target doc: [`50-migration-plan.md`](50-migration-plan.md#7-delete-jvm-host-legacy-repl-wrappers)
- Last touched: 2026-05-16

These are not annotated `@SinceKotlin` / `@ExperimentalApi` everywhere. Confirm we can remove them without a deprecation cycle, or budget the cycle in.

## Q12. Generated test runners

- Status: open — quick audit
- Owner: unassigned
- YT: —
- Target doc: [`50-migration-plan.md`](50-migration-plan.md#12-compiler-side-scripting-test-cleanup)
- Last touched: 2026-05-16

`plugins/scripting/scripting-tests/` includes generated runners (`*TestGenerated.java`). After deletions, re-run `./gradlew generateTests`. Confirm nothing else generates scripting-related test classes outside this module.

## Q13. K2 REPL `IR_EXTERNAL_DECLARATION_STUB` on external Kotlin top-level decls (umbrella; was: `@InlineOnly` / `[fake_override]`) — **CLOSED 2026-05-18**

- Status: **closed** — fix landed in [`50-migration-plan.md`](50-migration-plan.md) step **1b**, third iteration `2026-05-18_step1b-fix-landed.md`
- Owner: —
- YT: — (no separate YT issue filed; was bundled with the JSR-223 step 1 work; if a public-facing tracking issue is wanted post-hoc, file under "K2 REPL JVM codegen — external decl file-class facade")
- Target doc: [`50-migration-plan.md`](50-migration-plan.md#1b-fix-k2-repl-ir_external_declaration_stub)
- Last touched: 2026-05-18 (third iteration — fix landed)

**Resolution**: added a REPL-scoped EPPL-equivalent post-pass (`ReplSnippetExternalPackageParentPatcher`) inside `ReplSnippetsToClassesLowering` at `plugins/scripting/scripting-compiler/src/.../irLowerings/ReplSnippetLowering.kt`. It runs after each snippet's `finalizeReplSnippetClass`, walks the snippet `targetClass`, and for every `IrMemberAccessExpression` whose callee implements `IrMemberWithContainerSource`, has a `FacadeClassSource` container, and is currently parented on `IrExternalPackageFragment`, it synthesises a JVM file-class facade via `createJvmFileFacadeClass` (with `classNameOverride` so the bytecode references the real `*Kt` / multifile-facade class) and reparents the callee + corresponding property on the facade. This is the same logic that `org.jetbrains.kotlin.backend.jvm.lower.ExternalPackageParentPatcherLowering` applies for normal `.kt` compilation; the REPL pipeline previously missed it because the snippet body's IR is structurally hidden from the standard JVM lowering pass.

**Verification**: JSR-223 suite 12 PASS / 6 SKIP / 3 FAIL → 17 PASS / 1 SKIP / 3 FAIL. 5 of 6 BLOCKED-CODEGEN tests now pass: `testResolveFromContextStandard`, `testResolveFromContextLambda`, `testResolveFromContextDirectExperimental`, `testMultipleCompilable`, `testEvalWithContext`. The 6th (`testEvalWithContextDirect`) failed for a different, non-codegen reason that is now tracked as Q17.

Two sub-questions resolved as part of the umbrella fix:

| Sub | Question | Status | Owner | YT | Last touched |
|---|---|---|---|---|---|
| Q13a | `@InlineOnly` deserialisation: should the K2 REPL Fir2Ir pipeline run the inliner phase on declarations imported from stdlib `klib`/jar with `@InlineOnly`, or should `IrLazyDeclarations` keep the body materialised for those members regardless of inliner ordering? | **closed** — the umbrella parent-shape fix routes `@InlineOnly` calls through the standard `IrInlineFunctionResolver` path (callee now has `IrClass` parent → codegen no longer chokes; inliner runs at its usual phase). Body-materialisation as a separate concern is theoretically still open but not observed in any test after the fix. | — | — | 2026-05-18 (third iteration) |
| Q13b | Cross-snippet fake-override resolution: when a user snippet calls a member inherited by a class defined in a previous snippet's FIR module, why does the fake-override resolver fail to materialise the override chain? | **closed (no separate fix needed)** — once the umbrella fix lands, `[fake_override]` calls with `IrExternalPackageFragment` parent are reparented onto the JVM file-class facade and pass the codegen require. `Fir2IrReplSnippetConfiguratorExtensionImpl.getStateObject()` rehydration analysis (first iteration) is still useful as a mechanism description but does not require a production change. If a future test exposes the fake-override resolver gap independently of parent shape, file a fresh question. | — | — | 2026-05-18 (third iteration) |

## Q17. ~~JSR-223 K2 synthetic-snippet `null`-binding type generates non-null property accessor~~ — resolved

- Status: **resolved — landed 2026-07-01** (Option "emit `Any?` for null-valued bindings")
- Owner: unassigned
- YT: —
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-07-01

**Resolved** ([iteration](../iterations/2026-07-01_jsr223-q17-null-binding-type.md)): the generator already tagged null bindings `KotlinType(Any::class, isNullable = true)`, but `propertiesFromContext.kt` rendered the property/getter-cast type as `${type.typeName}`, and `KotlinType.typeName` strips the trailing `?` — so the emitted accessor was `var x: kotlin.Any` / `... as kotlin.Any`, which NPEd on the null value before the user's own null-safety could run. Fix: render the type with its nullability marker (`if (type.isNullable) "${type.typeName}?" else type.typeName`) in both the declared type and the getter cast. `testEvalWithContextDirect` un-`@Disabled` and now PASSes.

**Symptom**: `engine.put("nullable", null)` followed by `engine.eval("nullable?.let { it as Int } ?: -1")` throws `java.lang.NullPointerException: null cannot be cast to non-null type kotlin.Any` from the synthetic-snippet's `getNullable()` accessor. The user's `?.let { ... } ?: -1` defence is bypassed because the cast happens *before* the user code receives the value.

**Cause**: in `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`, the binding-property generator selects the property type from the *current* runtime value's class. When the bound value is `null`, the generator falls back to `Any` (non-null) instead of `Any?`, so the generated getter body is `return bindings["nullable"] as Any` — which NPEs at the cast on a `null` value.

**Options**:

| Option | Description |
|---|---|
| Emit `Any?` for null-valued bindings | Cheapest; preserves user's `?.let` semantics; loss of type information for the snippet because subsequent rebinds to a non-null value would not "narrow" the property type. |
| Emit `Any?` for *all* bindings whose runtime class cannot be inferred to be non-null at the bind point | Same as above, broader rule; aligns with JSR-223 spec which doesn't promise non-null bindings. |
| Inspect the binding `Bindings` map type at code-gen time and emit `Any?` only when the binding is actually null at first observation | More accurate but stateful; later rebinds may still surprise. |
| Defer typing until the user references the property and infer from usage | Out of scope — would require deeper FIR / call-site analysis. |

**Repro**: `testEvalWithContextDirect` in `KotlinJsr223ScriptEngineIT`. Currently `@Disabled` pending this design.

**Reference**: [iterations/2026-05-18_step1b-fix-landed.md](../iterations/2026-05-18_step1b-fix-landed.md) (third iteration — surfaced as side-effect of G11 fix).

## Q14. ~~JVM-safe binding-name encoding for JSR-223~~ — resolved

- Status: **resolved — prototype landed 2026-07-01e, refined 2026-07-02, marker alphabet refined again 2026-07-02b** (Option "backtick-quote + delegated property, marker-encode only the JVM-hard-invalid subset, uniform hex code-point marker alphabet")
- Owner: unassigned
- YT: —
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-07-02b

**Resolved, 2026-07-02b refinement** ([iteration](../iterations/2026-07-02b_jsr223-binding-name-uniform-codepoint-marker.md)): the user asked to replace the hand-picked mnemonic words (`dot`, `colon`, `lbracket`, ...) with "some more well-known encoding, e.g. html one". HTML5 named character references were researched and found to cover most of the still-marker-encoded characters (`&period;`, `&colon;`, `&semi;`, `&lsqb;`/`&rsqb;`, `&sol;`, `&bsol;`, `&lt;`, `&gt;`) but not backtick or raw newline/CR, and a literal numeric-escape style (`&#46;` / `%2E`) can't be embedded directly (it reintroduces other problematic characters — `&`, `#`, `;`, `%`). Chosen instead: drop `BINDING_NAME_CHAR_MNEMONICS` entirely and uniformly encode every marker-needing character as `__u<hex>__` (its Unicode code point in hex) — the same rule the fallback branch already used for characters with no mnemonic entry (e.g. `☺` → `__u263a__`), now the *only* rule (`a.b` → `a__u002e__b`, `c:d` → `c__u003a__d`, ...). A literal `\uXXXX`-style escape (with a real backslash) is impossible here, since `\` is itself one of the marker-needing characters. `testEvalWithContextNamesWithSymbols` stays PASS, now asserting the `__u<hex>__` spellings.

**Resolved, 2026-07-02 refinement** ([iteration](../iterations/2026-07-02_jsr223-binding-name-delegate-fix.md)): the 2026-07-01e prototype (below) marker-encoded *every* non-plain-identifier name, including ones Kotlin can legally backtick-quote (space, `$`, non-ASCII, JVM-"dangerous" `? * " | %`) — that over-application was actually a workaround for a narrower bug: [`current/80-known-gotchas.md`](../current/80-known-gotchas.md#g12-backtick-quoted-property--getbindings-call-in-the-same-live-repl-snippet-trips-property-getter-or-setter-expected) (G12), a K2 REPL/script-snippet parser bug where a backtick-quoted property with a **hardcoded** `get()`/`set()` body fails to parse if the same snippet also calls `getBindings(...)` (an implicit-receiver call) — reproducible only inside a live incremental REPL session, not a one-shot `.kts` compile. Fix: keep marker-encoding *only* for the JVM-hard-invalid chars (`. ; [ ] / < > : \`, plus backtick/newline — none of which survive even backtick-quoting); every other name is now backtick-quoted **verbatim** (restoring the original K1 spelling) and declared with a generated `__Jsr223BindingDelegate` (`by ...`) instead of a hardcoded accessor, since a delegate expression is parsed by `parsePropertyDelegateOrAssignment()` before the misfiring accessor-parsing code path is ever reached. `testEvalWithContextNamesWithSymbols` stays PASS, now asserting the mixed marker/backtick spellings.

**Resolved, 2026-07-01e prototype** ([iteration](../iterations/2026-07-01e_jsr223-binding-name-encoding.md)): binding names that aren't plain Kotlin identifiers are exposed as typed properties whose JVM-unsafe / non-ASCII characters are reversibly encoded into `__<mnemonic>__` markers (`a.b` → `a__dot__b`, `c:d` → `c__colon__d`, `☺` → `__u263a__`), which are plain identifiers (so no backticks) that pass `FirJvmNamesChecker`. The property getter/setter reaches the value through the raw binding key, so only injectivity — not runtime decode — is required; a pathological binding literally spelled like an emitted marker (e.g. `a__dot__b`) is the documented prototype limitation. `testEvalWithContextNamesWithSymbols` un-`@Disabled` and now PASSes (asserts the new marker spellings rather than the K1 backslash scheme).

**Two root causes fixed** (both in `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`):

1. K1's `\`-prefixed escape table (`.` → `\,`, `:` → `\!`, ...) is unusable on K2 because `\` is itself in `FirJvmNamesChecker.INVALID_CHARS` (`. ; [ ] / < > : \`). Replaced with the `__<mnemonic>__` scheme (`encodeBindingNameToMarkerIdentifier`; unmapped chars → `u<hex>` code-point mnemonic; a leading kept digit is `_`-guarded).
2. The pre-existing "is this a clean identifier?" gate used `Name.isValidIdentifier`, which is a **JVM-spec** check — it only rejects `. ; [ /` and a leading `<`, so JVM-legal-but-non-Kotlin names like `o]p`, `g$h`, `c:d`, `i<j`, `k>l`, `☺` and space slipped through as **raw** property names and failed to parse (`var <no name provided>`). Replaced with a proper plain-ASCII-identifier check so every non-plain name reaches the encoder.

**Contract note**: after the 2026-07-02 / 2026-07-02b refinements, only the JVM-hard-invalid subset gets a K1 → K2 user-visible spelling change (K1 spelled `a.b` as `` `a\,b` ``; K2 spells it `a__u002e__b`) — every other previously-K1-backtick-quoted name (`` `g$h` ``, `` `u v` ``, `` `☺` ``) keeps its original K1 spelling.

**Options considered** (chosen: hybrid of rows 1, 3, and the uniform code-point alphabet):

| Option | Description |
|---|---|
| Prefix-encoded ASCII for every non-plain-identifier name (`__dot__`, `__colon__`, ...) — **2026-07-01e prototype, since narrowed** | Reversible, JVM-safe, readable; clutters generated source; over-applies to names that don't actually need it (see G12). |
| Named mnemonics for the JVM-hard-invalid subset (`__dot__`, `__colon__`, ...) — **2026-07-02, since replaced 2026-07-02b** | Readable for the handful of hand-picked characters; needs a maintained table; doesn't generalize past the characters someone thought to name. |
| HTML5 named character references (`period`, `colon`, `semi`, `lsqb`, `rsqb`, `sol`, `bsol`, `lt`, `gt`, ...) — **considered, rejected 2026-07-02b** | "Well-known" vocabulary for web developers; doesn't cover every marker-needing character (no HTML5 name for backtick or raw newline/CR), so it would still need a fallback rule — not actually simpler than a hand-picked table. |
| Uniform hex code-point marker (`__u<hex>__` for every marker-needing character, no table) — **chosen, 2026-07-02b** | Zero maintenance, fully general over all Unicode, already used as the pre-existing fallback for unmapped characters; markers are less immediately readable than a mnemonic word (`a__u002e__b` vs `a__dot__b`). |
| JNI native-method name mangling (`_1`/`_2`/`_3`/`_0xxxx`) — **considered, not chosen** | The JDK's own official scheme for this exact problem class; fully general; markers are terser but more cryptic than `__u<hex>__` and diverge from this codebase's existing `u<hex>` convention. |
| Punycode-style | Reversible, compact, well-specified; verbose for common cases; needs a small Kotlin impl. |
| Backtick-quote verbatim + delegated property (`__Jsr223BindingDelegate`), marker-encode only the JVM-hard-invalid subset — **chosen, 2026-07-02** | Restores the original K1 spelling for everything Kotlin can legally backtick-quote; needs a generated delegate class instead of a hardcoded accessor (works around G12); marker fallback still needed for the hard-invalid subset. |
| Bind-only on subset, expose remainder via `bindings["..."]` | Cheapest; some K1 binding scenarios silently lose property access. |
| Rewrite the contract: drop typed properties for non-identifier names entirely; mute `testEvalWithContextNamesWithSymbols` | Cleanest; explicit K1 → K2 contract change; needs sign-off. |

## Q15. Lambda / anonymous-class binding-type rendering

- Status: in-design — generator-side filter landed; typed-access decision deferred
- Owner: unassigned
- YT: —
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-05-18

When a JSR-223 binding's runtime value is an indy-lambda (`-Xlambdas=indy`) or a local/anonymous class, `KClass.qualifiedName` may be non-null but not a valid Kotlin type reference (e.g. `Foo$$Lambda$1`, `MyKt$f$lambda$1`). Embedding it into synthetic-snippet source breaks the parser. See [`../current/80-known-gotchas.md`](../current/80-known-gotchas.md#g9-lambda-binding-types-have-non-parseable-qualifiedname) (G9).

Current state: `propertiesFromContext.kt` filters such bindings with `isParseableKotlinQualifiedName(qn)`; they remain accessible via `bindings["..."]` but not as typed properties. Open question: do we want typed access (e.g. emit `var foo: (Int) -> Int` by inspecting the functional-interface signature) or keep the current "skip with `Any?` fallback" behaviour? Decision rides on whether typed lambda accessors are a stated JSR-223 K2 contract.

## Q16. ~~JSR-223 K2 implicit-receiver strategy~~ — resolved

- Status: **resolved — landed 2026-07-02c** (Option "add a second implicit receiver")
- Owner: unassigned
- YT: —
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-07-02c

**Resolved**: `ScriptTemplateWithBindings` is now added as a **second** implicit receiver alongside `ScriptContext`, in both `configureExposedJsr223Context` overloads (`libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`). No FIR/IR/evaluator change was needed — a prior investigation confirmed the whole implicit-receiver pipeline (`FirReplSnippetConfiguratorExtensionImpl.configure()`, `ReplSnippetsToClassesLowering.makeImplicitReceiversFieldsWithParameters()`/`finalizeReplSnippetClass()`, `K2ReplEvaluator.eval()`) is already fully generic over N receivers, keyed by type/index. The compile-time overload adds `ScriptContext::class` and `ScriptTemplateWithBindings::class` idempotently (same existing-receiver-list guard that protects `testSimpleEvalInEval` from receiver-count drift across nested evals); the eval-time overload passes a new private `Jsr223ScriptTemplateWithBindings` concrete wrapper around the same live engine-scope `Bindings` map already backing `ScriptContext`'s `ENGINE_SCOPE`, so both receivers share one underlying data source with no extra synchronization. `KotlinJsr223ScriptEngineIT.testEvalInEvalWithBindingsWithLambda` is un-`@Disabled` and passes; full suite is 23/23 (0 skipped, 0 failed). No ambiguous-receiver diagnostics were observed — `ScriptTemplateWithBindings` exposes only `bindings`, while `ScriptContext` exposes JSR-223-specific methods (`getBindings`, `getAttribute`, `getWriter`, ...), so there's no member-name collision under normal use.

**Options considered** (chosen: row 2):

| Option | Description |
|---|---|
| Drop `ScriptTemplateWithBindings` helper API; document the K2 contract as "extension receivers must be on `ScriptContext` or `Bindings`" | Cleanest; breaks existing user code that used the K1 extension shape. |
| Add a second implicit receiver (`ScriptTemplateWithBindings`) on K2 `$$eval` — **chosen, 2026-07-02c** | Backwards-compatible; no FIR/IR/evaluator change needed (receivers are already N-ary); ambiguous-receiver risk did not materialize in practice. |
| Switch the JSR-223 script template entirely (so the K2 `$$eval` receiver IS `ScriptTemplateWithBindings`) | Compatible-by-construction; ripples through `KotlinJsr223DefaultScript` + every snippet's compilation config; de-prioritized once row 2 was shown to be free. |

**Follow-up — resolved 2026-07-02d**: `MainKtsJsr223Test` (in `kotlin-main-kts-test`) used to independently fail all 3 tests with `Unresolved reference 'getBindings'` on the synthetic snippet — i.e. even the pre-existing `ScriptContext` receiver wasn't being added for the `MainKtsScript` template. Root cause (confirmed via live debugging, not the classpath-rediscovery hypothesis originally suspected): `K2ReplCompiler.createCompilationState` never registered a `FirScriptCompilationComponent` nor a `ScriptCompilationConfigurationProvider` for its per-snippet FIR sessions — unlike the one-shot `ScriptJvmK2CompilerImpl`, which does both. So `FirScriptDefinitionProviderService.getBaseConfiguration` always fell back to a **freshly, independently built** `defaultHostConfiguration` (classpath-discovery-based, via `configureScriptDefinitions`), never the REPL session's own JSR-223-wired `hostConfiguration`. For `MainKtsScript` (whose `fileExtension` is `main.kts`), neither that fallback's classpath-rediscovered definition *nor even the REPL session's own correctly-wired definition* matched the synthetic snippet source name (`...repl.kts`, extension mismatch), so resolution fell through to the generic `ScriptTemplateWithArgs`-based default definition — carrying no JSR-223 wiring at all, hence zero implicit receivers. (The generic `KotlinJsr223DefaultScript`-based `jsr223-test` suite worked only by extension-matching coincidence: `.repl.kts` happens to end in `.kts`, the generic default's own extension.) **Fix** (`plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` + `K2ScriptingCompilerEnvironment.kt`): `K2ReplCompiler.createCompilationState` now registers its own `CliScriptDefinitionProvider`/`scriptCompilationConfigurationProvider` on the compiler configuration (mirroring `K2ScriptingCompilerEnvironment.createCompilerState`'s existing pattern), using a new `ReplSessionScriptDefinition` whose `isScript` always matches (since a REPL session only ever compiles snippets belonging to its own configuration) — this makes the session's own, correctly-wired definition always win and never falls through to classpath-based rediscovery. `testSimpleEval` and `testWithDirectBindings` now PASS; `testWithImport` still fails, but for an unrelated, separately-tracked, pre-existing compiler limitation (`TODO("KT-77583")` in `LightTreeRawFirDeclarationBuilder.convertReplSnippet` — light-tree REPL-snippet support isn't implemented yet; see migration-plan step 2 / KT-83498). See `../iterations/2026-07-02d_jsr223-mainkts-bypass-rediscovery.md`.
