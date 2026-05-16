# Target — Open Questions

Items needing brainstorm before they can be acted on.

## Q1. ~~LightTree path for `FirScript`~~ — resolved

**Resolved**: scripts already use LT exclusively on the K2 path. See `ScriptJvmK2CompilerImpl` + `convertToFirViaLightTree` + `LightTreeRawFirDeclarationBuilder.buildScript()`. No work needed.

## Q2. LightTree path for `FirReplSnippet` — KT-83498

Now a tracked work item, not an open question. `K2ReplCompiler.kt:351-359` is hybrid today; KT-83498 makes it LT-only. Open sub-questions:

- Priority: how soon? Blocks "fully PSI-free K2 scripting" claim and unblocks PSI-free JSR-223 embedding.
- Owner / scope: estimate medium. Touches `K2ReplCompiler`, LT builder (if a `markAsReplSnippet`-equivalent is needed), and `FirReplSnippetConfiguratorExtensionImpl.kt:173`.
- Shape: align with `ScriptJvmK2CompilerImpl` (`convertToFir` lambda) for symmetry, or keep `K2ReplCompiler` simpler with hardwired LT?

## Q3. ~~`scripting-ide-services` — delete or salvage?~~ — resolved

**Resolved**: delete confirmed. Future reimplementation possible in a different form, definitely without K1.

## Q4. ~~`scripting-ide-common` — what stays?~~ — resolved

**Resolved**: delete entirely confirmed. Future reimplementation possible in a different form, definitely without K1.

## Q5. JSR-223 remote compilation — stateless design

**Settled**: not "drop". At least one IntelliJ consumer relies on it (IntelliJ cannot host the Kotlin compiler in-process today). Target: **stateless snippet compilation** (snippet artifacts = class files + sidecar metadata). See [40-jsr223-target.md](40-jsr223-target.md#remote-out-of-process-compilation).

Remaining open sub-questions:

- **Reconstruction feasibility**: can `FirReplSnippetSymbol` + `FirReplSnippetResolveExtension.getSnippetScope` be implemented over symbols rebuilt from on-disk class metadata + sidecar? Needs prototype.
- **Sidecar format**: JSON, protobuf, or hand-rolled binary? Versioning strategy?
- **Performance**: O(N²) FIR reconstruction is a risk for long sessions; caller-side caching strategy?
- **Transport**: when the stateless core lands, do we expose it via Build Tools API (`CompileReplSnippetOperation`) or rely on direct in-process embedding (post IntelliJ-platform-dep cleanup)? Probably both, eventually.
- **Migration window**: the K1 daemon bridge will break before the stateless replacement lands. How wide is the gap? Can IntelliJ consumer pin to a Kotlin compiler version during transition?

## Q6. Classpath-based script definition discovery (KT-82551)

`META-INF/kotlin/script/templates/*.classname` markers are deprecated, but no successor exists. `kotlin-main-kts` and third-party defs depend on it.

| Option | Description |
|---|---|
| Un-deprecate, document as the SPI | Cheapest; accepts current contract |
| Replace with `ServiceLoader<ScriptDefinitionContributor>` SPI | Modern Java SPI; requires definition modules to adapt |
| Keep deprecated forever | Status quo; bad signal |

Default: un-deprecate + document, plan SPI replacement separately.

## Q7. ~~`libraries/scripting/intellij` — move or delete?~~ — resolved

**Resolved**: KEEP. Used by IntelliJ plugin authors to wire custom-scripts support. Not a candidate for removal or relocation.

## Q8. `IrScript` schema: drop K1-only fields

`providedProperties`, `providedPropertiesParameters` are unused on K2. After K1 frontend retires, regen `IrScript` without them.

- **Side question**: should K2 actually unify provided properties + explicit call parameters (current K2 behavior) or split them back out for clarity? Argues for keeping the field but document its K2 semantics.

## Q9. Single configurator extension vs split

`FirScript*` and `FirReplSnippet*` have separate but parallel sets of:
- Configurator (raw-fir)
- Resolution config / resolve extension
- Fir2Ir configurator

→ 6 EPs total (3 for script, 3 for snippet). Reasonable, because the shapes diverge. Open question: unify any of them? Probably not.

## Q10. K2 binding semantics in REPL — settled, sub-questions remain

**Settled**: pursue **Option D** in [40-jsr223-target.md](40-jsr223-target.md) — implicit-snippets refinement-DSL callback + a JSR-223 binding configurator that emits a delegating-property snippet on binding diffs.

Open sub-questions:

- **DSL naming**: `inferImplicitSnippetsBefore` vs `prependSnippets` vs `additionalSnippetsBefore` — pick during impl.
- **Implicit-snippet tagging in `FirReplHistoryProvider`**: does the history provider need an "implicit" tag in its EP so callers can filter user-only enumeration? Or is filtering caller-side bookkeeping (e.g. JSR-223 engine tracks its own list)?
- **Removal semantics**: when a binding name is removed from `Bindings`, what does the next snippet emit? Shadowing snippet with a "removed" marker, or rely on delegate-throws-at-access? Decide during prototyping.
- **Type stability**: if a binding's runtime type changes between calls, do we re-emit a new delegating property (shadowing the old) or fail? Probably re-emit; confirm.
- **Bootstrap timing**: canonical `bindings` accessor is emitted once on the first call that has a non-empty `Bindings`. What if `Bindings` is set, accessed, then cleared? Implementation detail; confirm during impl.
- **Composability with other handlers**: order of handler invocation across multiple definitions installed on one engine — registration order, sorted by priority key, or undefined?

## Q11. Public stability of `JvmScriptCompiler.createLegacy()` etc.

These are not annotated `@SinceKotlin` / `@ExperimentalApi` everywhere. Need to confirm we can remove them without a deprecation cycle, or budget the cycle in.

## Q12. Generated test runners

`plugins/scripting/scripting-tests/` includes generated runners (`*TestGenerated.java`). After deletions, re-run `./gradlew generateTests`. Confirm nothing else generates scripting-related test classes outside this module.
