/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.TestRoot;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

@SuppressWarnings("all")
@TestRoot("idea")
@TestMetadata("testData/indentationOnNewline")
@TestDataPath("$PROJECT_ROOT")
public class PerformanceTypingIndentationTestGenerated extends AbstractPerformanceTypingIndentationTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doPerfTest, this, testDataFilePath);
    }

    @TestMetadata("AfterCatch.kt")
    public void testAfterCatch() throws Exception {
        runTest("testData/indentationOnNewline/AfterCatch.kt");
    }

    @TestMetadata("AfterClassNameBeforeFun.kt")
    public void testAfterClassNameBeforeFun() throws Exception {
        runTest("testData/indentationOnNewline/AfterClassNameBeforeFun.kt");
    }

    @TestMetadata("AfterFinally.kt")
    public void testAfterFinally() throws Exception {
        runTest("testData/indentationOnNewline/AfterFinally.kt");
    }

    @TestMetadata("AfterImport.kt")
    public void testAfterImport() throws Exception {
        runTest("testData/indentationOnNewline/AfterImport.kt");
    }

    @TestMetadata("AfterTry.kt")
    public void testAfterTry() throws Exception {
        runTest("testData/indentationOnNewline/AfterTry.kt");
    }

    public void testAllFilesPresentInIndentationOnNewline() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("testData/indentationOnNewline"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("Annotation.kt")
    public void testAnnotation() throws Exception {
        runTest("testData/indentationOnNewline/Annotation.kt");
    }

    @TestMetadata("AnnotationInDeclaration.kt")
    public void testAnnotationInDeclaration() throws Exception {
        runTest("testData/indentationOnNewline/AnnotationInDeclaration.kt");
    }

    @TestMetadata("ArgumentListNormalIndent.kt")
    public void testArgumentListNormalIndent() throws Exception {
        runTest("testData/indentationOnNewline/ArgumentListNormalIndent.kt");
    }

    @TestMetadata("AssignmentAfterEq.kt")
    public void testAssignmentAfterEq() throws Exception {
        runTest("testData/indentationOnNewline/AssignmentAfterEq.kt");
    }

    @TestMetadata("BinaryWithTypeExpressions.kt")
    public void testBinaryWithTypeExpressions() throws Exception {
        runTest("testData/indentationOnNewline/BinaryWithTypeExpressions.kt");
    }

    @TestMetadata("ConsecutiveCallsAfterDot.kt")
    public void testConsecutiveCallsAfterDot() throws Exception {
        runTest("testData/indentationOnNewline/ConsecutiveCallsAfterDot.kt");
    }

    @TestMetadata("ConsecutiveCallsInSaeCallsMiddle.kt")
    public void testConsecutiveCallsInSaeCallsMiddle() throws Exception {
        runTest("testData/indentationOnNewline/ConsecutiveCallsInSaeCallsMiddle.kt");
    }

    @TestMetadata("ConsecutiveCallsInSafeCallsEnd.kt")
    public void testConsecutiveCallsInSafeCallsEnd() throws Exception {
        runTest("testData/indentationOnNewline/ConsecutiveCallsInSafeCallsEnd.kt");
    }

    @TestMetadata("DoInFun.kt")
    public void testDoInFun() throws Exception {
        runTest("testData/indentationOnNewline/DoInFun.kt");
    }

    @TestMetadata("EmptyParameters.kt")
    public void testEmptyParameters() throws Exception {
        runTest("testData/indentationOnNewline/EmptyParameters.kt");
    }

    @TestMetadata("For.kt")
    public void testFor() throws Exception {
        runTest("testData/indentationOnNewline/For.kt");
    }

    @TestMetadata("FunctionBlock.kt")
    public void testFunctionBlock() throws Exception {
        runTest("testData/indentationOnNewline/FunctionBlock.kt");
    }

    @TestMetadata("FunctionWithInference.kt")
    public void testFunctionWithInference() throws Exception {
        runTest("testData/indentationOnNewline/FunctionWithInference.kt");
    }

    @TestMetadata("If.kt")
    public void testIf() throws Exception {
        runTest("testData/indentationOnNewline/If.kt");
    }

    @TestMetadata("InBinaryExpressionInMiddle.kt")
    public void testInBinaryExpressionInMiddle() throws Exception {
        runTest("testData/indentationOnNewline/InBinaryExpressionInMiddle.kt");
    }

    @TestMetadata("InBinaryExpressionUnfinished.kt")
    public void testInBinaryExpressionUnfinished() throws Exception {
        runTest("testData/indentationOnNewline/InBinaryExpressionUnfinished.kt");
    }

    @TestMetadata("InBinaryExpressionUnfinishedInIf.kt")
    public void testInBinaryExpressionUnfinishedInIf() throws Exception {
        runTest("testData/indentationOnNewline/InBinaryExpressionUnfinishedInIf.kt");
    }

    @TestMetadata("InBinaryExpressionsBeforeCloseParenthesis.kt")
    public void testInBinaryExpressionsBeforeCloseParenthesis() throws Exception {
        runTest("testData/indentationOnNewline/InBinaryExpressionsBeforeCloseParenthesis.kt");
    }

    @TestMetadata("InDelegationListAfterColon.kt")
    public void testInDelegationListAfterColon() throws Exception {
        runTest("testData/indentationOnNewline/InDelegationListAfterColon.kt");
    }

    @TestMetadata("InDelegationListAfterComma.kt")
    public void testInDelegationListAfterComma() throws Exception {
        runTest("testData/indentationOnNewline/InDelegationListAfterComma.kt");
    }

    @TestMetadata("InDelegationListNotEmpty.kt")
    public void testInDelegationListNotEmpty() throws Exception {
        runTest("testData/indentationOnNewline/InDelegationListNotEmpty.kt");
    }

    @TestMetadata("InEnumAfterSemicolon.kt")
    public void testInEnumAfterSemicolon() throws Exception {
        runTest("testData/indentationOnNewline/InEnumAfterSemicolon.kt");
    }

    @TestMetadata("InEnumInitializerListAfterComma.kt")
    public void testInEnumInitializerListAfterComma() throws Exception {
        runTest("testData/indentationOnNewline/InEnumInitializerListAfterComma.kt");
    }

    @TestMetadata("InEnumInitializerListNotEmpty.kt")
    public void testInEnumInitializerListNotEmpty() throws Exception {
        runTest("testData/indentationOnNewline/InEnumInitializerListNotEmpty.kt");
    }

    @TestMetadata("InExpressionsParentheses.kt")
    public void testInExpressionsParentheses() throws Exception {
        runTest("testData/indentationOnNewline/InExpressionsParentheses.kt");
    }

    @TestMetadata("InExpressionsParenthesesBeforeOperand.kt")
    public void testInExpressionsParenthesesBeforeOperand() throws Exception {
        runTest("testData/indentationOnNewline/InExpressionsParenthesesBeforeOperand.kt");
    }

    @TestMetadata("InLabmdaAfterArrow.kt")
    public void testInLabmdaAfterArrow() throws Exception {
        runTest("testData/indentationOnNewline/InLabmdaAfterArrow.kt");
    }

    @TestMetadata("InLambdaAfterArrowWithSpaces.kt")
    public void testInLambdaAfterArrowWithSpaces() throws Exception {
        runTest("testData/indentationOnNewline/InLambdaAfterArrowWithSpaces.kt");
    }

    @TestMetadata("InLambdaBeforeParams.kt")
    public void testInLambdaBeforeParams() throws Exception {
        runTest("testData/indentationOnNewline/InLambdaBeforeParams.kt");
    }

    @TestMetadata("InLambdaInsideChainCallSameLine.kt")
    public void testInLambdaInsideChainCallSameLine() throws Exception {
        runTest("testData/indentationOnNewline/InLambdaInsideChainCallSameLine.kt");
    }

    @TestMetadata("InLambdaInsideChainCallSameLineWithSpaces.kt")
    public void testInLambdaInsideChainCallSameLineWithSpaces() throws Exception {
        runTest("testData/indentationOnNewline/InLambdaInsideChainCallSameLineWithSpaces.kt");
    }

    @TestMetadata("InLambdaInsideChainCallWithNewLine.kt")
    public void testInLambdaInsideChainCallWithNewLine() throws Exception {
        runTest("testData/indentationOnNewline/InLambdaInsideChainCallWithNewLine.kt");
    }

    @TestMetadata("InLambdaInsideChainCallWithNewLineWithSpaces.kt")
    public void testInLambdaInsideChainCallWithNewLineWithSpaces() throws Exception {
        runTest("testData/indentationOnNewline/InLambdaInsideChainCallWithNewLineWithSpaces.kt");
    }

    @TestMetadata("InMultilineLambdaAfterArrow.kt")
    public void testInMultilineLambdaAfterArrow() throws Exception {
        runTest("testData/indentationOnNewline/InMultilineLambdaAfterArrow.kt");
    }

    @TestMetadata("IsExpressionAfterIs.kt")
    public void testIsExpressionAfterIs() throws Exception {
        runTest("testData/indentationOnNewline/IsExpressionAfterIs.kt");
    }

    @TestMetadata("KT20783.kt")
    public void testKT20783() throws Exception {
        runTest("testData/indentationOnNewline/KT20783.kt");
    }

    @TestMetadata("LargeFile.kt")
    public void testLargeFile() throws Exception {
        runTest("testData/indentationOnNewline/LargeFile.kt");
    }

    @TestMetadata("ModifierListInUnfinishedDeclaration.kt")
    public void testModifierListInUnfinishedDeclaration() throws Exception {
        runTest("testData/indentationOnNewline/ModifierListInUnfinishedDeclaration.kt");
    }

    @TestMetadata("MultideclarationAfterEq.kt")
    public void testMultideclarationAfterEq() throws Exception {
        runTest("testData/indentationOnNewline/MultideclarationAfterEq.kt");
    }

    @TestMetadata("MultideclarationBeforeEq.kt")
    public void testMultideclarationBeforeEq() throws Exception {
        runTest("testData/indentationOnNewline/MultideclarationBeforeEq.kt");
    }

    @TestMetadata("NotFirstParameter.kt")
    public void testNotFirstParameter() throws Exception {
        runTest("testData/indentationOnNewline/NotFirstParameter.kt");
    }

    @TestMetadata("PropertyWithInference.kt")
    public void testPropertyWithInference() throws Exception {
        runTest("testData/indentationOnNewline/PropertyWithInference.kt");
    }

    @TestMetadata("ReturnContinue.kt")
    public void testReturnContinue() throws Exception {
        runTest("testData/indentationOnNewline/ReturnContinue.kt");
    }

    @TestMetadata("SettingAlignMultilineParametersInCalls.kt")
    public void testSettingAlignMultilineParametersInCalls() throws Exception {
        runTest("testData/indentationOnNewline/SettingAlignMultilineParametersInCalls.kt");
    }

    @TestMetadata("While.kt")
    public void testWhile() throws Exception {
        runTest("testData/indentationOnNewline/While.kt");
    }

    @TestMetadata("testData/indentationOnNewline/script")
    @TestDataPath("$PROJECT_ROOT")
    public static class Script extends AbstractPerformanceTypingIndentationTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doPerfTest, this, testDataFilePath);
        }

        public void testAllFilesPresentInScript() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("testData/indentationOnNewline/script"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
        }

        @TestMetadata("ScriptAfterClosingBrace.kts")
        public void testScriptAfterClosingBrace() throws Exception {
            runTest("testData/indentationOnNewline/script/ScriptAfterClosingBrace.kts");
        }

        @TestMetadata("ScriptAfterExpression.kts")
        public void testScriptAfterExpression() throws Exception {
            runTest("testData/indentationOnNewline/script/ScriptAfterExpression.kts");
        }

        @TestMetadata("ScriptAfterFun.kts")
        public void testScriptAfterFun() throws Exception {
            runTest("testData/indentationOnNewline/script/ScriptAfterFun.kts");
        }

        @TestMetadata("ScriptAfterImport.kts")
        public void testScriptAfterImport() throws Exception {
            runTest("testData/indentationOnNewline/script/ScriptAfterImport.kts");
        }

        @TestMetadata("ScriptBetweenFunctionCalls.kts")
        public void testScriptBetweenFunctionCalls() throws Exception {
            runTest("testData/indentationOnNewline/script/ScriptBetweenFunctionCalls.kts");
        }

        @TestMetadata("ScriptInsideFun.kts")
        public void testScriptInsideFun() throws Exception {
            runTest("testData/indentationOnNewline/script/ScriptInsideFun.kts");
        }
    }
}
