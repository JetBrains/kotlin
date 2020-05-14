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
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerNullnessGroupingTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerTreeTest
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinFileStructureTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiFileHighlightingTest
import org.jetbrains.kotlin.idea.stubs.AbstractResolveByStubTest
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterForWebDemoTest
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
    group("jvm-debugger/test") {
        suite<AbstractKotlinSteppingTest> {
            model("stepping/stepIntoAndSmartStepInto", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepIntoTest", testClassName = "StepInto")
            model("stepping/stepIntoAndSmartStepInto", pattern = KT_WITHOUT_DOTS, testMethodName = "doSmartStepIntoTest", testClassName = "SmartStepInto")
            model("stepping/stepInto", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepIntoTest", testClassName = "StepIntoOnly")
            model("stepping/stepOut", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepOutTest")
            model("stepping/stepOver", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepOverTest")
            model("stepping/filters", pattern = KT_WITHOUT_DOTS, testMethodName = "doStepIntoTest")
            model("stepping/custom", pattern = KT_WITHOUT_DOTS, testMethodName = "doCustomTest")
        }

        suite<AbstractKotlinEvaluateExpressionTest> {
            model("evaluation/singleBreakpoint", testMethodName = "doSingleBreakpointTest")
            model("evaluation/multipleBreakpoints", testMethodName = "doMultipleBreakpointsTest")
        }

        suite<AbstractSelectExpressionForDebuggerTest> {
            model("selectExpression", isRecursive = false)
            model("selectExpression/disallowMethodCalls", testMethodName = "doTestWoMethodCalls")
        }

        suite<AbstractPositionManagerTest> {
            model("positionManager", isRecursive = false, pattern = KT, testClassName = "SingleFile")
            model("positionManager", isRecursive = false, pattern = DIRECTORY, testClassName = "MultiFile")
        }

        suite<AbstractSmartStepIntoTest> {
            model("smartStepInto")
        }

        suite<AbstractBreakpointApplicabilityTest> {
            model("breakpointApplicability")
        }

        suite<AbstractFileRankingTest> {
            model("fileRanking")
        }

        suite<AbstractAsyncStackTraceTest> {
            model("asyncStackTrace")
        }

        suite<AbstractCoroutineDumpTest> {
            model("coroutines")
        }

        suite<AbstractSequenceTraceTestCase> {
            // TODO: implement mapping logic for terminal operations
            model("sequence/streams/sequence", excludedDirectories = listOf("terminal"))
        }
    }

    group("idea") {
        suite<AbstractAdditionalResolveDescriptorRendererTest> {
            model("resolve/additionalLazyResolve")
        }

        suite<AbstractPartialBodyResolveTest> {
            model("resolve/partialBodyResolve")
        }

        suite<AbstractPsiCheckerTest> {
            model("checker", isRecursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/scripts", pattern = KTS)
            model("checker/duplicateJvmSignature")
            model("checker/infos", testMethodName = "doTestWithInfos")
            model("checker/diagnosticsMessage")
        }

        suite<AbstractJavaAgainstKotlinSourceCheckerTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
            model("kotlinAndJavaChecker/javaWithKotlin")
        }

        suite<AbstractJavaAgainstKotlinSourceCheckerWithoutUltraLightTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
            model("kotlinAndJavaChecker/javaWithKotlin")
        }

        suite<AbstractJavaAgainstKotlinBinariesCheckerTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
        }

        suite<AbstractPsiUnifierTest> {
            model("unifier")
        }

        suite<AbstractCodeFragmentHighlightingTest> {
            model("checker/codeFragments", pattern = KT, isRecursive = false)
            model("checker/codeFragments/imports", testMethodName = "doTestWithImport", pattern = KT)
        }

        suite<AbstractCodeFragmentAutoImportTest> {
            model("quickfix.special/codeFragmentAutoImport", pattern = KT, isRecursive = false)
        }

        suite<AbstractJsCheckerTest> {
            model("checker/js")
        }

        suite<AbstractQuickFixTest> {
            model("quickfix", pattern = "^([\\w\\-_]+)\\.kt$".toRegex(), filenameStartsLowerCase = true)
        }

        suite<AbstractGotoSuperTest> {
            model("navigation/gotoSuper", pattern = TEST, isRecursive = false)
        }

        suite<AbstractGotoTypeDeclarationTest> {
            model("navigation/gotoTypeDeclaration", pattern = TEST)
        }

        suite<AbstractGotoDeclarationTest> {
            model("navigation/gotoDeclaration", pattern = TEST)
        }

        suite<AbstractParameterInfoTest> {
            model(
                "parameterInfo", pattern = "^([\\w\\-_]+)\\.kt$".toRegex(), isRecursive = true,
                excludedDirectories = listOf("withLib1/sharedLib", "withLib2/sharedLib", "withLib3/sharedLib")
            )
        }

        suite<AbstractKotlinGotoTest> {
            model("navigation/gotoClass", testMethodName = "doClassTest")
            model("navigation/gotoSymbol", testMethodName = "doSymbolTest")
        }

        suite<AbstractNavigateToLibrarySourceTest> {
            muteExtraSuffix(".libsrc")
            model("decompiler/navigation/usercode")
        }

        suite<AbstractNavigateJavaToLibrarySourceTest> {
            muteExtraSuffix(".libsrc")
            model("decompiler/navigation/userJavaCode", pattern = "^(.+)\\.java$".toRegex())
        }

        suite<AbstractNavigateToLibrarySourceTestWithJS> {
            muteExtraSuffix(".libsrcjs")
            model("decompiler/navigation/usercode", testClassName = "UsercodeWithJSModule")
        }

        suite<AbstractNavigateToDecompiledLibraryTest> {
            model("decompiler/navigation/usercode")
        }

        suite<AbstractKotlinGotoImplementationTest> {
            model("navigation/implementations", isRecursive = false)
        }

        suite<AbstractGotoTestOrCodeActionTest> {
            model("navigation/gotoTestOrCode", pattern = "^(.+)\\.main\\..+\$".toRegex())
        }

        suite<AbstractInheritorsSearchTest> {
            model("search/inheritance")
        }

        suite<AbstractAnnotatedMembersSearchTest> {
            model("search/annotations")
        }

        suite<AbstractQuickFixMultiFileTest> {
            model("quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""".toRegex(), testMethodName = "doTestWithExtraFile")
        }

        suite<AbstractKotlinTypeAliasByExpansionShortNameIndexTest> {
            model("typealiasExpansionIndex")
        }

        suite<AbstractHighlightingTest> {
            model("highlighter")
        }

        suite<AbstractDslHighlighterTest> {
            model("dslHighlighter")
        }

        suite<AbstractUsageHighlightingTest> {
            model("usageHighlighter")
        }

        suite<AbstractKotlinFoldingTest> {
            model("folding/noCollapse")
            model("folding/checkCollapse", testMethodName = "doSettingsFoldingTest")
        }

        suite<AbstractSurroundWithTest> {
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

        suite<AbstractJoinLinesTest> {
            model("joinLines")
        }

        suite<AbstractBreadcrumbsTest> {
            model("codeInsight/breadcrumbs")
        }

        suite<AbstractIntentionTest> {
            model("intentions", pattern = "^([\\w\\-_]+)\\.(kt|kts)$".toRegex())
        }

        suite<AbstractIntentionTest2> {
            model("intentions/loopToCallChain", pattern = "^([\\w\\-_]+)\\.kt$".toRegex())
        }

        suite<AbstractConcatenatedStringGeneratorTest> {
            model("concatenatedStringGenerator", pattern = "^([\\w\\-_]+)\\.kt$".toRegex())
        }

        suite<AbstractInspectionTest> {
            model("intentions", pattern = "^(inspections\\.test)$".toRegex(), flatten = true)
            model("inspections", pattern = "^(inspections\\.test)$".toRegex(), flatten = true)
            model("inspectionsLocal", pattern = "^(inspections\\.test)$".toRegex(), flatten = true)
        }

        suite<AbstractLocalInspectionTest> {
            model("inspectionsLocal", pattern = "^([\\w\\-_]+)\\.(kt|kts)$".toRegex())
        }

        suite<AbstractHierarchyTest> {
            model("hierarchy/class/type", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTypeClassHierarchyTest")
            model("hierarchy/class/super", pattern = DIRECTORY, isRecursive = false, testMethodName = "doSuperClassHierarchyTest")
            model("hierarchy/class/sub", pattern = DIRECTORY, isRecursive = false, testMethodName = "doSubClassHierarchyTest")
            model("hierarchy/calls/callers", pattern = DIRECTORY, isRecursive = false, testMethodName = "doCallerHierarchyTest")
            model("hierarchy/calls/callersJava", pattern = DIRECTORY, isRecursive = false, testMethodName = "doCallerJavaHierarchyTest")
            model("hierarchy/calls/callees", pattern = DIRECTORY, isRecursive = false, testMethodName = "doCalleeHierarchyTest")
            model("hierarchy/overrides", pattern = DIRECTORY, isRecursive = false, testMethodName = "doOverrideHierarchyTest")
        }

        suite<AbstractHierarchyWithLibTest> {
            model("hierarchy/withLib", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractMoveStatementTest> {
            model("codeInsight/moveUpDown/classBodyDeclarations", pattern = KT_OR_KTS, testMethodName = "doTestClassBodyDeclaration")
            model("codeInsight/moveUpDown/closingBraces", testMethodName = "doTestExpression")
            model("codeInsight/moveUpDown/expressions", pattern = KT_OR_KTS, testMethodName = "doTestExpression")
            model("codeInsight/moveUpDown/parametersAndArguments", testMethodName = "doTestExpression")
            model("codeInsight/moveUpDown/trailingComma", testMethodName = "doTestExpressionWithTrailingComma")
        }

        suite<AbstractMoveLeftRightTest> {
            model("codeInsight/moveLeftRight")
        }

        suite<AbstractInlineTest> {
            model("refactoring/inline", pattern = "^(\\w+)\\.kt$".toRegex())
        }

        suite<AbstractUnwrapRemoveTest> {
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

        suite<AbstractExpressionTypeTest> {
            model("codeInsight/expressionType")
        }

        suite<AbstractRenderingKDocTest> {
            model("codeInsight/renderingKDoc")
        }

        suite<AbstractBackspaceHandlerTest> {
            model("editor/backspaceHandler")
        }

        suite<AbstractMultiLineStringIndentTest> {
            model("editor/enterHandler/multilineString")
        }

        suite<AbstractQuickDocProviderTest> {
            model("editor/quickDoc", pattern = """^([^_]+)\.(kt|java)$""".toRegex())
        }

        suite<AbstractSafeDeleteTest> {
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

        suite<AbstractReferenceResolveTest> {
            model("resolve/references", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractReferenceResolveInJavaTest> {
            model("resolve/referenceInJava/binaryAndSource", pattern = JAVA)
            model("resolve/referenceInJava/sourceOnly", pattern = JAVA)
        }

        suite<AbstractReferenceToCompiledKotlinResolveInJavaTest> {
            model("resolve/referenceInJava/binaryAndSource", pattern = JAVA)
        }

        suite<AbstractReferenceResolveWithLibTest> {
            model("resolve/referenceWithLib", isRecursive = false)
        }

        suite<AbstractReferenceResolveInLibrarySourcesTest> {
            model("resolve/referenceInLib", isRecursive = false)
        }

        suite<AbstractReferenceToJavaWithWrongFileStructureTest> {
            model("resolve/referenceToJavaWithWrongFileStructure", isRecursive = false)
        }

        suite<AbstractFindUsagesTest> {
            model("findUsages/kotlin", pattern = """^(.+)\.0\.(kt|kts)$""".toRegex())
            model("findUsages/java", pattern = """^(.+)\.0\.java$""".toRegex())
            model("findUsages/propertyFiles", pattern = """^(.+)\.0\.properties$""".toRegex())
        }

        suite<AbstractFindUsagesWithDisableComponentSearchTest> {
            model("findUsages/kotlin/conventions/components", pattern = """^(.+)\.0\.(kt|kts)$""".toRegex())
        }

        suite<AbstractKotlinFindUsagesWithLibraryTest> {
            model("findUsages/libraryUsages", pattern = """^(.+)\.0\.kt$""".toRegex())
        }

        suite<AbstractMoveTest> {
            model("refactoring/move", pattern = TEST, flatten = true)
        }

        suite<AbstractCopyTest> {
            model("refactoring/copy", pattern = TEST, flatten = true)
        }

        suite<AbstractMultiModuleMoveTest> {
            model("refactoring/moveMultiModule", pattern = TEST, flatten = true)
        }

        suite<AbstractMultiModuleCopyTest> {
            model("refactoring/copyMultiModule", pattern = TEST, flatten = true)
        }

        suite<AbstractMultiModuleSafeDeleteTest> {
            model("refactoring/safeDeleteMultiModule", pattern = TEST, flatten = true)
        }

        suite<AbstractMultiFileIntentionTest> {
            model("multiFileIntentions", pattern = TEST, flatten = true, filenameStartsLowerCase = true)
        }

        suite<AbstractMultiFileLocalInspectionTest> {
            model("multiFileLocalInspections", pattern = TEST, flatten = true, filenameStartsLowerCase = true)
        }

        suite<AbstractMultiFileInspectionTest> {
            model("multiFileInspections", pattern = TEST, flatten = true)
        }

        suite<AbstractFormatterTest> {
            model("formatter", pattern = """^([^\.]+)\.after\.kt.*$""".toRegex())
            model("formatter/trailingComma", pattern = """^([^\.]+)\.call\.after\.kt.*$""".toRegex(), testMethodName = "doTestCallSite", testClassName = "FormatterCallSite")
            model("formatter", pattern = """^([^\.]+)\.after\.inv\.kt.*$""".toRegex(), testMethodName = "doTestInverted", testClassName = "FormatterInverted")
            model("formatter/trailingComma", pattern = """^([^\.]+)\.call\.after\.inv\.kt.*$""".toRegex(), testMethodName = "doTestInvertedCallSite", testClassName = "FormatterInvertedCallSite")
        }

        suite<AbstractTypingIndentationTestBase> {
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.kt.*$""".toRegex(), testMethodName = "doNewlineTest", testClassName = "DirectSettings")
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.inv\.kt.*$""".toRegex(), testMethodName = "doNewlineTestWithInvert", testClassName = "InvertedSettings")
        }

        suite<AbstractDiagnosticMessageTest> {
            model("diagnosticMessage", isRecursive = false)
        }

        suite<AbstractDiagnosticMessageJsTest> {
            model("diagnosticMessage/js", isRecursive = false, targetBackend = TargetBackend.JS)
        }

        suite<AbstractRenameTest> {
            model("refactoring/rename", pattern = TEST, flatten = true)
        }

        suite<AbstractMultiModuleRenameTest> {
            model("refactoring/renameMultiModule", pattern = TEST, flatten = true)
        }

        suite<AbstractOutOfBlockModificationTest> {
            model("codeInsight/outOfBlock", pattern = KT_OR_KTS)
        }

        suite<AbstractChangeLocalityDetectorTest> {
            model("codeInsight/changeLocality", pattern = KT_OR_KTS)
        }

        suite<AbstractDataFlowValueRenderingTest> {
            model("dataFlowValueRendering")
        }

        suite<AbstractJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", pattern = """^([^\.]+)\.java$""".toRegex())
        }

        suite<AbstractTextJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/plainTextConversion", pattern = """^([^\.]+)\.txt$""".toRegex())
        }

        suite<AbstractLiteralTextToKotlinCopyPasteTest> {
            model("copyPaste/plainTextLiteral", pattern = """^([^\.]+)\.txt$""".toRegex())
        }

        suite<AbstractLiteralKotlinToKotlinCopyPasteTest> {
            model("copyPaste/literal", pattern = """^([^\.]+)\.kt$""".toRegex())
        }

        suite<AbstractInsertImportOnPasteTest> {
            model("copyPaste/imports", pattern = KT_WITHOUT_DOTS, testMethodName = "doTestCopy", testClassName = "Copy", isRecursive = false)
            model("copyPaste/imports", pattern = KT_WITHOUT_DOTS, testMethodName = "doTestCut", testClassName = "Cut", isRecursive = false)
        }

        suite<AbstractMoveOnCutPasteTest> {
            model("copyPaste/moveDeclarations", pattern = KT_WITHOUT_DOTS, testMethodName = "doTest")
        }

        suite<AbstractUpdateKotlinCopyrightTest> {
            model("copyright", pattern = KT_OR_KTS, testMethodName = "doTest")
        }

        suite<AbstractHighlightExitPointsTest> {
            model("exitPoints")
        }

        suite<AbstractLineMarkersTest> {
            model("codeInsight/lineMarker")
        }

        suite<AbstractLineMarkersTestInLibrarySources> {
            model("codeInsightInLibrary/lineMarker", testMethodName = "doTestWithLibrary")
        }

        suite<AbstractMultiModuleLineMarkerTest> {
            model("multiModuleLineMarker", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractShortenRefsTest> {
            model("shortenRefs", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractAddImportTest> {
            model("addImport", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractAddImportAliasTest> {
            model("addImportAlias", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractSmartSelectionTest> {
            model("smartSelection", testMethodName = "doTestSmartSelection", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractKotlinFileStructureTest> {
            model("structureView/fileStructure", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractExpressionSelectionTest> {
            model("expressionSelection", testMethodName = "doTestExpressionSelection", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractCommonDecompiledTextTest> {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""".toRegex())
        }

        suite<AbstractJvmDecompiledTextTest> {
            model("decompiler/decompiledTextJvm", pattern = """^([^\.]+)$""".toRegex())
        }

        suite<AbstractCommonDecompiledTextFromJsMetadataTest> {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""".toRegex(), targetBackend = TargetBackend.JS)
        }

        suite<AbstractJsDecompiledTextFromJsMetadataTest> {
            model("decompiler/decompiledTextJs", pattern = """^([^\.]+)$""".toRegex(), targetBackend = TargetBackend.JS)
        }

        suite<AbstractClsStubBuilderTest> {
            model("decompiler/stubBuilder", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractJvmOptimizeImportsTest> {
            model("editor/optimizeImports/jvm", pattern = KT_OR_KTS_WITHOUT_DOTS)
            model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractJsOptimizeImportsTest> {
            model("editor/optimizeImports/js", pattern = KT_WITHOUT_DOTS)
            model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractKotlinExceptionFilterTest> {
            model("debugger/exceptionFilter", pattern = """^([^\.]+)$""".toRegex(), isRecursive = false)
        }

        suite<AbstractStubBuilderTest> {
            model("stubs", pattern = KT)
        }

        suite<AbstractMultiFileHighlightingTest> {
            model("multiFileHighlighting", isRecursive = false)
        }

        suite<AbstractMultiPlatformHighlightingTest> {
            model("multiModuleHighlighting/multiplatform/", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractMultiplatformAnalysisTest> {
            model("multiplatform", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractQuickFixMultiModuleTest> {
            model("multiModuleQuickFix", pattern = DIRECTORY, depth = 1)
        }

        suite<AbstractKotlinGotoImplementationMultiModuleTest> {
            model("navigation/implementations/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractKotlinGotoRelatedSymbolMultiModuleTest> {
            model("navigation/relatedSymbols/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractKotlinGotoSuperMultiModuleTest> {
            model("navigation/gotoSuper/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractExtractionTest> {
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

        suite<AbstractPullUpTest> {
            model("refactoring/pullUp/k2k", pattern = KT, flatten = true, testClassName = "K2K", testMethodName = "doKotlinTest")
            model("refactoring/pullUp/k2j", pattern = KT, flatten = true, testClassName = "K2J", testMethodName = "doKotlinTest")
            model("refactoring/pullUp/j2k", pattern = JAVA, flatten = true, testClassName = "J2K", testMethodName = "doJavaTest")
        }

        suite<AbstractPushDownTest> {
            model("refactoring/pushDown/k2k", pattern = KT, flatten = true, testClassName = "K2K", testMethodName = "doKotlinTest")
            model("refactoring/pushDown/k2j", pattern = KT, flatten = true, testClassName = "K2J", testMethodName = "doKotlinTest")
            model("refactoring/pushDown/j2k", pattern = JAVA, flatten = true, testClassName = "J2K", testMethodName = "doJavaTest")
        }

        suite<AbstractKotlinCoverageOutputFilesTest> {
            model("coverage/outputFiles")
        }

        suite<AbstractBytecodeToolWindowTest> {
            model("internal/toolWindow", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractReferenceResolveTest>("org.jetbrains.kotlin.idea.kdoc.KdocResolveTestGenerated") {
            model("kdoc/resolve")
        }

        suite<AbstractKDocHighlightingTest> {
            model("kdoc/highlighting")
        }

        suite<AbstractKDocTypingTest> {
            model("kdoc/typing")
        }

        suite<AbstractGenerateTestSupportMethodActionTest> {
            model("codeInsight/generate/testFrameworkSupport")
        }

        suite<AbstractGenerateHashCodeAndEqualsActionTest> {
            model("codeInsight/generate/equalsWithHashCode")
        }

        suite<AbstractCodeInsightActionTest> {
            model("codeInsight/generate/secondaryConstructors")
        }

        suite<AbstractGenerateToStringActionTest> {
            model("codeInsight/generate/toString")
        }

        suite<AbstractIdeReplCompletionTest> {
            model("repl/completion")
        }

        suite<AbstractPostfixTemplateProviderTest> {
            model("codeInsight/postfix")
        }

        suite<AbstractScriptConfigurationHighlightingTest> {
            model("script/definition/highlighting", pattern = DIRECTORY, isRecursive = false)
            model("script/definition/complex", pattern = DIRECTORY, isRecursive = false, testMethodName = "doComplexTest")
        }

        suite<AbstractScriptConfigurationNavigationTest> {
            model("script/definition/navigation", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractScriptConfigurationCompletionTest> {
            model("script/definition/completion", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractScriptDefinitionsOrderTest> {
            model("script/definition/order", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractNameSuggestionProviderTest> {
            model("refactoring/nameSuggestionProvider")
        }

        suite<AbstractSlicerTreeTest> {
            model("slicer")
        }

        suite<AbstractSlicerLeafGroupingTest> {
            model("slicer/inflow", flatten = true)
        }

        suite<AbstractSlicerNullnessGroupingTest> {
            model("slicer/inflow", flatten = true)
        }
    }

    group("fir", testDataPath = "../idea/testData") {
        suite<AbstractFirMultiModuleResolveTest> {
            model("fir/multiModule", isRecursive = false, pattern = DIRECTORY)
        }

        suite<AbstractFirLazyResolveTest> {
            model("fir/lazyResolve", pattern = TEST, flatten = true, filenameStartsLowerCase = true)
        }

        suite<AbstractFirReferenceResolveTest> {
            model("resolve/references", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractFirPsiCheckerTest> {
            model("checker", isRecursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/duplicateJvmSignature")
            model("checker/infos")
            model("checker/diagnosticsMessage")
        }
    }

    group("scripting-support") {
        suite<AbstractScratchRunActionTest> {
            model("scratch", pattern = KTS, testMethodName = "doScratchCompilingTest", testClassName = "ScratchCompiling", isRecursive = false)
            model("scratch", pattern = KTS, testMethodName = "doScratchReplTest", testClassName = "ScratchRepl", isRecursive = false)
            model("scratch/multiFile", pattern = DIRECTORY, testMethodName = "doScratchMultiFileTest", testClassName = "ScratchMultiFile", isRecursive = false)
            model("worksheet", pattern = WS_KTS, testMethodName = "doWorksheetCompilingTest", testClassName = "WorksheetCompiling", isRecursive = false)
            model("worksheet", pattern = WS_KTS, testMethodName = "doWorksheetReplTest", testClassName = "WorksheetRepl", isRecursive = false)
            model("worksheet/multiFile", pattern = DIRECTORY, testMethodName = "doWorksheetMultiFileTest", testClassName = "WorksheetMultiFile", isRecursive = false)
            model("scratch/rightPanelOutput", pattern = KTS, testMethodName = "doRightPreviewPanelOutputTest", testClassName = "ScratchRightPanelOutput", isRecursive = false)
        }

        suite<AbstractScratchLineMarkersTest> {
            model("scratch/lineMarker", testMethodName = "doScratchTest", pattern = KT_OR_KTS)
        }
    }

    group("maven") {
        suite<AbstractMavenConfigureProjectByChangingFileTest> {
            model("configurator/jvm", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestWithMaven")
            model("configurator/js", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestWithJSMaven")
        }

        suite<AbstractKotlinMavenInspectionTest> {
            model("maven-inspections", pattern = "^([\\w\\-]+).xml$".toRegex(), flatten = true)
        }
    }

    group("gradle/gradle-idea", testDataPath = "../../idea/testData") {
        suite<AbstractGradleConfigureProjectByChangingFileTest> {
            model("configuration/gradle", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestGradle")
            model("configuration/gsk", pattern = DIRECTORY, isRecursive = false, testMethodName = "doTestGradle")
        }
    }

    group("idea") {
        suite<AbstractResolveByStubTest> {
            model("compiler/loadJava/compiledKotlin")
        }

        suite<AbstractLoadJavaClsStubTest> {
            model("compiler/loadJava/compiledKotlin", testMethodName = "doTestCompiledKotlin")
        }

        suite<AbstractIdeLightClassTest> {
            model("compiler/asJava/lightClasses", excludedDirectories = listOf("delegation", "script"), pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractIdeLightClassForScriptTest> {
            model("compiler/asJava/script/ide", pattern = KT_OR_KTS_WITHOUT_DOTS)
        }

        suite<AbstractUltraLightClassSanityTest> {
            model("compiler/asJava/lightClasses", pattern = KT_OR_KTS)
        }
        suite<AbstractUltraLightClassLoadingTest> {
            model("compiler/asJava/ultraLightClasses", pattern = KT_OR_KTS)
        }

        suite<AbstractUltraLightFacadeClassTest> {
            model("compiler/asJava/ultraLightFacades", pattern = KT_OR_KTS)
        }

        suite<AbstractIdeCompiledLightClassTest> {
            model("compiler/asJava/lightClasses", excludedDirectories = listOf("local", "compilationErrors", "ideRegression"), pattern = KT_OR_KTS_WITHOUT_DOTS)
        }
    }

    group("completion") {
        suite<AbstractCompiledKotlinInJavaCompletionTest> {
            model("injava", pattern = JAVA, isRecursive = false)
        }

        suite<AbstractKotlinSourceInJavaCompletionTest> {
            model("injava", pattern = JAVA, isRecursive = false)
        }

        suite<AbstractKotlinStdLibInJavaCompletionTest> {
            model("injava/stdlib", pattern = JAVA, isRecursive = false)
        }

        suite<AbstractBasicCompletionWeigherTest> {
            model("weighers/basic", pattern = KT_OR_KTS_WITHOUT_DOTS)
        }

        suite<AbstractSmartCompletionWeigherTest> {
            model("weighers/smart", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractJSBasicCompletionTest> {
            model("basic/common")
            model("basic/js")
        }

        suite<AbstractJvmBasicCompletionTest> {
            model("basic/common")
            model("basic/java")
        }

        suite<AbstractJvmSmartCompletionTest> {
            model("smart")
        }

        suite<AbstractKeywordCompletionTest> {
            model("keywords", isRecursive = false)
        }

        suite<AbstractJvmWithLibBasicCompletionTest> {
            model("basic/withLib", isRecursive = false)
        }

        suite<AbstractBasicCompletionHandlerTest> {
            model("handlers/basic", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractSmartCompletionHandlerTest> {
            model("handlers/smart")
        }

        suite<AbstractKeywordCompletionHandlerTest> {
            model("handlers/keywords")
        }

        suite<AbstractCompletionCharFilterTest> {
            model("handlers/charFilter", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractMultiFileJvmBasicCompletionTest> {
            model("basic/multifile", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractMultiFileSmartCompletionTest> {
            model("smartMultiFile", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractJvmBasicCompletionTest>("org.jetbrains.kotlin.idea.completion.test.KDocCompletionTestGenerated") {
            model("kdoc")
        }

        suite<AbstractJava8BasicCompletionTest> {
            model("basic/java8")
        }

        suite<AbstractCompletionIncrementalResolveTest> {
            model("incrementalResolve")
        }

        suite<AbstractMultiPlatformCompletionTest> {
            model("multiPlatform", isRecursive = false, pattern = DIRECTORY)
        }
    }

    group("project-wizard/cli") {
        suite<AbstractYamlBuildFileGenerationTest> {
            model("buildFileGeneration", isRecursive = false, pattern = DIRECTORY)
        }
        suite<AbstractProjectTemplateBuildFileGenerationTest> {
            model("projectTemplatesBuildFileGeneration", isRecursive = false, pattern = DIRECTORY)
        }
    }

    group("project-wizard/idea", testDataPath = "../cli/testData") {
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
        suite<AbstractYamlNewWizardProjectImportTest> {
            allBuildSystemTests("buildFileGeneration")
        }
        suite<AbstractProjectTemplateNewWizardProjectImportTest> {
            allBuildSystemTests("projectTemplatesBuildFileGeneration")
        }
    }

    //TODO: move these tests into idea-completion module
    group("idea", testDataPath = "../completion/testData") {
        suite<AbstractCodeFragmentCompletionHandlerTest> {
            model("handlers/runtimeCast")
        }

        suite<AbstractCodeFragmentCompletionTest> {
            model("basic/codeFragments", pattern = KT)
        }
    }

    group("j2k/old") {
        suite<AbstractJavaToKotlinConverterSingleFileTest> {
            model("fileOrElement", pattern = JAVA)
        }
        suite<AbstractJavaToKotlinConverterMultiFileTest> {
            model("multiFile", pattern = DIRECTORY, isRecursive = false)
        }
        suite<AbstractJavaToKotlinConverterForWebDemoTest> {
            model("fileOrElement", pattern = JAVA)
        }
    }

    group("j2k/new") {
        suite<AbstractNewJavaToKotlinConverterSingleFileTest> {
            model("newJ2k", pattern = """^([^\.]+)\.java$""".toRegex())
        }
        suite<AbstractCommonConstraintCollectorTest> {
            model("inference/common")
        }
        suite<AbstractNullabilityInferenceTest> {
            model("inference/nullability")
        }
        suite<AbstractMutabilityInferenceTest> {
            model("inference/mutability")
        }
        suite<AbstractNewJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste", pattern = """^([^\.]+)\.java$""".toRegex())
        }
        suite<AbstractTextNewJavaToKotlinCopyPasteConversionTest> {
            model("copyPastePlainText", pattern = """^([^\.]+)\.txt$""".toRegex())
        }
        suite<AbstractNewJavaToKotlinConverterMultiFileTest> {
            model("multiFile", pattern = DIRECTORY, isRecursive = false)
        }
    }

    group("jps/jps-plugin") {
        suite<AbstractIncrementalJvmJpsTest> {
            model("incremental/multiModule/common", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/multiModule/jvm", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/multiModule/multiplatform/custom", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/pureKotlin", pattern = DIRECTORY, isRecursive = false)
            model("incremental/withJava", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/inlineFunCallSite", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/classHierarchyAffected", pattern = DIRECTORY, excludeParentDirs = true)
        }

        //actualizeMppJpsIncTestCaseDirs(testDataAbsoluteRoot, "incremental/multiModule/multiplatform/withGeneratedContent")

        suite<AbstractIncrementalJsJpsTest> {
            model("incremental/multiModule/common", pattern = DIRECTORY, excludeParentDirs = true)
        }

        suite<AbstractMultiplatformJpsTestWithGeneratedContent> {
            model(
                "incremental/multiModule/multiplatform/withGeneratedContent", isRecursive = true, pattern = DIRECTORY,
                testClassName = "MultiplatformMultiModule", excludeParentDirs = true
            )
        }

        suite<AbstractJvmLookupTrackerTest> {
            model("incremental/lookupTracker/jvm", pattern = DIRECTORY, isRecursive = false)
        }
        suite<AbstractJsLookupTrackerTest> {
            model("incremental/lookupTracker/js", pattern = DIRECTORY, isRecursive = false)
        }
        suite<AbstractJsKlibLookupTrackerTest> {
            // todo: investigate why lookups are different from non-klib js
            model("incremental/lookupTracker/jsKlib", pattern = DIRECTORY, isRecursive = false)
        }

        suite<AbstractIncrementalLazyCachesTest> {
            model("incremental/lazyKotlinCaches", pattern = DIRECTORY, excludeParentDirs = true)
            model("incremental/changeIncrementalOption", pattern = DIRECTORY, excludeParentDirs = true)
        }

        suite<AbstractIncrementalCacheVersionChangedTest> {
            model("incremental/cacheVersionChanged", pattern = DIRECTORY, excludeParentDirs = true)
        }

        suite<AbstractDataContainerVersionChangedTest> {
            model("incremental/cacheVersionChanged", pattern = DIRECTORY, excludeParentDirs = true)
        }
    }

    group("jps/jps-plugin") {
        fun MutableTSuite.commonProtoComparisonTests() {
            model("comparison/classSignatureChange", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/classPrivateOnlyChange", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/classMembersOnlyChanged", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/packageMembers", pattern = DIRECTORY, excludeParentDirs = true)
            model("comparison/unchanged", pattern = DIRECTORY, excludeParentDirs = true)
        }

        suite<AbstractJvmProtoComparisonTest> {
            commonProtoComparisonTests()
            model("comparison/jvmOnly", pattern = DIRECTORY, excludeParentDirs = true)
        }

        suite<AbstractJsProtoComparisonTest> {
            commonProtoComparisonTests()
            model("comparison/jsOnly", pattern = DIRECTORY, excludeParentDirs = true)
        }
    }

    group("performance-tests", testDataPath = "../idea/testData") {
        suite<AbstractPerformanceJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", testMethodName = "doPerfTest", pattern = """^([^\.]+)\.java$""".toRegex())
        }

        suite<AbstractPerformanceNewJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", testMethodName = "doPerfTest", pattern = """^([^\.]+)\.java$""".toRegex())
        }

        suite<AbstractPerformanceLiteralKotlinToKotlinCopyPasteTest> {
            model("copyPaste/literal", testMethodName = "doPerfTest", pattern = """^([^\.]+)\.kt$""".toRegex())
        }

        suite<AbstractPerformanceHighlightingTest> {
            model("highlighter", testMethodName = "doPerfTest")
        }

        suite<AbstractPerformanceAddImportTest> {
            model("addImport", testMethodName = "doPerfTest", pattern = KT_WITHOUT_DOTS)
        }
    }

    group("performance-tests", testDataPath = "../completion/testData") {
        suite<AbstractPerformanceCompletionIncrementalResolveTest> {
            model("incrementalResolve", testMethodName = "doPerfTest")
        }

        suite<AbstractPerformanceBasicCompletionHandlerTest> {
            model("handlers/basic", testMethodName = "doPerfTest", pattern = KT_WITHOUT_DOTS)
        }

        suite<AbstractPerformanceSmartCompletionHandlerTest> {
            model("handlers/smart", testMethodName = "doPerfTest")
        }

        suite<AbstractPerformanceKeywordCompletionHandlerTest> {
            model("handlers/keywords", testMethodName = "doPerfTest")
        }

        suite<AbstractPerformanceCompletionCharFilterTest> {
            model("handlers/charFilter", testMethodName = "doPerfTest", pattern = KT_WITHOUT_DOTS)
        }
    }
}
