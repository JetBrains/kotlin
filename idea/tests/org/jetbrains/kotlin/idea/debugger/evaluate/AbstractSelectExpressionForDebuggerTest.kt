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

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert

abstract class AbstractSelectExpressionForDebuggerTest : LightCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        invalidateLibraryCache(project)
    }

    fun doTest(path: String) {
        doTest(path, true)
    }

    fun doTestWoMethodCalls(path: String) {
        doTest(path, false)
    }

    fun doTest(path: String, allowMethodCalls: Boolean) {
        myFixture.configureByFile(path)

        val elementAt = myFixture.file?.findElementAt(myFixture.caretOffset)!!
        val selectedExpression = KotlinEditorTextProvider.findExpressionInner(elementAt, allowMethodCalls)

        val expected = InTextDirectivesUtils.findStringWithPrefixes(myFixture.file?.text!!, "// EXPECTED: ")
        val actualResult = if (selectedExpression != null)
            KotlinEditorTextProvider.getElementInfo(selectedExpression) { it.text }
        else
            "null"

        Assert.assertEquals("Another expression should be selected", expected, actualResult)
    }

    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/debugger/selectExpression"
}
