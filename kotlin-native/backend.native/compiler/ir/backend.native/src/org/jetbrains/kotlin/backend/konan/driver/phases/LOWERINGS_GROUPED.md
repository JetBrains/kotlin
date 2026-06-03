# Kotlin/Native Lowerings — Grouped by Locality for Pass Fusion

Companion to [LOWERINGS.md](LOWERINGS.md). Same set of lowerings (everything inside [`List<BackendJobFragment>.runAllLowerings`](TopLevelPhases.kt#L142)), but classified along 4 locality dimensions and then grouped into **candidate buckets** — runs of consecutive lowerings that could plausibly share a single IR tree walk per file (per-element fusion) instead of each being a full pass over the file.

> **Caveat.** Classifications are based on strict reading of each lowering's implementation, but a few are approximate (marked `[?]`). Bucket membership is a *fusion candidate* — proof of correctness requires checking that none of the in-bucket lowerings reads IR shape that an earlier in-bucket lowering would have invalidated mid-element.

## Dimensions

For each lowering we recorded four values:

| Dimension | Values |
|-----------|--------|
| **visit_scope** | `expression` · `body` · `member` · `class` · `file` — the IR element each invocation processes one-at-a-time |
| **outward_writes** | `none` · `in-place` · `local-var` · `class-member` · `file-member` · `cross-file` · `module-meta` — what mutations escape the visit unit |
| **outward_reads** | `none` · `intrinsics` · `enclosing-fn` · `enclosing-class` · `whole-file` · `cross-file` — what context the lowering needs beyond the visit unit |
| **phase_barrier** | `none` · `pre-pass` · `post-pass` · `produces-symbols` · `must-finish-globally` — does this lowering require a sync point before/after? |

### Bucketing rules

Two lowerings can share a bucket iff:
1. Their visit scopes nest cleanly (broader scope drives the iteration; inner scopes ride along).
2. No `must-finish-globally` between them (cross-file barriers like inlining).
3. No `produces-symbols` whose output a later in-bucket lowering would need to re-walk.
4. No `cross-file` read in the middle of a bucket whose producer hasn't completed file-wide.

## Classification table (execution order)

| # | Phase | visit_scope | outward_writes | outward_reads | phase_barrier |
|---|-------|-------------|----------------|---------------|---------------|
| 1 | [`validateIrBeforeLowering`](NativeLoweringPhases.kt#L77) | file | none | whole-file | none |
| 2 | [`checkInlineCallCyclesPhase`](NativeLoweringPhases.kt#L82) | file | none | cross-file | pre-pass |
| 3 | [`testProcessorPhase`](NativeLoweringPhases.kt#L279) | file | file-member | whole-file | pre-pass, produces-symbols |
| 4 | [`upgradeCallableReferencesPhase`](NativeLoweringPhases.kt#L143) | expression | in-place [?] | enclosing-fn | none |
| 5 | [`assertionWrapperPhase`](NativeLoweringPhases.kt#L585) | expression | in-place | intrinsics | none |
| 6 | [`lateinitPhase`](NativeLoweringPhases.kt#L154) | expression | class-member, in-place | enclosing-class | none |
| 7 | [`sharedVariablesPhase`](NativeLoweringPhases.kt#L159) | body | local-var | enclosing-fn | pre-pass |
| 8 | [`extractLocalClassesFromInlineBodies`](NativeLoweringPhases.kt#L165) | body | local-var | enclosing-fn | none |
| 9 | [`arrayConstructorPhase`](NativeLoweringPhases.kt#L148) | expression | local-var | intrinsics | none |
| 10 | [`inlineOnlyPrivateFunctionsPhase`](NativeLoweringPhases.kt#L357) | body | cross-file [?] | cross-file | **must-finish-globally** |
| 11 | [`outerThisSpecialAccessorInInlineFunctionsPhase`](NativeLoweringPhases.kt#L362) | file | class-member | enclosing-class | post-pass, produces-symbols |
| 12 | [`syntheticAccessorGenerationPhase`](NativeLoweringPhases.kt#L368) | file | class-member | whole-file | post-pass, produces-symbols |
| 13 | [`validateIrAfterInliningOnlyPrivateFunctions`](NativeLoweringPhases.kt#L88) | file | none | whole-file | none |
| 14 | [`inlineAllFunctionsPhase`](NativeLoweringPhases.kt#L377) | body | cross-file | cross-file | **must-finish-globally** |
| 15 | [`specialObjCValidationPhase`](NativeLoweringPhases.kt#L412) | file | none | whole-file | none |
| 16 | [`redundantCastsRemoverPhase`](NativeLoweringPhases.kt#L596) | expression | in-place | none | none |
| 17 | [`validateIrAfterInliningAllFunctions`](NativeLoweringPhases.kt#L101) | file | none | whole-file | none |
| 18 | [`constEvaluationPhase`](NativeLoweringPhases.kt#L602) | file | in-place | whole-file | none |
| 19 | [`reifiedFunctionLowering`](NativeLoweringPhases.kt#L382) | member | in-place | intrinsics | none |
| 20 | [`typeOfProcessingLowering`](NativeLoweringPhases.kt#L387) | expression | in-place | intrinsics, enclosing-fn | none |
| 21 | [`specializeSharedVariableBoxes`](NativeLoweringPhases.kt#L392) | body | in-place | intrinsics | none |
| 22 | [`interopPhase`](NativeLoweringPhases.kt#L398) | file | class-member, file-member, in-place | intrinsics, whole-file | produces-symbols |
| 23 | [`specialInteropIntrinsicsPhase`](NativeLoweringPhases.kt#L406) | expression | in-place | intrinsics | none |
| 24 | [`initTestsPhase`](NativeLoweringPhases.kt#L284) | file | file-member, in-place | intrinsics | none |
| 25 | [`dumpTestsPhase`](NativeLoweringPhases.kt#L289) | file | none *(writes external file)* | whole-file, intrinsics | none |
| 26 | [`removeExpectDeclarationsPhase`](NativeLoweringPhases.kt#L127) | file | in-place | none | none |
| 27 | [`stripTypeAliasDeclarationsPhase`](NativeLoweringPhases.kt#L132) | member | in-place | none | none |
| 28 | [`assertionRemoverPhase`](NativeLoweringPhases.kt#L590) | expression | in-place | intrinsics | none |
| 29 | [`volatilePhase`](NativeLoweringPhases.kt#L241) | file | class-member, file-member, in-place | intrinsics, enclosing-class | produces-symbols |
| 30 | [`delegatedPropertyOptimizationPhase`](NativeLoweringPhases.kt#L294) | file | file-member, in-place | intrinsics, whole-file | produces-symbols |
| 31 | [`propertyReferencePhase`](NativeLoweringPhases.kt#L300) | expression | in-place | intrinsics | none |
| 32 | [`functionReferencePhase`](NativeLoweringPhases.kt#L307) | expression | file-member, in-place | intrinsics, enclosing-fn | produces-symbols |
| 33 | [`singleAbstractMethodPhase`](NativeLoweringPhases.kt#L337) | expression | file-member, in-place | intrinsics, enclosing-class | produces-symbols |
| 34 | [`postInlinePhase`](NativeLoweringPhases.kt#L171) | body | in-place | intrinsics, enclosing-fn | none |
| 35 | [`contractsDslRemovePhase`](NativeLoweringPhases.kt#L176) | member | in-place | none | none |
| 36 | [`annotationImplementationPhase`](NativeLoweringPhases.kt#L137) | expression | class-member, file-member, in-place | intrinsics, whole-file | produces-symbols |
| 37 | [`rangeContainsLoweringPhase`](NativeLoweringPhases.kt#L263) | body | local-var, in-place | intrinsics | none |
| 38 | [`enumConstructorsPhase`](NativeLoweringPhases.kt#L209) | class | class-member | enclosing-class | none |
| 39 | [`initializersPhase`](NativeLoweringPhases.kt#L214) | class | class-member | enclosing-class | none |
| 40 | [`inventNamesForInteropBridgesPhase`](NativeLoweringPhases.kt#L220) | file | in-place [?], module-meta | intrinsics | none |
| 41 | [`inventNamesForLocalClasses`](NativeLoweringPhases.kt#L561) | file | in-place | whole-file | none |
| 42 | [`inventNamesForLocalFunctions`](NativeLoweringPhases.kt#L566) | body | in-place | enclosing-class | none |
| 43 | [`localFunctionsPhase`](NativeLoweringPhases.kt#L225) | body | class-member | enclosing-fn | none |
| 44 | [`tailrecPhase`](NativeLoweringPhases.kt#L235) | body | local-var | enclosing-fn | none |
| 45 | [`finallyBlocksPhase`](NativeLoweringPhases.kt#L273) | body | local-var | enclosing-fn | none |
| 46 | [`computeTypesPhase`](NativeLoweringPhases.kt#L503) *(1st)* | body | in-place | enclosing-fn | none |
| 47 | [`forLoopsPhase`](NativeLoweringPhases.kt#L181) | body | local-var | intrinsics | none |
| 48 | [`flattenStringConcatenationPhase`](NativeLoweringPhases.kt#L187) | expression | in-place | intrinsics | none |
| 49 | [`stringConcatenationPhase`](NativeLoweringPhases.kt#L192) | expression | local-var | intrinsics | none |
| 50 | [`stringConcatenationTypeNarrowingPhase`](NativeLoweringPhases.kt#L198) | expression | local-var | intrinsics | none |
| 51 | [`defaultParameterExtentPhase`](NativeLoweringPhases.kt#L247) | member | class-member | enclosing-class | produces-symbols |
| 52 | [`innerClassPhase`](NativeLoweringPhases.kt#L257) | class | class-member | enclosing-class | none |
| 53 | [`dataClassesPhase`](NativeLoweringPhases.kt#L268) | expression | in-place | intrinsics | none |
| 54 | [`ifNullExpressionsFusionPhase`](NativeLoweringPhases.kt#L525) | expression | local-var | intrinsics | none |
| 55 | [`staticCallableReferenceOptimizationPhase`](NativeLoweringPhases.kt#L312) | expression | in-place | intrinsics | none |
| 56 | [`enumWhenPhase`](NativeLoweringPhases.kt#L318) | expression | in-place | enclosing-class | none |
| 57 | [`enumClassPhase`](NativeLoweringPhases.kt#L324) | class | class-member | enclosing-class | produces-symbols |
| 58 | [`enumUsagePhase`](NativeLoweringPhases.kt#L330) | expression | in-place | enclosing-class | none |
| 59 | [`varargPhase`](NativeLoweringPhases.kt#L418) | member | in-place | intrinsics | none |
| 60 | [`kotlinNothingValueExceptionPhase`](NativeLoweringPhases.kt#L204) | body | in-place | intrinsics | none |
| 61 | [`coroutinesPhase`](NativeLoweringPhases.kt#L424) | file | file-member | cross-file | produces-symbols |
| 62 | [`coroutinesLivenessAnalysisPhase`](NativeLoweringPhases.kt#L445) | body | module-meta | enclosing-fn | none |
| 63 | [`coroutinesLivenessAnalysisFallbackPhase`](NativeLoweringPhases.kt#L439) | body | module-meta | enclosing-fn | none |
| 64 | [`expressionBodyTransformPhase`](NativeLoweringPhases.kt#L514) | member | in-place | none | none |
| 65 | [`objectClassesPhase`](NativeLoweringPhases.kt#L580) | class | class-member | enclosing-class | produces-symbols |
| 66 | [`staticInitializersPhase`](NativeLoweringPhases.kt#L519) | member | class-member, file-member | enclosing-class | produces-symbols |
| 67 | [`computeTypesPhase`](NativeLoweringPhases.kt#L503) *(2nd)* | body | in-place | enclosing-fn | none |
| 68 | [`removeCastsFromNothing`](NativeLoweringPhases.kt#L343) | expression | in-place | none | none |
| 69 | [`optimizeCastsPhase`](NativeLoweringPhases.kt#L509) | body | in-place | enclosing-fn | none |
| 70 | [`typeOperatorPhase`](NativeLoweringPhases.kt#L467) | expression | in-place | intrinsics | none |
| 71 | [`builtinOperatorPhase`](NativeLoweringPhases.kt#L348) | expression | in-place | intrinsics | none |
| 72 | [`bridgesPhase`](NativeLoweringPhases.kt#L473) | class | class-member | enclosing-class | produces-symbols |
| 73 | [`exportInternalAbiPhase`](NativeLoweringPhases.kt#L530) | file | file-member | whole-file | produces-symbols |
| 74 | [`useInternalAbiPhase`](NativeLoweringPhases.kt#L571) | expression | in-place | cross-file | post-pass |
| 75 | [`eraseGenericCallsReturnTypesPhase`](NativeLoweringPhases.kt#L482) | body | in-place | intrinsics | none |
| 76 | [`autoboxPhase`](NativeLoweringPhases.kt#L487) | file | class-member, file-member | enclosing-class | produces-symbols |
| 77 | [`constructorsLoweringPhase`](NativeLoweringPhases.kt#L493) | class | class-member | enclosing-class | produces-symbols |
| 78 | [`returnsInsertionPhase`](NativeLoweringPhases.kt#L535) | member | in-place | intrinsics | none |
| 79 | [`lowerCastsPhase`](NativeLoweringPhases.kt#L498) | body | in-place | intrinsics | none |
| 80 | [`validateIrAfterLowering`](NativeLoweringPhases.kt#L117) | file | none | whole-file | none |

## Buckets

Each bucket = a candidate single-walk pass. Lowerings inside one bucket could be applied in-order per IR element (e.g. per file, per body) instead of as separate full passes. Hard barriers (must-finish-globally) and produces-symbols boundaries close buckets.

### Pre-inlining

**B1 — Pre-inlining validation gate** *(read-only file walk)*
- #1 `validateIrBeforeLowering`, #2 `checkInlineCallCyclesPhase`
- *Both file-scope, zero IR mutation; `checkInlineCallCyclesPhase`'s pre-pass is its own concern (build call graph). Single read-only walk over the module is the natural fusion.*

**B2 — Test suite synthesis** *(standalone)*
- #3 `testProcessorPhase`
- *File-scope, pre-pass + produces test-suite classes. Subsequent walks must see the synthesized classes, so closes its own bucket.*

**B3 — Expression-level pre-inlining rewrites**
- #4 `upgradeCallableReferencesPhase`, #5 `assertionWrapperPhase`, #6 `lateinitPhase`
- *All expression-scope, mostly in-place. `lateinitPhase`'s `class-member` write is confined to the same property it visits (backing field mutation), so it doesn't spill across the bucket.*

**B4 — Body-level pre-inlining rewrites**
- #7 `sharedVariablesPhase`, #8 `extractLocalClassesFromInlineBodies`, #9 `arrayConstructorPhase`
- *Body/expression-scope, `local-var` writes. `sharedVariablesPhase`'s pre-pass is per-body (not file-wide), so it doesn't break inter-body fusion. `arrayConstructorPhase` (expression) rides inside the body walk.*

### Inlining sync points

🛑 **#10 `inlineOnlyPrivateFunctionsPhase`** — `must-finish-globally`, mutates other files. Hard sync.

**B5 — Accessor generation** *(shared infrastructure)*
- #11 `outerThisSpecialAccessorInInlineFunctionsPhase`, #12 `syntheticAccessorGenerationPhase`
- *Both file-scope with a post-pass that adds accessors to parents via the same `KlibSyntheticAccessorGenerator`. Strong fusion candidate: single file walk, single batched accessor-emission step.*

🛑 **#13 `validateIrAfterInliningOnlyPrivateFunctions`** — single read-only walk; could absorb into a future "post-private-inline validate" bucket if combined with anything similar (none here).

🛑 **#14 `inlineAllFunctionsPhase`** — hard sync.

### Post-inlining cleanup

**B6 — Post-inlining cleanup**
- #15 `specialObjCValidationPhase`, #16 `redundantCastsRemoverPhase`
- *Read-only ObjC check (file scope) fuses with expression-level cast removal in one walk.*

🛑 **#17 `validateIrAfterInliningAllFunctions`** — standalone validation gate.

🛑 **#18 `constEvaluationPhase`** — uses `IrInterpreter` over the whole file; opaque, doesn't fuse with transformer-based passes.

### Lowering body (the long run)

**B7 — In-place rewrites (round 1)**
- #19 `reifiedFunctionLowering`, #20 `typeOfProcessingLowering`, #21 `specializeSharedVariableBoxes`
- *Mixed member/expression/body scope, all `in-place`. Can share one per-file walk.*

🛑 **#22 `interopPhase`** — file-scope, produces ObjC stubs + top-level init fields. Hard sync (the new file-members must be visible to all later lowerings).

**B8** — #23 `specialInteropIntrinsicsPhase` *(expression, in-place — alone between two sync points)*

🛑 **#24 `initTestsPhase`** — file-member writes; the new init function must exist before subsequent lowerings touch it.

**B9 — File-level subtractive/diagnostic walk**
- #25 `dumpTestsPhase` (scans only, writes external), #26 `removeExpectDeclarationsPhase`, #27 `stripTypeAliasDeclarationsPhase`, #28 `assertionRemoverPhase`
- *Mostly subtractive or trivial expression rewrites. Fusible into one walk.*

🛑 **#29 `volatilePhase`** — generates CAS/RMW functions per volatile field.
🛑 **#30 `delegatedPropertyOptimizationPhase`** — lifts KProperty refs into top-level cached fields.

**B10** — #31 `propertyReferencePhase` *(expression, in-place — between two produces-symbols barriers)*

🛑 **#32 `functionReferencePhase`** — synthesizes anonymous classes per `IrRichFunctionReference`.
🛑 **#33 `singleAbstractMethodPhase`** — synthesizes SAM wrapper classes.

**B11 — Body-level cleanup**
- #34 `postInlinePhase`, #35 `contractsDslRemovePhase`
- *postInline (body, in-place) + contracts removal (member, subtractive). Compatible.*

🛑 **#36 `annotationImplementationPhase`** — synthesizes annotation impl classes.

**B12** — #37 `rangeContainsLoweringPhase` *(body, local-var — alone)*

### Class-level lowerings

**B13 — Enum / initializer class transforms**
- #38 `enumConstructorsPhase`, #39 `initializersPhase`
- *Both `ClassLoweringPass` with `class-member` writes. Walk every class once, apply both.*

**B14 — Naming inventors**
- #40 `inventNamesForInteropBridgesPhase`, #41 `inventNamesForLocalClasses`, #42 `inventNamesForLocalFunctions`
- *Largely read-only-with-attribute-set; can share a single walk over the file + bodies.*

**B15 — Body lowering long run**
- #43 `localFunctionsPhase`, #44 `tailrecPhase`, #45 `finallyBlocksPhase`, #46 `computeTypesPhase` (1st), #47 `forLoopsPhase`
- *All body-scope. `localFunctionsPhase` extracts locals to the enclosing container — its `class-member` write goes to a different IR element than the body being walked, which is fine within a per-body bucket. `computeTypesPhase` and `forLoopsPhase` ride inside the same walk.*

**B16 — String concatenation chain**
- #48 `flattenStringConcatenationPhase`, #49 `stringConcatenationPhase`, #50 `stringConcatenationTypeNarrowingPhase`
- *Three consecutive expression-scope lowerings explicitly chained (each declares the previous as `prerequisite`). Strong fusion candidate: one walk, three rewrite steps per `IrStringConcatenation`.*

🛑 **#51 `defaultParameterExtentPhase`** — produces `$default` stub functions that subsequent lowerings reference.

**B17** — #52 `innerClassPhase` *(class, class-member — alone before expression buckets)*

**B18 — Misc expression cleanups**
- #53 `dataClassesPhase`, #54 `ifNullExpressionsFusionPhase`, #55 `staticCallableReferenceOptimizationPhase`
- *All expression-scope, mostly in-place. Compatible.*

**B19 — Enum lowering chain (per-file ordering inside)**
- #56 `enumWhenPhase`, #57 `enumClassPhase`, #58 `enumUsagePhase`
- *`enumClassPhase` produces VALUES/ENTRIES fields that `enumUsagePhase` reads. Fusible **per file** if the order within the bucket is preserved (which is natural — visit each class, run enumClass; then walk uses). Cannot interleave across files because enum class lowering must finish for file F before usages in F are rewritten.*

**B20** — #59 `varargPhase`, #60 `kotlinNothingValueExceptionPhase` *(member + body, both in-place; fusible)*

🛑 **#61 `coroutinesPhase`** — biggest non-inlining barrier. Chains 4 sub-lowerings; produces coroutine classes with cross-file references.

**B21 — Coroutine liveness annotation**
- #62 `coroutinesLivenessAnalysisPhase`, #63 `coroutinesLivenessAnalysisFallbackPhase`
- *Both body-scope, both write only `module-meta` (annotations on `IrSuspensionPoint`). Either-or in execution (one would suffice for correctness), but if both run they can share a walk.*

**B22** — #64 `expressionBodyTransformPhase` *(member, in-place — alone)*

🛑 **#65 `objectClassesPhase`** — adds singleton instance getter to each object class.
🛑 **#66 `staticInitializersPhase`** — builds `$init_global` / `$init_thread_local` functions.

**B23 — Cast / type-operator cleanup** *(strong fusion candidate)*
- #67 `computeTypesPhase` (2nd), #68 `removeCastsFromNothing`, #69 `optimizeCastsPhase`, #70 `typeOperatorPhase`, #71 `builtinOperatorPhase`
- *Five consecutive body/expression in-place lowerings. No produces-symbols, no cross-file. One walk could apply all five.*

🛑 **#72 `bridgesPhase`** — generates bridge methods + worker bridges (class-member).

🛑 **#73 `exportInternalAbiPhase`** — adds accessor functions for the file's private ABI.
🛑 **#74 `useInternalAbiPhase`** — **cross-file read** of #73's output. Hard ordering: every file's `exportInternalAbiPhase` must finish before any file's `useInternalAbiPhase` starts. Module-wide sync, like inlining.

**B24** — #75 `eraseGenericCallsReturnTypesPhase` *(body, in-place — alone before autoboxPhase)*

🛑 **#76 `autoboxPhase`** — file-scope, builds inline-class accessor functions.
🛑 **#77 `constructorsLoweringPhase`** — splits constructors into alloc + static-init pair.

**B25 — Final cleanup**
- #78 `returnsInsertionPhase`, #79 `lowerCastsPhase`
- *member + body, both in-place.*

🛑 **#80 `validateIrAfterLowering`** — final read-only validation.

## Summary

| | Count |
|---|---|
| Original phases | 80 |
| Buckets identified | 25 |
| Hard sync points (single-phase, can't fuse) | 28 |
| **Best fusion candidates** | B5 (accessors), B16 (string concat), B19 (enum chain), B23 (cast/operator cleanup) |

**Strongest patterns:**
- The two inlining phases (#10, #14) and the cross-cache phases (#73/#74) are absolute module-wide barriers.
- `produces-symbols` is the dominant in-file barrier — roughly 1 in 4 lowerings synthesizes new declarations that subsequent lowerings must see.
- Validation phases are uniformly cheap, read-only, file-scope — every consecutive run of them collapses into one walk.
- The cast/type-operator cleanup (B23) and the string concatenation chain (B16) are the cleanest fusion wins outside the existing prerequisite chains.

## Caveats and `[?]` items

- **#4 `upgradeCallableReferencesPhase`** — synthesizes wrapper `IrSimpleFunction`s, but they live *inside* `IrRichFunctionReference.invokeFunction` (embedded in the replaced expression), not added to enclosing class/file declarations. Classified `in-place`. Some plugin-extracted lambda paths also mutate `IrClass.declarations` (removing used lambdas in `visitClass`) — flagged.
- **#10 `inlineOnlyPrivateFunctionsPhase`** — cross-file writes depend on the `InlineFunctionResolver`. Kotlin/Native's resolver lowers fetched callees in their home file. Other backends may differ.
- **#40 `inventNamesForInteropBridgesPhase`** — renames in place but also registers C stubs into `generationState.cStubsManager`, a module-level side channel. The cStubsManager additions are effectively `module-meta` but don't affect IR structure.
- **#57 `enumClassPhase` → #58 `enumUsagePhase`** — fusion claim assumes per-file ordering is preserved; depends on visiting all enum classes in a file before any enum usage in that file is rewritten.