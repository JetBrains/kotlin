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
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinQuickDocumentationProvider
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtClass
import org.junit.Assert

class QuickDocInCompletionTest(): LightPlatformCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/inCompletion/"
    }
    fun testSimple() {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val lookupElements = myFixture.completeBasic()
        val lookupObject = lookupElements.first().`object`
        val element = KotlinQuickDocumentationProvider().getDocumentationElementForLookupItem(
                myFixture.psiManager, lookupObject, null)
        Assert.assertTrue(element is KtClass)
    }
}