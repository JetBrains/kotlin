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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.KotlinQuickDocumentationProvider
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.junit.Assert

class QuickDocNavigationTest() : KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/navigate/"
    }

    override fun getProjectDescriptor() = ProjectDescriptorWithStdlibSources.INSTANCE

    fun testSimple() {
        val target = resolveDocLink("C")
        UsefulTestCase.assertInstanceOf(target, KtClass::class.java)
        Assert.assertEquals("C", (target as KtClass).name)
    }

    fun testJdkClass() {
        val target = resolveDocLink("ArrayList")
        UsefulTestCase.assertInstanceOf(target, PsiClass::class.java)
        Assert.assertEquals("ArrayList", (target as PsiClass).name)
    }

    fun testStdlibFunction() {
        val target = resolveDocLink("reader")
        UsefulTestCase.assertInstanceOf(target, KtFunction::class.java)
        Assert.assertEquals("reader", (target as KtFunction).name)

        val secondaryTarget = KotlinQuickDocumentationProvider().getDocumentationElementForLink(
                myFixture.psiManager, "InputStream", target)
        UsefulTestCase.assertInstanceOf(secondaryTarget, PsiClass::class.java)
        Assert.assertEquals("InputStream", (secondaryTarget as PsiClass).name)
    }

    private fun resolveDocLink(linkText: String): PsiElement? {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val source = myFixture.elementAtCaret.getParentOfType<KtFunction>(false)
        return KotlinQuickDocumentationProvider().getDocumentationElementForLink(
                myFixture.psiManager, linkText, source)
    }
}
