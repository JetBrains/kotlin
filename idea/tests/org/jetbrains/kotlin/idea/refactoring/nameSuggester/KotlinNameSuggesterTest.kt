/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.nameSuggester

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.IntroduceRefactoringException
import org.jetbrains.kotlin.idea.refactoring.selectElement
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.lang.AssertionError

class KotlinNameSuggesterTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testArrayList() = doTest()

    fun testGetterSure() = doTest()

    fun testNameArrayOfClasses() = doTest()

    fun testNameArrayOfStrings() = doTest()

    fun testNamePrimitiveArray() = doTest()

    fun testNameCallExpression() = doTest()

    fun testNameClassCamelHump() = doTest()

    fun testNameLong() = doTest()

    fun testNameReferenceExpression() = doTest()

    fun testNameString() = doTest()

    fun testAnonymousObject() = doTest()

    fun testAnonymousObjectWithSuper() = doTest()

    fun testArrayOfObjectsType() = doTest()

    fun testURL() = doTest()

    fun testParameterNameByArgumentExpression() = doTest()

    fun testParameterNameByParenthesizedArgumentExpression() = doTest()

    fun testIdWithDigits() = doTest()

    fun testIdWithNonASCII() = doTest()

    fun testFunction1() = doTest()

    fun testFunction2() = doTest()

    fun testExtensionFunction1() = doTest()

    fun testExtensionFunction2() = doTest()

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/refactoring/nameSuggester"
    }

    private fun doTest() {
        try {
            myFixture.configureByFile(getTestName(false) + ".kt")
            val file = myFixture.file as KtFile
            val expectedResultText = KotlinTestUtils.getLastCommentInFile(file)

            selectElement(myFixture.editor, file, listOf(CodeInsightUtils.ElementKind.EXPRESSION)) {
                val names = KotlinNameSuggester
                    .suggestNamesByExpressionAndType(
                        it as KtExpression,
                        null,
                        it.analyze(BodyResolveMode.PARTIAL),
                        { true },
                        "value"
                    )
                    .sorted()
                val result = StringUtil.join(names, "\n").trim()
                assertEquals(expectedResultText, result)
            }
        } catch (e: IntroduceRefactoringException) {
            throw AssertionError("Failed to find expression: " + e.message)
        }
    }
}
