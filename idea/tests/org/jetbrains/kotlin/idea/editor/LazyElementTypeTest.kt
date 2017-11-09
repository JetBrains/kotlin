/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.junit.Assert

class LazyElementTypeTest : KotlinLightCodeInsightFixtureTestCaseBase() {
    fun testSplitArrow() = reparse("val t = { a: Int -<caret>> }", ' ')
    fun testDeleteArrow() = reparse("val t = { a: Int -><caret> }", EditorTestUtil.BACKSPACE_FAKE_CHAR)

    fun testReformatNearArrow() = noReparse("val t = { a: Int<caret>-> }", ' ')
    fun testChangeAfterArrow() = noReparse("val t = { a: Int -> <caret> }", 'a')
    fun testDeleteIrrelevantArrow() = noReparse("val t = { a: Int -> (1..3).filter { b -><caret> b > 2 } }", EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testReformatNearLambdaStart() = noReparse("val t = {<caret>a: Int -> }", ' ')
    fun testNoArrow() = noReparse("val t = { <caret> }", 'a')

    fun reparse(text: String, char: Char): Unit = doTest(text, char, true)
    fun noReparse(text: String, char: Char): Unit = doTest(text, char, false)

    fun doTest(text: String, char: Char, reparse: Boolean) {
        val file = myFixture.configureByText("a.kt", text.trimMargin())

        val lambdaExpressionBefore = PsiTreeUtil.findChildOfType(file, KtLambdaExpression::class.java)

        EditorTestUtil.performTypingAction(myFixture.editor, char)
        PsiDocumentManager.getInstance(LightPlatformTestCase.getProject()).commitDocument(myFixture.getDocument(file))

        val lambdaExpressionAfter = PsiTreeUtil.findChildOfType(file, KtLambdaExpression::class.java)

        val actualReparse = lambdaExpressionAfter != lambdaExpressionBefore

        Assert.assertEquals("Lazy element behaviour was unexpected", reparse, actualReparse)
    }

    override fun getProjectDescriptor() = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
}