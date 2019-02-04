/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.EditorTestUtil.*
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.junit.Assert

class LazyElementTypeTest : KotlinLightCodeInsightFixtureTestCaseBase() {
    fun testSplitArrow() = reparse("val t = { a: Int -<caret>> }", ' ')
    fun testDeleteArrow() = reparse("val t = { a: Int -><caret> }", BACKSPACE_FAKE_CHAR)

    fun testReformatNearArrow() = noReparse("val t = { a: Int<caret>-> }", ' ')
    fun testChangeAfterArrow() = noReparse("val t = { a: Int -> <caret> }", 'a')
    fun testDeleteIrrelevantArrow() = noReparse("val t = { a: Int -> (1..3).filter { b -><caret> b > 2 } }", BACKSPACE_FAKE_CHAR)
    fun testReformatNearLambdaStart() = noReparse("val t = {<caret>a: Int -> }", ' ')
    fun testNoArrow() = noReparse("val t = { <caret> }", 'a')

    fun testAfterRemovingParameterComma() = reparse(inIf("{t,<caret>}"), BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingNoParameterComma() = noReparse(inIf("{,<caret>}"), BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingNotLastParameterComma() = noReparse(inIf("{a, b,<caret>}"), BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingSecondParameter() = noReparse(inIf("{a,b<caret>}"), BACKSPACE_FAKE_CHAR)
    fun testAfterFirstParameterRenamed() = noReparse(inIf("{a<caret>,}"), 'b')

    fun testAfterRemovingFirstParameterWithOther() = reparse(inIf("{a<caret>,b}"), BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingFirstParameter() = reparse(inIf("{a<caret>,}"), BACKSPACE_FAKE_CHAR)
    fun testAfterTypeComma() = reparse(inIf("{a<caret>}"), ',')

    fun reparse(text: String, char: Char): Unit = doTest(text, char, true)
    fun noReparse(text: String, char: Char): Unit = doTest(text, char, false)

    fun doTest(text: String, char: Char, reparse: Boolean) {
        val file = myFixture.configureByText("a.kt", text.trimMargin())

        val lambdaExpressionBefore = PsiTreeUtil.findChildOfType(file, KtLambdaExpression::class.java)

        performTypingAction(myFixture.editor, char)
        PsiDocumentManager.getInstance(LightPlatformTestCase.getProject()).commitDocument(myFixture.getDocument(file))

        val lambdaExpressionAfter = PsiTreeUtil.findChildOfType(file, KtLambdaExpression::class.java)

        val actualReparse = lambdaExpressionAfter != lambdaExpressionBefore

        Assert.assertEquals("Lazy element behaviour was unexpected", reparse, actualReparse)
    }

    private fun inIf(lambda: String) = "val t: Unit = if (true) $lambda else {}"

    override fun getProjectDescriptor() = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
}