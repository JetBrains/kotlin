# Target — Open Questions

> **When to consult**: before committing to a design answer or claiming a Q* as a task. Q1–Q12 are referenceable IDs; sub-questions Q5a–e, Q10a–f are individually delegate-able.
> **Cache lifetime**: mutable-per-iteration
> **Last verified**: 2026-07-01 (Q17 resolved)

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
- Last touched: 2026-05-16

Tracked as migration-plan step 2. Sub-questions (priority, shape — `convertToFir` lambda vs hardwired LT) are recorded inline in step 2 "Design notes".

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
- Last touched: 2026-05-16

**Settled**: pursue **Option D** in [`40-jsr223-target.md`](40-jsr223-target.md) — implicit-snippets refinement-DSL callback + a JSR-223 binding configurator that emits a delegating-property snippet on binding diffs.

| Sub | Question | Status | Owner | YT | Last touched |
|---|---|---|---|---|---|
| Q10a | DSL naming: settled as `prependSyntheticSnippets`. See [iterations/2026-05-17_bindings-partial.md](../iterations/2026-05-17_bindings-partial.md) | resolved | unassigned | — | 2026-05-17 |
| Q10b | Implicit-snippet tagging in `FirReplHistoryProvider`: needs an EP "implicit" tag, or caller-side bookkeeping? | **in-design — sidecar-tag direction locked 2026-05-27** ([iteration](../iterations/2026-05-27_stateless-repl-diagnostics-coverage.md)): `SnippetArtifactSidecar.isImplicit: Boolean` (sidecar v2), read surface via `ArtifactBackedFirReplHistoryProvider.isImplicit(symbol)`. Closes outright once a concrete `prependSyntheticSnippets` consumer exercises the read path. | unassigned | — | 2026-05-27 |
| Q10c | Removal semantics: when a binding name is removed, what does the next snippet emit? Shadowing marker vs delegate-throws-at-access | open — decide during prototyping | unassigned | — | 2026-05-16 |
| Q10d | Type stability: if a binding's runtime type changes, re-emit new delegating property (shadow old) vs fail? Probably re-emit; confirm | open — decide during prototyping | unassigned | — | 2026-05-16 |
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

## Q14. JVM-safe binding-name encoding for JSR-223

- Status: open — design needed
- Owner: unassigned
- YT: —
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-05-18

K1 contract escaped JSR-223 binding names that aren't Kotlin identifiers using a `\`-prefixed escape table (`.` → `\,`, `:` → `\!`, ...). On K2 the JVM names checker (`FirJvmNamesChecker.INVALID_CHARS`) rejects `\` along with `.`, `:`, `;`, `[`, `]`, `/`, `<`, `>`, so the K1 escape scheme cannot be honoured. Underscore-only fallbacks collide with the parser rule "Names `_`, `__`, `___`, ... are reserved". See [`../current/80-known-gotchas.md`](../current/80-known-gotchas.md#g8-jvm-identifier-constraints-block---based-binding-name-escaping) (G8).

| Option | Description |
|---|---|
| Prefix-encoded ASCII (e.g. `__dot__`, `__colon__`) | Reversible, JVM-safe; clutters generated source; needs an underscore prefix to dodge the reserved-name rule (e.g. `n__dot__name`). |
| Punycode-style | Reversible, compact, well-specified; verbose for common cases; needs a small Kotlin impl. |
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

## Q16. JSR-223 K2 implicit-receiver strategy

- Status: open — design needed
- Owner: unassigned
- YT: —
- Target doc: [`40-jsr223-target.md`](40-jsr223-target.md)
- Last touched: 2026-05-18

K1 path exposed bindings via helpers receiving `ScriptTemplateWithBindings`. K2 path uses `ScriptContext` as `$$eval`'s implicit receiver, so user-defined `fun ScriptTemplateWithBindings.foo(...)` is unreachable. See [`../current/80-known-gotchas.md`](../current/80-known-gotchas.md#g10-k2-jsr-223-implicit-receiver-is-scriptcontext-not-scripttemplatewithbindings) (G10).

| Option | Description |
|---|---|
| Drop `ScriptTemplateWithBindings` helper API; document the K2 contract as "extension receivers must be on `ScriptContext` or `Bindings`" | Cleanest; breaks existing user code that used the K1 extension shape. |
| Add a second implicit receiver (`ScriptTemplateWithBindings`) on K2 `$$eval` | Backwards-compatible; risk of ambiguous-receiver diagnostics; needs FIR snippet-resolve EP support. |
| Switch the JSR-223 script template entirely (so the K2 `$$eval` receiver IS `ScriptTemplateWithBindings`) | Compatible-by-construction; ripples through `KotlinJsr223DefaultScript` + every snippet's compilation config. |
