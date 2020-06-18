/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testGenerator

import org.jetbrains.kotlin.AbstractDataFlowValueRenderingTest
import org.jetbrains.kotlin.addImport.AbstractAddImportTest
import org.jetbrains.kotlin.addImportAlias.AbstractAddImportAliasTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightClassLoadingTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightClassSanityTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightFacadeClassTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightScriptLoadingTest
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.copyright.AbstractUpdateKotlinCopyrightTest
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesTest
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesWithDisableComponentSearchTest
import org.jetbrains.kotlin.findUsages.AbstractKotlinFindUsagesWithLibraryTest
import org.jetbrains.kotlin.formatter.AbstractFormatterTest
import org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase
import org.jetbrains.kotlin.idea.AbstractExpressionSelectionTest
import org.jetbrains.kotlin.idea.AbstractSmartSelectionTest
import org.jetbrains.kotlin.idea.actions.AbstractGotoTestOrCodeActionTest
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractCodeInsightActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateHashCodeAndEqualsActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateTestSupportMethodActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateToStringActionTest
import org.jetbrains.kotlin.idea.codeInsight.moveUpDown.AbstractMoveLeftRightTest
import org.jetbrains.kotlin.idea.codeInsight.moveUpDown.AbstractMoveStatementTest
import org.jetbrains.kotlin.idea.codeInsight.postfix.AbstractPostfixTemplateProviderTest
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.AbstractSurroundWithTest
import org.jetbrains.kotlin.idea.codeInsight.unwrap.AbstractUnwrapRemoveTest
import org.jetbrains.kotlin.idea.completion.test.*
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractBasicCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionCharFilterTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractKeywordCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractSmartCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractBasicCompletionWeigherTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractSmartCompletionWeigherTest
import org.jetbrains.kotlin.idea.configuration.AbstractGradleConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralKotlinToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralTextToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractTextJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.coverage.AbstractKotlinCoverageOutputFilesTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentAutoImportTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentCompletionHandlerTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentCompletionTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentHighlightingTest
import org.jetbrains.kotlin.idea.debugger.test.*
import org.jetbrains.kotlin.idea.debugger.test.sequence.exec.AbstractSequenceTraceTestCase
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateJavaToLibrarySourceTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToLibrarySourceTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToLibrarySourceTestWithJS
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractClsStubBuilderTest
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractLoadJavaClsStubTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJsDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJvmDecompiledTextTest
import org.jetbrains.kotlin.idea.editor.AbstractMultiLineStringIndentTest
import org.jetbrains.kotlin.idea.editor.backspaceHandler.AbstractBackspaceHandlerTest
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractQuickDocProviderTest
import org.jetbrains.kotlin.idea.filters.AbstractKotlinExceptionFilterTest
import org.jetbrains.kotlin.idea.fir.AbstractFirLazyResolveTest
import org.jetbrains.kotlin.idea.fir.AbstractFirMultiModuleResolveTest
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyWithLibTest
import org.jetbrains.kotlin.idea.highlighter.*
import org.jetbrains.kotlin.idea.imports.AbstractJsOptimizeImportsTest
import org.jetbrains.kotlin.idea.imports.AbstractJvmOptimizeImportsTest
import org.jetbrains.kotlin.idea.index.AbstractKotlinTypeAliasByExpansionShortNameIndexTest
import org.jetbrains.kotlin.idea.inspections.AbstractLocalInspectionTest
import org.jetbrains.kotlin.idea.inspections.AbstractMultiFileLocalInspectionTest
import org.jetbrains.kotlin.idea.intentions.AbstractConcatenatedStringGeneratorTest
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest2
import org.jetbrains.kotlin.idea.intentions.AbstractMultiFileIntentionTest
import org.jetbrains.kotlin.idea.intentions.declarations.AbstractJoinLinesTest
import org.jetbrains.kotlin.idea.internal.AbstractBytecodeToolWindowTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocHighlightingTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocTypingTest
import org.jetbrains.kotlin.idea.maven.AbstractKotlinMavenInspectionTest
import org.jetbrains.kotlin.idea.maven.configuration.AbstractMavenConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.navigation.*
import org.jetbrains.kotlin.idea.parameterInfo.AbstractParameterInfoTest
import org.jetbrains.kotlin.idea.perf.*
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiModuleTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.refactoring.AbstractNameSuggestionProviderTest
import org.jetbrains.kotlin.idea.refactoring.copy.AbstractCopyTest
import org.jetbrains.kotlin.idea.refactoring.copy.AbstractMultiModuleCopyTest
import org.jetbrains.kotlin.idea.refactoring.inline.AbstractInlineTest
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractExtractionTest
import org.jetbrains.kotlin.idea.refactoring.move.AbstractMoveTest
import org.jetbrains.kotlin.idea.refactoring.move.AbstractMultiModuleMoveTest
import org.jetbrains.kotlin.idea.refactoring.pullUp.AbstractPullUpTest
import org.jetbrains.kotlin.idea.refactoring.pushDown.AbstractPushDownTest
import org.jetbrains.kotlin.idea.refactoring.rename.AbstractMultiModuleRenameTest
import org.jetbrains.kotlin.idea.refactoring.rename.AbstractRenameTest
import org.jetbrains.kotlin.idea.refactoring.safeDelete.AbstractMultiModuleSafeDeleteTest
import org.jetbrains.kotlin.idea.refactoring.safeDelete.AbstractSafeDeleteTest
import org.jetbrains.kotlin.idea.repl.AbstractIdeReplCompletionTest
import org.jetbrains.kotlin.idea.resolve.*
import org.jetbrains.kotlin.idea.scratch.AbstractScratchLineMarkersTest
import org.jetbrains.kotlin.idea.scratch.AbstractScratchRunActionTest
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationCompletionTest
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationHighlightingTest
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationNavigationTest
import org.jetbrains.kotlin.idea.script.AbstractScriptDefinitionsOrderTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerLeafGroupingTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerMultiplatformTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerNullnessGroupingTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerTreeTest
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinFileStructureTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiFileHighlightingTest
import org.jetbrains.kotlin.idea.stubs.AbstractResolveByStubTest
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.incremental.AbstractJsProtoComparisonTest
import org.jetbrains.kotlin.jps.incremental.AbstractJvmProtoComparisonTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.nj2k.AbstractTextNewJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.nj2k.inference.common.AbstractCommonConstraintCollectorTest
import org.jetbrains.kotlin.nj2k.inference.mutability.AbstractMutabilityInferenceTest
import org.jetbrains.kotlin.nj2k.inference.nullability.AbstractNullabilityInferenceTest
import org.jetbrains.kotlin.psi.patternMatching.AbstractPsiUnifierTest
import org.jetbrains.kotlin.search.AbstractAnnotatedMembersSearchTest
import org.jetbrains.kotlin.search.AbstractInheritorsSearchTest
import org.jetbrains.kotlin.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.testGenerator.generator.TestGenerator
import org.jetbrains.kotlin.testGenerator.model.*
import org.jetbrains.kotlin.testGenerator.model.Patterns.DIRECTORY
import org.jetbrains.kotlin.testGenerator.model.Patterns.JAVA
import org.jetbrains.kotlin.testGenerator.model.Patterns.KT
import org.jetbrains.kotlin.testGenerator.model.Patterns.KTS
import org.jetbrains.kotlin.testGenerator.model.Patterns.KT_OR_KTS
import org.jetbrains.kotlin.testGenerator.model.Patterns.KT_OR_KTS_WITHOUT_DOTS
import org.jetbrains.kotlin.testGenerator.model.Patterns.KT_WITHOUT_DOTS
import org.jetbrains.kotlin.testGenerator.model.Patterns.TEST
import org.jetbrains.kotlin.testGenerator.model.Patterns.WS_KTS
import org.jetbrains.kotlin.tools.projectWizard.cli.AbstractProjectTemplateBuildFileGenerationTest
import org.jetbrains.kotlin.tools.projectWizard.cli.AbstractYamlBuildFileGenerationTest
import org.jetbrains.kotlin.tools.projectWizard.wizard.AbstractProjectTemplateNewWizardProjectImportTest
import org.jetbrains.kotlin.tools.projectWizard.wizard.AbstractYamlNewWizardProjectImportTest

fun main() {
    System.setProperty("java.awt.headless", "true")
    TestGenerator.write(assembleWorkspace())
}

private fun assembleWorkspace(): TWorkspace = workspace {
    testGroup("jvm-debugger/test") {
        testClass<AbstractKotlinSteppingTest> {
            model("stepping/stepIntoAndSmartStepInto", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepIntoTest", testClassName = "StepInto")
            model("stepping/stepIntoAndSmartStepInto", pattern = KT_WITHOUT_DOTS, testMethodName = "doSmartStepIntoTest", testClassName = "SmartStepInto")
            model("stepping/stepInto", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepIntoTest", testClassName = "StepIntoOnly")
            model("stepping/stepOut", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepOutTest")
            model("stepping/stepOver", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepOverTest")
            model("stepping/filters", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepIntoTest")
            model("stepping/custom", pattern = KT_WITHOUT_DOTS, testMethodName = "doCustomTest")
        }

        testClass<AbstractKotlinEvaluateExpressionTest> {
            model("evaluation/singleBreakpoint", testMethodName = "doSingleBreakpointTest")
            model("evaluation/multipleBreakpoints", testMethodName = "doMultipleBreakpointsTest")
        }

        testClass<AbstractSelectExpressionForDebuggerTest> {
            model("selectExpression", isRecursive = false)
            model("selectExpression/disallowMethodCalls", testMethodName = "doTestWoMethodCalls")
        }

        testClass<AbstractPositionManagerTest> {
            model("positionManager", isRecursive = false, pattern = KT, testClassName = "SingleFile")
            model("positionManager", isRecursive = false, pattern = DIRECTORY, testClassName = "MultiFile")
        }

        testClass<AbstractSmartStepIntoTest> {
            model("smartStepInto")
        }

        testClass<AbstractBreakpointApplicabilityTest> {
            model("breakpointApplicability")
        }

        testClass<AbstractFileRankingTest> {
            model("fileRanking")
        }

        testClass<AbstractAsyncStackTraceTest> {
            model("asyncStackTrace")
        }

        testClass<AbstractCoroutineDumpTest> {
            model("coroutines")
        }

        testClass<AbstractSequenceTraceTestCase> {
            // TODO: implement mapping logic for terminal operations
            model("sequence/streams/sequence", excludedDirectories = listOf("terminal"))
        }

        testClass<AbstractContinuationStackTraceTest> {
            model("continuation")
        }

        testClass<AbstractXCoroutinesStackTraceTest> {
            model("xcoroutines")
        }
    }

    testGroup("idea") {
        testClass<AbstractAdditionalResolveDescriptorRendererTest> {
            model("resolve/additionalLazyResolve")
        }

        testClass<AbstractPartialBodyResolveTest> {
            model("resolve/partialBodyResolve")
        }

        testClass<AbstractPsiCheckerTest> {
            model("checker", isRecursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/scripts", pattern = KTS)
            model("checker/duplicateJvmSignature")
            model("checker/infos", testMethodName = "doTestWithInfos")
            model("checker/diagnosticsMessage")
        }

        testClass<AbstractJavaAgainstKotlinSourceCheckerTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
            model("kotlinAndJavaChecker/javaWithKotlin")
        }

        testClass<AbstractJavaAgainstKotlinSourceCheckerWithoutUltraLightTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
            model("kotlinAndJavaChecker/javaWithKotlin")
        }

        testClass<AbstractJavaAgainstKotlinBinariesCheckerTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
        }

        testClass<AbstractPsiUnifierTest> {
            model("unifier")
        }

        testClass<AbstractCodeFragmentHighlightingTest> {
            model("checker/codeFragments", pattern = KT, isRecursive = false)
            model("checker/codeFragments/imports", testMethodName = "doTestWithImport", pattern = KT)
        }

        testClass<AbstractCodeFragmentAutoImportTest> {
            model("quickfix.special/codeFragmentAutoImport", pattern = KT, isRecursive = false)
        }

        testClass<AbstractJsCheckerTest> {
            model("checker/js")
        }

        testClass<AbstractQuickFixTest> {
            model("quickfix", pattern = "^([\\w\\-_]+)\\.kt$".toRegex(), filenameStartsLowerCase = true)
        }

        testClass<AbstractGotoSuperTest> {
            model("navigation/gotoSuper", pattern = TEST, isRecursive = false)
        }

        testClass<AbstractGotoTypeDeclarationTest> {
            model("navigation/gotoTypeDeclaration", pattern = TEST)
        }

        testClass<AbstractGotoDeclarationTest> {
            model("navigation/gotoDeclaration", pattern = TEST)
        }

        testClass<AbstractParameterInfoTest> {
            model(
                "parameterInfo", pattern = "^([\\w\\-_]+)\\.kt$".toRegex(), isRecursive = true,
                excludedDirectories = listOf("withLib1/sharedLib", "withLib2/sharedLib", "withLib3/sharedLib")
            )
        }

        testClass<AbstractKotlinGotoTest> {
            model("navigation/gotoClass", testMethodName = "doClassTest")
            model("navigation/gotoSymbol", testMethodName = "doSymbolTest")
        }

        testClass<AbstractNavigateToLibrarySourceTest> {
            model("decompiler/navigation/usercode")
        }

        testClass<AbstractNavigateJavaToLibrarySourceTest> {
            model("decompiler/navigation/userJavaCode", pattern = "^(.+)\\.java$".toRegex())
        }

        testClass<AbstractNavigateToLibrarySourceTestWithJS> {
            model("decompiler/navigation/usercode", testClassName = "UsercodeWithJSModule")
        }

        testClass<AbstractNavigateToDecompiledLibraryTest> {
            model("decompiler/navigation/usercode")
        }

        testClass<AbstractKotlinGotoImplementationTest> {
            model("navigation/implementations", isRecursive = false)
        }

        testClass<AbstractGotoTestOrCodeActionTest> {
            model("navigation/gotoTestOrCode", pattern = "^(.+)\\.main\\..+\$".toRegex())
        }

        testClass<AbstractInheritorsSearchTest> {
            model("search/inheritance")
        }

        testClass<AbstractAnnotatedMembersSearchTest> {
            model("search/annotations")
        }

        testClass<AbstractQuickFixMultiFileTest> {
            model("quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""".toRegex(), testMethodName = "doTestWithExtraFile")
        }

        testClass<AbstractKotlinTypeAliasByExpansionShortNameIndexTest> {
            model("typealiasExpansionIndex")
        }

        testClass<AbstractHighlightingTest> {
            model("highlighter")
        }

        testClass<AbstractDslHighlighterTest> {
            model("dslHighlighter")
        }

        testClass<AbstractUsageHighlightingTest> {
            model("usageHighlighter")
        }

        testClass<AbstractKotlinFoldingTest> {
            model("folding/noCollapse")
            model("folding/checkCollapse", testMethodName = "doSettingsFoldingTest")
        }

        testClass<AbstractSurroundWithTest> {
            model("codeInsight/surroundWith/if", testMethodName = "doTestWithIfSurrounder")
            model("codeInsight/surroundWith/ifElse", testMethodName = "doTestWithIfElseSurrounder")
            model("codeInsight/surroundWith/ifElseExpression", testMethodName = "doTestWithIfElseExpressionSurrounder")
            model("codeInsight/surroundWith/ifElseExpressionBraces", testMethodName = "doTestWithIfElseExpressionBracesSurrounder")
            model("codeInsight/surroundWith/not", testMethodName = "doTestWithNotSurrounder")
            model("codeInsight/surroundWith/parentheses", testMethodName = "doTestWithParenthesesSurrounder")
            model("codeInsight/surroundWith/stringTemplate", testMethodName = "doTestWithStringTemplateSurrounder")
            model("codeInsight/surroundWith/when", testMethodName = "doTestWithWhenSurrounder")
            model("codeInsight/surroundWith/tryCatch", testMethodName = "doTestWithTryCatchSurrounder")
            model("codeInsight/surroundWith/tryCatchExpression", testMethodName = "doTestWithTryCatchExpressionSurrounder")
            model("codeInsight/surroundWith/tryCatchFinally", testMethodName = "doTestWithTryCatchFinallySurrounder")
            model("codeInsight/surroundWith/tryCatchFinallyExpression", testMethodName = "doTestWithTryCatchFinallyExpressionSurrounder")
            model("codeInsight/surroundWith/tryFinally", testMethodName = "doTestWithTryFinallySurrounder")
            model("codeInsight/surroundWith/functionLiteral", testMethodName = "doTestWithFunctionLiteralSurrounder")
            model("codeInsight/surroundWith/withIfExpression", testMethodName = "doTestWithSurroundWithIfExpression")
            model("codeInsight/surroundWith/withIfElseExpression", testMethodName = "doTestWithSurroundWithIfElseExpression")
        }

        testClass<AbstractJoinLinesTest> {
            model("joinLines")
        }

        testClass<AbstractBreadcrumbsTest> {
            model("codeInsight/breadcrumbs")
        }

        testClass<AbstractIntentionTest> {
            model("intentions", pattern = "^([\\w\\-_]+)\\.(kt|kts)$".toRegex())
        }

        testClass<AbstractIntentionTest2> {
            model("intentions/loopToCallChain", pattern = "^([\\w\\-_]+)\\.kt$".toRegex())
        }

        testClass<AbstractConcatenatedStringGeneratorTest> {
            model("concatenatedStringGenerator", pattern = "^([\\w\\-_]+)\\.kt$".toRegex())
        }

        testClass<AbstractInspectionTest> {
            model("intentions", pattern = "^(inspections\\.test)$".toRegex(), flatten = true)
            model("inspections", pattern = "^(inspections\\.test)$".toRegex(), flatten = true)
            model("inspectionsLocal", pattern = "^(inspections\\.test)$".toRegex(), flatten = true)
        }

        testClass<AbstractLocalInspectionTest> {
            model("inspectionsLocal", pattern = "^([\\w\\-_]+)\\.(kt|kts)$".toRegex())
        }

        testClass<AbstractHierarchyTest> {
            model("hierarchy/class/type", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTypeClassHierarchyTest")
            model("hierarchy/class/super", pattern = DIRECTORY, isRecursive = false, testMethodName = "doSuperClassHierarchyTest")
            model("hierarchy/class/sub", pattern = DIRECTORY, isRecursive = false, testMethodName = "doSubClassHierarchyTest")
            model("hierarchy/calls/callers", pattern = DIRECTORY, isRecursive = false, testMethodName = "doCallerHierarchyTest")
            model("hierarchy/calls/callersJava", pattern = DIRECTORY, isRecursive = false, testMethodName = "doCallerJavaHierarchyTest")
            model("hierarchy/calls/callees", pattern = DIRECTORY, isRecursive = false, testMethodName = "doCalleeHierarchyTest")
            model("hierarchy/overrides", pattern = DIRECTORY, isRecursive = false, testMethodName = "doOverrideHierarchyTest")
        }

        testClass<AbstractHierarchyWithLibTest> {
            model("hierarchy/withLib", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractMoveStatementTest> {
            model("codeInsight/moveUpDown/classBodyDeclarations", pattern = KT_OR_KTS, testMethodName = "doTestClassBodyDeclaration")
            model("codeInsight/moveUpDown/closingBraces", testMethodName = "doTestExpression")
            model("codeInsight/moveUpDown/expressions", pattern = KT_OR_KTS, testMethodName = "doTestExpression")
            model("codeInsight/moveUpDown/parametersAndArguments", testMethodName = "doTestExpression")
            model("codeInsight/moveUpDown/trailingComma", testMethodName = "doTestExpressionWithTrailingComma")
        }

        testClass<AbstractMoveLeftRightTest> {
            model("codeInsight/moveLeftRight")
        }

        testClass<AbstractInlineTest> {
            model("refactoring/inline", pattern = "^(\\w+)\\.kt$".toRegex())
        }

        testClass<AbstractUnwrapRemoveTest> {
            model("codeInsight/unwrapAndRemove/removeExpression", testMethodName = "doTestExpressionRemover")
            model("codeInsight/unwrapAndRemove/unwrapThen", testMethodName = "doTestThenUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapElse", testMethodName = "doTestElseUnwrapper")
            model("codeInsight/unwrapAndRemove/removeElse", testMethodName = "doTestElseRemover")
            model("codeInsight/unwrapAndRemove/unwrapLoop", testMethodName = "doTestLoopUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapTry", testMethodName = "doTestTryUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapCatch", testMethodName = "doTestCatchUnwrapper")
            model("codeInsight/unwrapAndRemove/removeCatch", testMethodName = "doTestCatchRemover")
            model("codeInsight/unwrapAndRemove/unwrapFinally", testMethodName = "doTestFinallyUnwrapper")
            model("codeInsight/unwrapAndRemove/removeFinally", testMethodName = "doTestFinallyRemover")
            model("codeInsight/unwrapAndRemove/unwrapLambda", testMethodName = "doTestLambdaUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapFunctionParameter", testMethodName = "doTestFunctionParameterUnwrapper")
        }

        testClass<AbstractExpressionTypeTest> {
            model("codeInsight/expressionType")
        }

        testClass<AbstractRenderingKDocTest> {
            model("codeInsight/renderingKDoc")
        }

        testClass<AbstractBackspaceHandlerTest> {
            model("editor/backspaceHandler")
        }

        testClass<AbstractMultiLineStringIndentTest> {
            model("editor/enterHandler/multilineString")
        }

        testClass<AbstractQuickDocProviderTest> {
            model("editor/quickDoc", pattern = """^([^_]+)\.(kt|java)$""".toRegex())
        }

        testClass<AbstractSafeDeleteTest> {
            model("refactoring/safeDelete/deleteClass/kotlinClass", testMethodName = "doClassTest")
            model("refactoring/safeDelete/deleteClass/kotlinClassWithJava", testMethodName = "doClassTestWithJava")
            model("refactoring/safeDelete/deleteClass/javaClassWithKotlin", pattern = JAVA, testMethodName = "doJavaClassTest")
            model("refactoring/safeDelete/deleteObject/kotlinObject", testMethodName = "doObjectTest")
            model("refactoring/safeDelete/deleteFunction/kotlinFunction", testMethodName = "doFunctionTest")
            model("refactoring/safeDelete/deleteFunction/kotlinFunctionWithJava", testMethodName = "doFunctionTestWithJava")
            model("refactoring/safeDelete/deleteFunction/javaFunctionWithKotlin", testMethodName = "doJavaMethodTest")
            model("refactoring/safeDelete/deleteProperty/kotlinProperty", testMethodName = "doPropertyTest")
            model("refactoring/safeDelete/deleteProperty/kotlinPropertyWithJava", testMethodName = "doPropertyTestWithJava")
            model("refactoring/safeDelete/deleteProperty/javaPropertyWithKotlin", testMethodName = "doJavaPropertyTest")
            model("refactoring/safeDelete/deleteTypeAlias/kotlinTypeAlias", testMethodName = "doTypeAliasTest")
            model("refactoring/safeDelete/deleteTypeParameter/kotlinTypeParameter", testMethodName = "doTypeParameterTest")
            model("refactoring/safeDelete/deleteTypeParameter/kotlinTypeParameterWithJava", testMethodName = "doTypeParameterTestWithJava")
            model("refactoring/safeDelete/deleteValueParameter/kotlinValueParameter", testMethodName = "doValueParameterTest")
            model("refactoring/safeDelete/deleteValueParameter/kotlinValueParameterWithJava", testMethodName = "doValueParameterTestWithJava")
        }

        testClass<AbstractReferenceResolveTest> {
            model("resolve/references", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractReferenceResolveInJavaTest> {
            model("resolve/referenceInJava/binaryAndSource", pattern = JAVA)
            model("resolve/referenceInJava/sourceOnly", pattern = JAVA)
        }

        testClass<AbstractReferenceToCompiledKotlinResolveInJavaTest> {
            model("resolve/referenceInJava/binaryAndSource", pattern = JAVA)
        }

        testClass<AbstractReferenceResolveWithLibTest> {
            model("resolve/referenceWithLib", isRecursive = false)
        }

        testClass<AbstractReferenceResolveInLibrarySourcesTest> {
            model("resolve/referenceInLib", isRecursive = false)
        }

        testClass<AbstractReferenceToJavaWithWrongFileStructureTest> {
            model("resolve/referenceToJavaWithWrongFileStructure", isRecursive = false)
        }

        testClass<AbstractFindUsagesTest> {
            model("findUsages/kotlin", pattern = """^(.+)\.0\.(kt|kts)$""".toRegex())
            model("findUsages/java", pattern = """^(.+)\.0\.java$""".toRegex())
            model("findUsages/propertyFiles", pattern = """^(.+)\.0\.properties$""".toRegex())
        }

        testClass<AbstractFindUsagesWithDisableComponentSearchTest> {
            model("findUsages/kotlin/conventions/components", pattern = """^(.+)\.0\.(kt|kts)$""".toRegex())
        }

        testClass<AbstractKotlinFindUsagesWithLibraryTest> {
            model("findUsages/libraryUsages", pattern = """^(.+)\.0\.kt$""".toRegex())
        }

        testClass<AbstractMoveTest> {
            model("refactoring/move", pattern = TEST, flatten = true)
        }

        testClass<AbstractCopyTest> {
            model("refactoring/copy", pattern = TEST, flatten = true)
        }

        testClass<AbstractMultiModuleMoveTest> {
            model("refactoring/moveMultiModule", pattern = TEST, flatten = true)
        }

        testClass<AbstractMultiModuleCopyTest> {
            model("refactoring/copyMultiModule", pattern = TEST, flatten = true)
        }

        testClass<AbstractMultiModuleSafeDeleteTest> {
            model("refactoring/safeDeleteMultiModule", pattern = TEST, flatten = true)
        }

        testClass<AbstractMultiFileIntentionTest> {
            model("multiFileIntentions", pattern = TEST, flatten = true, filenameStartsLowerCase = true)
        }

        testClass<AbstractMultiFileLocalInspectionTest> {
            model("multiFileLocalInspections", pattern = TEST, flatten = true, filenameStartsLowerCase = true)
        }

        testClass<AbstractMultiFileInspectionTest> {
            model("multiFileInspections", pattern = TEST, flatten = true)
        }

        testClass<AbstractFormatterTest> {
            model("formatter", pattern = """^([^\.]+)\.after\.kt.*$""".toRegex())
            model("formatter/trailingComma", pattern = """^([^\.]+)\.call\.after\.kt.*$""".toRegex(), testMethodName = "doTestCallSite", testClassName = "FormatterCallSite")
            model("formatter", pattern = """^([^\.]+)\.after\.inv\.kt.*$""".toRegex(), testMethodName = "doTestInverted", testClassName = "FormatterInverted")
            model("formatter/trailingComma", pattern = """^([^\.]+)\.call\.after\.inv\.kt.*$""".toRegex(), testMethodName = "doTestInvertedCallSite", testClassName = "FormatterInvertedCallSite")
        }

        testClass<AbstractTypingIndentationTestBase> {
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.kt.*$""".toRegex(), testMethodName = "doNewlineTest", testClassName = "DirectSettings")
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.inv\.kt.*$""".toRegex(), testMethodName = "doNewlineTestWithInvert", testClassName = "InvertedSettings")
        }

        testClass<AbstractDiagnosticMessageTest> {
            model("diagnosticMessage", isRecursive = false)
        }

        testClass<AbstractDiagnosticMessageJsTest> {
            model("diagnosticMessage/js", isRecursive = false, targetBackend = TargetBackend.JS)
        }

        testClass<AbstractRenameTest> {
            model("refactoring/rename", pattern = TEST, flatten = true)
        }

        testClass<AbstractMultiModuleRenameTest> {
            model("refactoring/renameMultiModule", pattern = TEST, flatten = true)
        }

        testClass<AbstractOutOfBlockModificationTest> {
            model("codeInsight/outOfBlock", pattern = KT_OR_KTS)
        }

        testClass<AbstractChangeLocalityDetectorTest> {
            model("codeInsight/changeLocality", pattern = KT_OR_KTS)
        }

        testClass<AbstractDataFlowValueRenderingTest> {
            model("dataFlowValueRendering")
        }

        testClass<AbstractJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", pattern = """^([^\.]+)\.java$""".toRegex())
        }

        testClass<AbstractTextJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/plainTextConversion", pattern = """^([^\.]+)\.txt$""".toRegex())
        }

        testClass<AbstractLiteralTextToKotlinCopyPasteTest> {
            model("copyPaste/plainTextLiteral", pattern = """^([^\.]+)\.txt$""".toRegex())
        }

        testClass<AbstractLiteralKotlinToKotlinCopyPasteTest> {
            model("copyPaste/literal", pattern = """^([^\.]+)\.kt$""".toRegex())
        }

        testClass<AbstractInsertImportOnPasteTest> {
            model("copyPaste/imports", pattern = KT_WITHOUT_DOTS, testMethodName = "doTestCopy", testClassName = "Copy", isRecursive = false)
            model("copyPaste/imports", pattern = KT_WITHOUT_DOTS, testMethodName = "doTestCut", testClassName = "Cut", isRecursive = false)
        }

        testClass<AbstractMoveOnCutPasteTest> {
            model("copyPaste/moveDeclarations", pattern = KT_WITHOUT_DOTS, testMethodName = "doTest")
        }

        testClass<AbstractUpdateKotlinCopyrightTest> {
            model("copyright", pattern = KT_OR_KTS, testMethodName = "doTest")
        }

        testClass<AbstractHighlightExitPointsTest> {
            model("exitPoints")
        }

        testClass<AbstractLineMarkersTest> {
            model("codeInsight/lineMarker")
        }

        testClass<AbstractLineMarkersTestInLibrarySources> {
            model("codeInsightInLibrary/lineMarker", testMethodName = "doTestWithLibrary")
        }

        testClass<AbstractMultiModuleLineMarkerTest> {
            model("multiModuleLineMarker", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractShortenRefsTest> {
            model("shortenRefs", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractAddImportTest> {
            model("addImport", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractAddImportAliasTest> {
            model("addImportAlias", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractSmartSelectionTest> {
            model("smartSelection", testMethodName = "doTestSmartSelection", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractKotlinFileStructureTest> {
            model("structureView/fileStructure", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractExpressionSelectionTest> {
            model("expressionSelection", testMethodName = "doTestExpressionSelection", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractCommonDecompiledTextTest> {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""".toRegex())
        }

        testClass<AbstractJvmDecompiledTextTest> {
            model("decompiler/decompiledTextJvm", pattern = """^([^\.]+)$""".toRegex())
        }

        testClass<AbstractCommonDecompiledTextFromJsMetadataTest> {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""".toRegex(), targetBackend = TargetBackend.JS)
        }

        testClass<AbstractJsDecompiledTextFromJsMetadataTest> {
            model("decompiler/decompiledTextJs", pattern = """^([^\.]+)$""".toRegex(), targetBackend = TargetBackend.JS)
        }

        testClass<AbstractClsStubBuilderTest> {
            model("decompiler/stubBuilder", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractJvmOptimizeImportsTest> {
            model("editor/optimizeImports/jvm", pattern = KT_OR_KTS_WITHOUT_DOTS)
            model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractJsOptimizeImportsTest> {
            model("editor/optimizeImports/js", pattern = KT_WITHOUT_DOTS)
            model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractKotlinExceptionFilterTest> {
            model("debugger/exceptionFilter", pattern = """^([^\.]+)$""".toRegex(), isRecursive = false)
        }

        testClass<AbstractStubBuilderTest> {
            model("stubs", pattern = KT)
        }

        testClass<AbstractMultiFileHighlightingTest> {
            model("multiFileHighlighting", isRecursive = false)
        }

        testClass<AbstractMultiPlatformHighlightingTest> {
            model("multiModuleHighlighting/multiplatform/", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractMultiplatformAnalysisTest> {
            model("multiplatform", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractQuickFixMultiModuleTest> {
            model("multiModuleQuickFix", pattern = DIRECTORY, depth = 1)
        }

        testClass<AbstractKotlinGotoImplementationMultiModuleTest> {
            model("navigation/implementations/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractKotlinGotoRelatedSymbolMultiModuleTest> {
            model("navigation/relatedSymbols/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractKotlinGotoSuperMultiModuleTest> {
            model("navigation/gotoSuper/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractExtractionTest> {
            model("refactoring/introduceVariable", pattern = KT_OR_KTS, testMethodName = "doIntroduceVariableTest")
            model("refactoring/extractFunction", pattern = KT_OR_KTS, testMethodName = "doExtractFunctionTest")
            model("refactoring/introduceProperty", pattern = KT_OR_KTS, testMethodName = "doIntroducePropertyTest")
            model("refactoring/introduceParameter", pattern = KT_OR_KTS, testMethodName = "doIntroduceSimpleParameterTest")
            model("refactoring/introduceLambdaParameter", pattern = KT_OR_KTS, testMethodName = "doIntroduceLambdaParameterTest")
            model("refactoring/introduceJavaParameter", pattern = JAVA, testMethodName = "doIntroduceJavaParameterTest")
            model("refactoring/introduceTypeParameter", pattern = KT_OR_KTS, testMethodName = "doIntroduceTypeParameterTest")
            model("refactoring/introduceTypeAlias", pattern = KT_OR_KTS, testMethodName = "doIntroduceTypeAliasTest")
            model("refactoring/extractSuperclass", pattern = KT_OR_KTS_WITHOUT_DOTS, testMethodName = "doExtractSuperclassTest")
            model("refactoring/extractInterface", pattern = KT_OR_KTS_WITHOUT_DOTS, testMethodName = "doExtractInterfaceTest")
        }

        testClass<AbstractPullUpTest> {
            model("refactoring/pullUp/k2k", pattern = KT, flatten = true, testClassName = "K2K", testMethodName = "doKotlinTest")
            model("refactoring/pullUp/k2j", pattern = KT, flatten = true, testClassName = "K2J", testMethodName = "doKotlinTest")
            model("refactoring/pullUp/j2k", pattern = JAVA, flatten = true, testClassName = "J2K", testMethodName = "doJavaTest")
        }

        testClass<AbstractPushDownTest> {
            model("refactoring/pushDown/k2k", pattern = KT, flatten = true, testClassName = "K2K", testMethodName = "doKotlinTest")
            model("refactoring/pushDown/k2j", pattern = KT, flatten = true, testClassName = "K2J", testMethodName = "doKotlinTest")
            model("refactoring/pushDown/j2k", pattern = JAVA, flatten = true, testClassName = "J2K", testMethodName = "doJavaTest")
        }

        testClass<AbstractKotlinCoverageOutputFilesTest> {
            model("coverage/outputFiles")
        }

        testClass<AbstractBytecodeToolWindowTest> {
            model("internal/toolWindow", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractReferenceResolveTest>("org.jetbrains.kotlin.idea.kdoc.KdocResolveTestGenerated") {
            model("kdoc/resolve")
        }

        testClass<AbstractKDocHighlightingTest> {
            model("kdoc/highlighting")
        }

        testClass<AbstractKDocTypingTest> {
            model("kdoc/typing")
        }

        testClass<AbstractGenerateTestSupportMethodActionTest> {
            model("codeInsight/generate/testFrameworkSupport")
        }

        testClass<AbstractGenerateHashCodeAndEqualsActionTest> {
            model("codeInsight/generate/equalsWithHashCode")
        }

        testClass<AbstractCodeInsightActionTest> {
            model("codeInsight/generate/secondaryConstructors")
        }

        testClass<AbstractGenerateToStringActionTest> {
            model("codeInsight/generate/toString")
        }

        testClass<AbstractIdeReplCompletionTest> {
            model("repl/completion")
        }

        testClass<AbstractPostfixTemplateProviderTest> {
            model("codeInsight/postfix")
        }

        testClass<AbstractScriptConfigurationHighlightingTest> {
            model("script/definition/highlighting", pattern = DIRECTORY, isRecursive = false)
            model("script/definition/complex", pattern = DIRECTORY, isRecursive = false, testMethodName = "doComplexTest")
        }

        testClass<AbstractScriptConfigurationNavigationTest> {
            model("script/definition/navigation", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractScriptConfigurationCompletionTest> {
            model("script/definition/completion", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractScriptDefinitionsOrderTest> {
            model("script/definition/order", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractNameSuggestionProviderTest> {
            model("refactoring/nameSuggestionProvider")
        }

        testClass<AbstractSlicerTreeTest> {
            model("slicer")
        }

        testClass<AbstractSlicerLeafGroupingTest> {
            model("slicer/inflow", flatten = true)
        }

        testClass<AbstractSlicerNullnessGroupingTest> {
            model("slicer/inflow", flatten = true)
        }

        testClass<AbstractSlicerMultiplatformTest> {
            model("slicer/mpp", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractSlicerMultiplatformTest> {
            model("navigationToolbar", isRecursive = false)
        }
    }

    testGroup("fir", testDataPath = "../idea/testData") {
        testClass<AbstractFirMultiModuleResolveTest> {
            model("fir/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        testClass<AbstractFirLazyResolveTest> {
            model("fir/lazyResolve", pattern = TEST, flatten = true, filenameStartsLowerCase = true)
        }

        testClass<AbstractFirReferenceResolveTest> {
            model("resolve/references", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractFirPsiCheckerTest> {
            model("checker", isRecursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/duplicateJvmSignature")
            model("checker/infos")
            model("checker/diagnosticsMessage")
        }
    }

    testGroup("scripting-support") {
        testClass<AbstractScratchRunActionTest> {
            model("scratch", pattern = KTS, testMethodName = "doScratchCompilingTest", testClassName = "ScratchCompiling", isRecursive = false)
            model("scratch", pattern = KTS, testMethodName = "doScratchReplTest", testClassName = "ScratchRepl", isRecursive = false)
            model("scratch/multiFile", pattern = DIRECTORY, testMethodName = "doScratchMultiFileTest", testClassName = "ScratchMultiFile", isRecursive = false)
            model("worksheet", pattern = WS_KTS, testMethodName = "doWorksheetCompilingTest", testClassName = "WorksheetCompiling", isRecursive = false)
            model("worksheet", pattern = WS_KTS, testMethodName = "doWorksheetReplTest", testClassName = "WorksheetRepl", isRecursive = false)
            model("worksheet/multiFile", pattern = DIRECTORY, testMethodName = "doWorksheetMultiFileTest", testClassName = "WorksheetMultiFile", isRecursive = false)
            model("scratch/rightPanelOutput", pattern = KTS, testMethodName = "doRightPreviewPanelOutputTest", testClassName = "ScratchRightPanelOutput", isRecursive = false)
        }

        testClass<AbstractScratchLineMarkersTest> {
            model("scratch/lineMarker", testMethodName = "doScratchTest", pattern = KT_OR_KTS)
        }
    }

    testGroup("maven") {
        testClass<AbstractMavenConfigureProjectByChangingFileTest> {
            model("configurator/jvm", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestWithMaven")
            model("configurator/js", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestWithJSMaven")
        }

        testClass<AbstractKotlinMavenInspectionTest> {
            model("maven-inspections", pattern = "^([\\w\\-]+).xml$".toRegex(), flatten = true)
        }
    }

    testGroup("gradle/gradle-idea", testDataPath = "../../idea/testData") {
        testClass<AbstractGradleConfigureProjectByChangingFileTest> {
            model("configuration/gradle", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestGradle")
            model("configuration/gsk", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestGradle")
        }
    }

    testGroup("idea") {
        testClass<AbstractResolveByStubTest> {
            model("compiler/loadJava/compiledKotlin")
        }

        testClass<AbstractLoadJavaClsStubTest> {
            model("compiler/loadJava/compiledKotlin", testMethodName = "doTestCompiledKotlin")
        }

        testClass<AbstractIdeLightClassTest> {
            model("compiler/asJava/lightClasses", excludedDirectories = listOf("delegation", "script"), pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractIdeLightClassForScriptTest> {
            model("compiler/asJava/script/ide", pattern = KT_OR_KTS_WITHOUT_DOTS)
        }

        testClass<AbstractUltraLightClassSanityTest> {
            model("compiler/asJava/lightClasses", pattern = KT_OR_KTS)
        }
        testClass<AbstractUltraLightClassLoadingTest> {
            model("compiler/asJava/ultraLightClasses", pattern = KT_OR_KTS)
        }
        testClass<AbstractUltraLightScriptLoadingTest> {
            model("compiler/asJava/ultraLightScripts", pattern = KT_OR_KTS)
        }
        testClass<AbstractUltraLightFacadeClassTest> {
            model("compiler/asJava/ultraLightFacades", pattern = KT_OR_KTS)
        }

        testClass<AbstractIdeCompiledLightClassTest> {
            model("compiler/asJava/lightClasses", excludedDirectories = listOf("local", "compilationErrors", "ideRegression"), pattern = KT_OR_KTS_WITHOUT_DOTS)
        }
    }

    testGroup("completion") {
        testClass<AbstractCompiledKotlinInJavaCompletionTest> {
            model("injava", pattern = JAVA, isRecursive = false)
        }

        testClass<AbstractKotlinSourceInJavaCompletionTest> {
            model("injava", pattern = JAVA, isRecursive = false)
        }

        testClass<AbstractKotlinStdLibInJavaCompletionTest> {
            model("injava/stdlib", pattern = JAVA, isRecursive = false)
        }

        testClass<AbstractBasicCompletionWeigherTest> {
            model("weighers/basic", pattern = KT_OR_KTS_WITHOUT_DOTS)
        }

        testClass<AbstractSmartCompletionWeigherTest> {
            model("weighers/smart", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractJSBasicCompletionTest> {
            model("basic/common")
            model("basic/js")
        }

        testClass<AbstractJvmBasicCompletionTest> {
            model("basic/common")
            model("basic/java")
        }

        testClass<AbstractJvmSmartCompletionTest> {
            model("smart")
        }

        testClass<AbstractKeywordCompletionTest> {
            model("keywords", isRecursive = false)
        }

        testClass<AbstractJvmWithLibBasicCompletionTest> {
            model("basic/withLib", isRecursive = false)
        }

        testClass<AbstractBasicCompletionHandlerTest> {
            model("handlers/basic", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractSmartCompletionHandlerTest> {
            model("handlers/smart")
        }

        testClass<AbstractKeywordCompletionHandlerTest> {
            model("handlers/keywords")
        }

        testClass<AbstractCompletionCharFilterTest> {
            model("handlers/charFilter", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractMultiFileJvmBasicCompletionTest> {
            model("basic/multifile", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractMultiFileSmartCompletionTest> {
            model("smartMultiFile", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractJvmBasicCompletionTest>("org.jetbrains.kotlin.idea.completion.test.KDocCompletionTestGenerated") {
            model("kdoc")
        }

        testClass<AbstractJava8BasicCompletionTest> {
            model("basic/java8")
        }

        testClass<AbstractCompletionIncrementalResolveTest> {
            model("incrementalResolve")
        }

        testClass<AbstractMultiPlatformCompletionTest> {
            model("multiPlatform", isRecursive = false, pattern = DIRECTORY)
        }
    }

    testGroup("project-wizard/cli") {
        testClass<AbstractYamlBuildFileGenerationTest> {
            model("buildFileGeneration", isRecursive = false, pattern = DIRECTORY)
        }
        testClass<AbstractProjectTemplateBuildFileGenerationTest> {
            model("projectTemplatesBuildFileGeneration", isRecursive = false, pattern = DIRECTORY)
        }
    }

    testGroup("project-wizard/idea", testDataPath = "../cli/testData") {
        fun MutableTSuite.allBuildSystemTests(relativeRootPath: String) {
            for (testClass in listOf("GradleKts", "GradleGroovy", "Maven")) {
                model(
                    relativeRootPath,
                    isRecursive = false,
                    pattern = DIRECTORY,
                    testMethodName = "doTest${testClass}",
                    testClassName = testClass
                )
            }
        }
        testClass<AbstractYamlNewWizardProjectImportTest> {
            allBuildSystemTests("buildFileGeneration")
        }
        testClass<AbstractProjectTemplateNewWizardProjectImportTest> {
            allBuildSystemTests("projectTemplatesBuildFileGeneration")
        }
    }

    //TODO: move these tests into idea-completion module
    testGroup("idea", testDataPath = "../completion/testData") {
        testClass<AbstractCodeFragmentCompletionHandlerTest> {
            model("handlers/runtimeCast")
        }

        testClass<AbstractCodeFragmentCompletionTest> {
            model("basic/codeFragments", pattern = KT)
        }
    }

    testGroup("j2k/old") {
        testClass<AbstractJavaToKotlinConverterSingleFileTest> {
            model("fileOrElement", pattern = JAVA)
        }
        testClass<AbstractJavaToKotlinConverterMultiFileTest> {
            model("multiFile", pattern = DIRECTORY, isRecursive = false)
        }
    }

    testGroup("j2k/new") {
        testClass<AbstractNewJavaToKotlinConverterSingleFileTest> {
            model("newJ2k", pattern = """^([^\.]+)\.java$""".toRegex())
        }
        testClass<AbstractCommonConstraintCollectorTest> {
            model("inference/common")
        }
        testClass<AbstractNullabilityInferenceTest> {
            model("inference/nullability")
        }
        testClass<AbstractMutabilityInferenceTest> {
            model("inference/mutability")
        }
        testClass<AbstractNewJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste", pattern = """^([^\.]+)\.java$""".toRegex())
        }
        testClass<AbstractTextNewJavaToKotlinCopyPasteConversionTest> {
            model("copyPastePlainText", pattern = """^([^\.]+)\.txt$""".toRegex())
        }
        testClass<AbstractNewJavaToKotlinConverterMultiFileTest> {
            model("multiFile", pattern = DIRECTORY, isRecursive = false)
        }
    }

    testGroup("jps/jps-plugin") {
        testClass<AbstractIncrementalJvmJpsTest> {
            model("incremental/multiModule/common", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/multiModule/jvm", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/multiModule/multiplatform/custom", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/pureKotlin", pattern = DIRECTORY, isRecursive = false)
            model("incremental/withJava", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/inlineFunCallSite", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/classHierarchyAffected", pattern = DIRECTORY, excludeParentDirs = true)
        }

        //actualizeMppJpsIncTestCaseDirs(testDataAbsoluteRoot, "incremental/multiModule/multiplatform/withGeneratedContent")

        testClass<AbstractIncrementalJsJpsTest> {
            model("incremental/multiModule/common", pattern = DIRECTORY, excludeParentDirs = true)
        }

        testClass<AbstractMultiplatformJpsTestWithGeneratedContent> {
            model(
                "incremental/multiModule/multiplatform/withGeneratedContent", isRecursive = true, pattern = DIRECTORY,
                testClassName = "MultiplatformMultiModule", excludeParentDirs = true
            )
        }

        testClass<AbstractJvmLookupTrackerTest> {
            model("incremental/lookupTracker/jvm", pattern = DIRECTORY, isRecursive = false)
        }
        testClass<AbstractJsLookupTrackerTest> {
            model("incremental/lookupTracker/js", pattern = DIRECTORY, isRecursive = false)
        }
        testClass<AbstractJsKlibLookupTrackerTest> {
            // todo: investigate why lookups are different from non-klib js
            model("incremental/lookupTracker/jsKlib", pattern = DIRECTORY, isRecursive = false)
        }

        testClass<AbstractIncrementalLazyCachesTest> {
            model("incremental/lazyKotlinCaches", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/changeIncrementalOption", pattern = DIRECTORY, excludeParentDirs = true)
        }

        testClass<AbstractIncrementalCacheVersionChangedTest> {
            model("incremental/cacheVersionChanged", pattern = DIRECTORY, excludeParentDirs = true)
        }

        testClass<AbstractDataContainerVersionChangedTest> {
            model("incremental/cacheVersionChanged", pattern = DIRECTORY, excludeParentDirs = true)
        }
    }

    testGroup("jps/jps-plugin") {
        fun MutableTSuite.commonProtoComparisonTests() {
            model("comparison/classSignatureChange", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/classPrivateOnlyChange", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/classMembersOnlyChanged", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/packageMembers", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/unchanged", pattern = DIRECTORY, excludeParentDirs = true)
        }

        testClass<AbstractJvmProtoComparisonTest> {
            commonProtoComparisonTests()
            model("comparison/jvmOnly", pattern = DIRECTORY, excludeParentDirs = true)
        }

        testClass<AbstractJsProtoComparisonTest> {
            commonProtoComparisonTests()
            model("comparison/jsOnly", pattern = DIRECTORY, excludeParentDirs = true)
        }
    }

    testGroup("performance-tests", testDataPath = "../idea/testData") {
        testClass<AbstractPerformanceJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", testMethodName = "doPerfTest", pattern = """^([^\.]+)\.java$""".toRegex())
        }

        testClass<AbstractPerformanceNewJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", testMethodName = "doPerfTest", pattern = """^([^\.]+)\.java$""".toRegex())
        }

        testClass<AbstractPerformanceLiteralKotlinToKotlinCopyPasteTest> {
            model("copyPaste/literal", testMethodName = "doPerfTest", pattern = """^([^\.]+)\.kt$""".toRegex())
        }

        testClass<AbstractPerformanceHighlightingTest> {
            model("highlighter", testMethodName = "doPerfTest")
        }

        testClass<AbstractPerformanceAddImportTest> {
            model("addImport", testMethodName = "doPerfTest", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractPerformanceTypingIndentationTest> {
            model("indentationOnNewline", testMethodName = "doPerfTest", pattern = KT_OR_KTS_WITHOUT_DOTS)
        }
    }

    testGroup("performance-tests", testDataPath = "../completion/testData") {
        testClass<AbstractPerformanceCompletionIncrementalResolveTest> {
            model("incrementalResolve", testMethodName = "doPerfTest")
        }

        testClass<AbstractPerformanceBasicCompletionHandlerTest> {
            model("handlers/basic", testMethodName = "doPerfTest", pattern = KT_WITHOUT_DOTS)
        }

        testClass<AbstractPerformanceSmartCompletionHandlerTest> {
            model("handlers/smart", testMethodName = "doPerfTest")
        }

        testClass<AbstractPerformanceKeywordCompletionHandlerTest> {
            model("handlers/keywords", testMethodName = "doPerfTest")
        }

        testClass<AbstractPerformanceCompletionCharFilterTest> {
            model("handlers/charFilter", testMethodName = "doPerfTest", pattern = KT_WITHOUT_DOTS)
        }
    }
}
