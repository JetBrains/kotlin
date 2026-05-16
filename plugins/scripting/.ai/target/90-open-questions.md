# Target — Open Questions

> **When to consult**: before committing to a design answer or claiming a Q* as a task. Q1–Q12 are referenceable IDs; sub-questions Q5a–e, Q10a–f are individually delegate-able.
> **Cache lifetime**: mutable-per-iteration
> **Last verified**: 2026-05-16

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

- Status: in-design (umbrella; per-sub TBD)
- Owner: unassigned
- YT: — (umbrella)
- Target doc: [`40-jsr223-target.md#remote-out-of-process-compilation`](40-jsr223-target.md)
- Last touched: 2026-05-16

**Settled**: stateless snippet compilation (snippet artifacts = class files + sidecar metadata). At least one IntelliJ consumer relies on out-of-process JSR-223 compilation today.

| Sub | Question | Status | Owner | YT | Last touched |
|---|---|---|---|---|---|
| Q5a | Reconstruction feasibility: can `FirReplSnippetSymbol` + `FirReplSnippetResolveExtension.getSnippetScope` be implemented over symbols rebuilt from on-disk class metadata + sidecar? | open — prototype needed | unassigned | — | 2026-05-16 |
| Q5b | Sidecar format (JSON / proto / hand-rolled binary) + versioning strategy | open | unassigned | — | 2026-05-16 |
| Q5c | Performance: O(N²) FIR reconstruction risk for long sessions; caller-side caching strategy? | open | unassigned | — | 2026-05-16 |
| Q5d | Transport: BTA `CompileReplSnippetOperation` vs direct in-process embedding (post IntelliJ-platform-dep cleanup) — probably both eventually | in-design | unassigned | — | 2026-05-16 |
| Q5e | Migration window: K1 daemon bridge breaks before stateless lands; IntelliJ consumer pin to a Kotlin version during transition? | blocked on Q5a + step 3 prototype | unassigned | — | 2026-05-16 |

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
| Q10a | DSL naming: `inferImplicitSnippetsBefore` vs `prependSnippets` vs `additionalSnippetsBefore` | in-design — pick during step 1 impl | unassigned | — | 2026-05-16 |
| Q10b | Implicit-snippet tagging in `FirReplHistoryProvider`: needs an EP "implicit" tag, or caller-side bookkeeping? | open | unassigned | — | 2026-05-16 |
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
