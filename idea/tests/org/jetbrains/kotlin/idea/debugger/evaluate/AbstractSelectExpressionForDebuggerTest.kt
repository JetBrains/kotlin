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
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert

public abstract class AbstractSelectExpressionForDebuggerTest : LightCodeInsightFixtureTestCase() {

    fun doTest(path: String) {
        doTest(path, true)
    }

    fun doTestWoMethodCalls(path: String) {
        doTest(path, false)
    }

    fun doTest(path: String, allowMethodCalls: Boolean) {
        myFixture.configureByFile(path)

        val elementAt = myFixture.getFile()?.findElementAt(myFixture.getCaretOffset())!!
        val selectedExpression = KotlinEditorTextProvider.findExpressionInner(elementAt, allowMethodCalls)

        val expected = InTextDirectivesUtils.findStringWithPrefixes(myFixture.getFile()?.getText()!!, "// EXPECTED: ")
        Assert.assertEquals("Another expression should be selected", expected, selectedExpression?.getText() ?: "null")
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/debugger/selectExpression";
}
