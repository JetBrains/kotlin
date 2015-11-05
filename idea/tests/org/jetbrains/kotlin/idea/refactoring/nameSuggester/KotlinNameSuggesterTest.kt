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
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.KotlinTestUtils

public class KotlinNameSuggesterTest : LightCodeInsightFixtureTestCase() {
    public fun testArrayList() { doTest() }

    public fun testGetterSure() { doTest() }

    public fun testNameArrayOfClasses() { doTest() }

    public fun testNameArrayOfStrings() { doTest() }

    public fun testNamePrimitiveArray() { doTest() }

    public fun testNameCallExpression() { doTest() }

    public fun testNameClassCamelHump() { doTest() }

    public fun testNameLong() { doTest() }

    public fun testNameReferenceExpression() { doTest() }

    public fun testNameString() { doTest() }

    public fun testAnonymousObject() { doTest() }

    public fun testAnonymousObjectWithSuper() { doTest() }

    public fun testArrayOfObjectsType() { doTest() }

    public fun testURL() { doTest() }

    override fun setUp() {
        super.setUp()
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/refactoring/nameSuggester")
    }

    private fun doTest() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val file = myFixture.getFile() as KtFile
        val expectedResultText = KotlinTestUtils.getLastCommentInFile(file)
        try {
            KotlinRefactoringUtil.selectExpression(myFixture.getEditor(), file, object : KotlinRefactoringUtil.SelectExpressionCallback {
                override fun run(expression: KtExpression?) {
                    val names = KotlinNameSuggester.suggestNamesByExpressionAndType(expression!!, expression.analyze(BodyResolveMode.PARTIAL), { true }, "value").sorted()
                    val result = StringUtil.join(names, "\n").trim()
                    assertEquals(expectedResultText, result)
                }
            })
        }
        catch (e: KotlinRefactoringUtil.IntroduceRefactoringException) {
            throw AssertionError("Failed to find expression: " + e.getMessage())
        }
    }
}
