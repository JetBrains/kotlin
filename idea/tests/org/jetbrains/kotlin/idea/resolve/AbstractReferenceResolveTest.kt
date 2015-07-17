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

package org.jetbrains.kotlin.idea.resolve

import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.ReferenceUtils
import org.jetbrains.kotlin.test.util.configureWithExtraFile
import org.junit.Assert
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

public abstract class AbstractReferenceResolveTest : KotlinLightPlatformCodeInsightFixtureTestCase() {
    public class ExpectedResolveData(private val shouldBeUnresolved: Boolean?, public val referenceString: String) {

        public fun shouldBeUnresolved(): Boolean {
            return shouldBeUnresolved!!
        }
    }

    protected open fun doTest(path: String) {
        assert(path.endsWith(".kt")) { path }
        myFixture.configureWithExtraFile(path, ".Data")
        performChecks()
    }

    protected fun performChecks() {
        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.getFile().getText(), MULTIRESOLVE)) {
            doMultiResolveTest()
        }
        else {
            doSingleResolveTest()
        }
    }

    protected fun doSingleResolveTest() {
        forEachCaret { index, offset ->
            val expectedResolveData = readResolveData(myFixture.getFile().getText(), index)
            val psiReference = myFixture.getFile().findReferenceAt(offset)
            checkReferenceResolve(expectedResolveData, offset, psiReference)
        }
    }

    protected fun doMultiResolveTest() {
        forEachCaret { index, offset ->
            val expectedReferences = getExpectedReferences(myFixture.getFile().getText(), index)

            val psiReference = myFixture.getFile().findReferenceAt(offset)
            assertTrue(psiReference is PsiPolyVariantReference)
            psiReference as PsiPolyVariantReference

            val results = psiReference.multiResolve(true)

            val actualResolvedTo = Lists.newArrayList<String>()
            for (result in results) {
                actualResolvedTo.add(ReferenceUtils.renderAsGotoImplementation(result.getElement()!!))
            }

            UsefulTestCase.assertOrderedEquals("Not matching for reference #$index", actualResolvedTo.sort(), expectedReferences.sort())
        }
    }

    private fun forEachCaret(action: (index: Int, offset: Int) -> Unit) {
        val offsets = myFixture.getEditor().getCaretModel().getAllCarets().map { it.getOffset() }
        val singleCaret = offsets.size() == 1
        for ((index, offset) in offsets.withIndex()) {
            action(if (singleCaret) -1 else index + 1, offset)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor? = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun getTestDataPath() = "./"

    companion object {
        public val MULTIRESOLVE: String = "MULTIRESOLVE"
        public val REF_EMPTY: String = "REF_EMPTY"

        public fun readResolveData(fileText: String, index: Int): ExpectedResolveData {
            val shouldBeUnresolved = InTextDirectivesUtils.isDirectiveDefined(fileText, REF_EMPTY)
            val refs = getExpectedReferences(fileText, index)

            val referenceToString: String
            if (shouldBeUnresolved) {
                Assert.assertTrue("REF: directives will be ignored for $REF_EMPTY test: $refs", refs.isEmpty())
                referenceToString = "<empty>"
            }
            else {
                assertTrue(refs.size() == 1, "Must be a single ref: $refs.\nUse $MULTIRESOLVE if you need multiple refs\nUse $REF_EMPTY for an unresolved reference")
                referenceToString = refs.get(0)
                Assert.assertNotNull("Test data wasn't found, use \"// REF: \" directive", referenceToString)
            }

            return ExpectedResolveData(shouldBeUnresolved, referenceToString)
        }

        // purpose of this helper is to deal with the case when navigation element is a file
        // see ReferenceResolveInJavaTestGenerated.testPackageFacade()
        private fun getExpectedReferences(text: String, index: Int): List<String> {
            val prefix = if (index > 0) "// REF$index:" else "// REF:"
            val prefixes = InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, prefix)
            return prefixes.map {
                val replaced = it.replace("<test dir>", PluginTestCaseBase.getTestDataPathBase())
                PathUtil.toSystemDependentName(replaced).replace("//", "/") //happens on Unix
            }
        }

        public fun checkReferenceResolve(expectedResolveData: ExpectedResolveData, offset: Int, psiReference: PsiReference?) {
            if (psiReference != null) {
                val resolvedTo = psiReference.resolve()
                if (resolvedTo != null) {
                    val resolvedToElementStr = ReferenceUtils.renderAsGotoImplementation(resolvedTo)
                    assertEquals(expectedResolveData.referenceString, resolvedToElementStr, "Found reference to '$resolvedToElementStr', but '${expectedResolveData.referenceString}' was expected")
                }
                else {
                    if (!expectedResolveData.shouldBeUnresolved()) {
                        assertNull(expectedResolveData.referenceString, "Element $psiReference wasn't resolved to anything, but ${expectedResolveData.referenceString} was expected")
                    }
                }
            }
            else {
                assertNull(expectedResolveData.referenceString, "No reference found at offset: $offset, but one resolved to ${expectedResolveData.referenceString} was expected")
            }
        }
    }
}
