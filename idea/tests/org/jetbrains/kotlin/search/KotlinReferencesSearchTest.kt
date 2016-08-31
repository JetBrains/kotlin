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

package org.jetbrains.kotlin.search

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.junit.Assert
import java.io.File

class KotlinReferencesSearchTest(): AbstractSearcherTest() {
    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/search/references").path + File.separator
    }

    fun testPlus() {
        val refs = doTest<KtFunction>()
        Assert.assertEquals(3, refs.size)
        Assert.assertEquals("+", refs[0].canonicalText)
        Assert.assertEquals("plus", refs[1].canonicalText)
        Assert.assertEquals("plus", refs[2].canonicalText)
    }

    fun testParam() {
        val refs = doTest<KtParameter>()
        Assert.assertEquals(3, refs.size)
        Assert.assertEquals("n", refs[0].canonicalText)
        Assert.assertEquals("component1", refs[1].canonicalText)
        Assert.assertTrue(refs[2] is KtDestructuringDeclarationReference)
    }

    fun testComponentFun() {
        val refs = doTest<KtFunction>()
        Assert.assertEquals(2, refs.size)
        Assert.assertEquals("component1", refs[0].canonicalText)
        Assert.assertTrue(refs[1] is KtDestructuringDeclarationReference)
    }

    fun testInvokeFun() {
        val refs = doTest<KtFunction>()
        Assert.assertEquals(2, refs.size)
        Assert.assertEquals("invoke", refs[0].canonicalText)
        Assert.assertTrue(refs[1] is KtInvokeFunctionReference)
    }

    // workaround for KT-9788 AssertionError from backand when we read field from inline function
    private val myFixtureProxy: JavaCodeInsightTestFixture get() = myFixture

    private inline fun <reified T: PsiElement> doTest(): List<PsiReference> {
        val psiFile = myFixtureProxy.configureByFile(fileName)
        val func = myFixtureProxy.elementAtCaret.getParentOfType<T>(false)!!
        val refs = ReferencesSearch.search(func).findAll().sortedBy { it.element.textRange.startOffset }

        // check that local reference search gives the same result
        try {
            ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED
            val localRefs = ReferencesSearch.search(func, LocalSearchScope(psiFile)).findAll()
            Assert.assertEquals(refs.size, localRefs.size)
        }
        finally {
            ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART
        }

        return refs
    }
}
