# Kotlin/Native Lowerings Applied After IR Linking

This file lists, in execution order, every lowering invoked by [`List<BackendJobFragment>.runAllLowerings`](TopLevelPhases.kt#L142) — i.e. every lowering the Kotlin/Native backend applies after IR linking, before code generation.

Helper functions ([`getLoweringsUpToAndIncludingSyntheticAccessors`](NativeLoweringPhases.kt#L611) and [`NativeSecondStageCompilationConfig.getLoweringsAfterInlining`](NativeLoweringPhases.kt#L624)) are expanded inline. Validation phases are included because they are part of the same pipeline; they are marked `[validate]`. Phases gated by `.takeIf`/`.takeUnless` are marked `[conditional]`.

> Note: lowerings run per file (not per module). The pipeline contains "synchronization points" where every fragment is brought to the same stage before continuing.

## Execution order
1. [`validateIrBeforeLowering`](NativeLoweringPhases.kt#L77) — `[validate]`
2. [`checkInlineCallCyclesPhase`](NativeLoweringPhases.kt#L82) — `[validate]`

   --- *KLIB Common Lowerings Prefix begins* (call site: [TopLevelPhases.kt#L166](TopLevelPhases.kt#L166), via [`getLoweringsUpToAndIncludingSyntheticAccessors()`](NativeLoweringPhases.kt#L611)) ---

3. [`testProcessorPhase`](NativeLoweringPhases.kt#L279)
4. [`upgradeCallableReferencesPhase`](NativeLoweringPhases.kt#L143)
5. [`assertionWrapperPhase`](NativeLoweringPhases.kt#L585)
6. [`lateinitPhase`](NativeLoweringPhases.kt#L154)
7. [`sharedVariablesPhase`](NativeLoweringPhases.kt#L159)
8. [`extractLocalClassesFromInlineBodies`](NativeLoweringPhases.kt#L165)
9. [`arrayConstructorPhase`](NativeLoweringPhases.kt#L148)
10. [`inlineOnlyPrivateFunctionsPhase`](NativeLoweringPhases.kt#L357)
11. [`outerThisSpecialAccessorInInlineFunctionsPhase`](NativeLoweringPhases.kt#L362)
12. [`syntheticAccessorGenerationPhase`](NativeLoweringPhases.kt#L368)

   --- *Synchronization point* ---

13. [`validateIrAfterInliningOnlyPrivateFunctions`](NativeLoweringPhases.kt#L88) — `[validate]`
14. [`inlineAllFunctionsPhase`](NativeLoweringPhases.kt#L377) — *mutates multiple files; all preceding lowerings must already be applied*
15. [`specialObjCValidationPhase`](NativeLoweringPhases.kt#L412) — `[validate]`
16. [`redundantCastsRemoverPhase`](NativeLoweringPhases.kt#L596)

   --- *KLIB Common Lowerings Prefix ends* ---

17. [`validateIrAfterInliningAllFunctions`](NativeLoweringPhases.kt#L101) — `[validate]`
18. [`constEvaluationPhase`](NativeLoweringPhases.kt#L602)

   --- *Post-inlining lowerings begin* (call site: [TopLevelPhases.kt#L174](TopLevelPhases.kt#L174), via [`config.getLoweringsAfterInlining()`](NativeLoweringPhases.kt#L624)) ---

19. [`reifiedFunctionLowering`](NativeLoweringPhases.kt#L382)
20. [`typeOfProcessingLowering`](NativeLoweringPhases.kt#L387)
21. [`specializeSharedVariableBoxes`](NativeLoweringPhases.kt#L392)
22. [`interopPhase`](NativeLoweringPhases.kt#L398)
23. [`specialInteropIntrinsicsPhase`](NativeLoweringPhases.kt#L406)
24. [`initTestsPhase`](NativeLoweringPhases.kt#L284)
25. [`dumpTestsPhase`](NativeLoweringPhases.kt#L289) — `[conditional: GENERATE_TEST_RUNNER != NONE]`
26. [`removeExpectDeclarationsPhase`](NativeLoweringPhases.kt#L127)
27. [`stripTypeAliasDeclarationsPhase`](NativeLoweringPhases.kt#L132)
28. [`assertionRemoverPhase`](NativeLoweringPhases.kt#L590)
29. [`volatilePhase`](NativeLoweringPhases.kt#L241)
30. [`delegatedPropertyOptimizationPhase`](NativeLoweringPhases.kt#L294)
31. [`propertyReferencePhase`](NativeLoweringPhases.kt#L300)
32. [`functionReferencePhase`](NativeLoweringPhases.kt#L307)
33. [`singleAbstractMethodPhase`](NativeLoweringPhases.kt#L337)
34. [`postInlinePhase`](NativeLoweringPhases.kt#L171)
35. [`contractsDslRemovePhase`](NativeLoweringPhases.kt#L176)
36. [`annotationImplementationPhase`](NativeLoweringPhases.kt#L137)
37. [`rangeContainsLoweringPhase`](NativeLoweringPhases.kt#L263)
38. [`enumConstructorsPhase`](NativeLoweringPhases.kt#L209)
39. [`initializersPhase`](NativeLoweringPhases.kt#L214)
40. [`inventNamesForInteropBridgesPhase`](NativeLoweringPhases.kt#L220)
41. [`inventNamesForLocalClasses`](NativeLoweringPhases.kt#L561)
42. [`inventNamesForLocalFunctions`](NativeLoweringPhases.kt#L566)
43. [`localFunctionsPhase`](NativeLoweringPhases.kt#L225)
44. [`tailrecPhase`](NativeLoweringPhases.kt#L235)
45. [`finallyBlocksPhase`](NativeLoweringPhases.kt#L273)
46. [`computeTypesPhase`](NativeLoweringPhases.kt#L503) — *inliner erases generics; this restores some info and simplifies IR*
47. [`forLoopsPhase`](NativeLoweringPhases.kt#L181)
48. [`flattenStringConcatenationPhase`](NativeLoweringPhases.kt#L187)
49. [`stringConcatenationPhase`](NativeLoweringPhases.kt#L192)
50. [`stringConcatenationTypeNarrowingPhase`](NativeLoweringPhases.kt#L198) — `[conditional: optimizationsEnabled]`
51. [`defaultParameterExtentPhase`](NativeLoweringPhases.kt#L247)
52. [`innerClassPhase`](NativeLoweringPhases.kt#L257)
53. [`dataClassesPhase`](NativeLoweringPhases.kt#L268)
54. [`ifNullExpressionsFusionPhase`](NativeLoweringPhases.kt#L525)
55. [`staticCallableReferenceOptimizationPhase`](NativeLoweringPhases.kt#L312)
56. [`enumWhenPhase`](NativeLoweringPhases.kt#L318)
57. [`enumClassPhase`](NativeLoweringPhases.kt#L324)
58. [`enumUsagePhase`](NativeLoweringPhases.kt#L330)
59. [`varargPhase`](NativeLoweringPhases.kt#L418)
60. [`kotlinNothingValueExceptionPhase`](NativeLoweringPhases.kt#L204)
61. [`coroutinesPhase`](NativeLoweringPhases.kt#L424)
62. [`coroutinesLivenessAnalysisPhase`](NativeLoweringPhases.kt#L445) — *more optimal; either this or the fallback could be turned off*
63. [`coroutinesLivenessAnalysisFallbackPhase`](NativeLoweringPhases.kt#L439) — *simple fallback*
64. [`expressionBodyTransformPhase`](NativeLoweringPhases.kt#L514)
65. [`objectClassesPhase`](NativeLoweringPhases.kt#L580)
66. [`staticInitializersPhase`](NativeLoweringPhases.kt#L519)
67. [`computeTypesPhase`](NativeLoweringPhases.kt#L503) — *2nd run; also corrects type inaccuracies introduced by earlier lowerings*
68. [`removeCastsFromNothing`](NativeLoweringPhases.kt#L343)
69. [`optimizeCastsPhase`](NativeLoweringPhases.kt#L509) — `[conditional: genericSafeCasts]`
70. [`typeOperatorPhase`](NativeLoweringPhases.kt#L467)
71. [`builtinOperatorPhase`](NativeLoweringPhases.kt#L348)
72. [`bridgesPhase`](NativeLoweringPhases.kt#L473)
73. [`exportInternalAbiPhase`](NativeLoweringPhases.kt#L530) — `[conditional: produce.isCache]`
74. [`useInternalAbiPhase`](NativeLoweringPhases.kt#L571)
75. [`eraseGenericCallsReturnTypesPhase`](NativeLoweringPhases.kt#L482)
76. [`autoboxPhase`](NativeLoweringPhases.kt#L487)
77. [`constructorsLoweringPhase`](NativeLoweringPhases.kt#L493)
78. [`returnsInsertionPhase`](NativeLoweringPhases.kt#L535)
79. [`lowerCastsPhase`](NativeLoweringPhases.kt#L498) — `[conditional: !optimizationsEnabled]`

   --- *Post-inlining lowerings end* ---

80. [`validateIrAfterLowering`](NativeLoweringPhases.kt#L117) — `[validate]`

   --- *`finalizeLowerings` runs `mergeDependencies` — not a lowering, so omitted from the count* ---

## Summary

- **Lowering passes (non-validation):** ~70 (a few are conditional or run twice)
- **Validation passes:** 6 (`validateIrBeforeLowering`, `checkInlineCallCyclesPhase`, `validateIrAfterInliningOnlyPrivateFunctions`, `specialObjCValidationPhase`, `validateIrAfterInliningAllFunctions`, `validateIrAfterLowering`)
- **Inlining boundary:** `inlineAllFunctionsPhase` (#14) — everything before it must be applied in any file it might inline from; the surrounding `validateIr*` phases enforce this