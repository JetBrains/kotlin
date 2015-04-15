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

package org.jetbrains.kotlin.idea.editor.quickDoc

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinQuickDocumentationProvider
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.junit.Assert

public class QuickDocNavigationTest() : LightPlatformCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/navigate/"
    }

    public fun testSimple() {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val source = myFixture.getElementAtCaret().getParentOfType<JetFunction>(false)
        val target = KotlinQuickDocumentationProvider().getDocumentationElementForLink(
                myFixture.getPsiManager(), "C", source);
        Assert.assertTrue(target is JetClass)
        Assert.assertEquals("C", (target as JetClass).getName())
    }
}
