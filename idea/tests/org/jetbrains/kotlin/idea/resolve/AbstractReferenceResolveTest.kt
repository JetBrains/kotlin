/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.google.common.collect.Lists
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.junit.Assert
import kotlin.test.assertTrue

abstract class AbstractReferenceResolveTest : KotlinLightPlatformCodeInsightFixtureTestCase() {
    class ExpectedResolveData(private val shouldBeUnresolved: Boolean?, val referenceString: String) {

        fun shouldBeUnresolved(): Boolean {
            return shouldBeUnresolved!!
        }
    }

    protected open fun doTest(path: String) {
        assert(path.endsWith(".kt")) { path }
        myFixture.configureWithExtraFile(path, ".Data")
        performChecks()
    }

    protected fun performChecks() {
        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, MULTIRESOLVE)) {
            doMultiResolveTest()
        }
        else {
            doSingleResolveTest()
        }
    }

    protected fun doSingleResolveTest() {
        forEachCaret { index, offset ->
            val expectedResolveData = readResolveData(myFixture.file.text, index, refMarkerText)
            val psiReference = wrapReference(myFixture.file.findReferenceAt(offset))
            checkReferenceResolve(expectedResolveData, offset, psiReference) { checkResolvedTo(it) }
        }
    }

    open fun checkResolvedTo(element: PsiElement) {
        // do nothing
    }

    open fun wrapReference(reference: PsiReference?): PsiReference? = reference
    open fun wrapReference(reference: PsiPolyVariantReference): PsiPolyVariantReference = reference

    protected fun doMultiResolveTest() {
        forEachCaret { index, offset ->
            val expectedReferences = getExpectedReferences(myFixture.file.text, index, refMarkerText)

            val psiReference = myFixture.file.findReferenceAt(offset)
            assertTrue(psiReference is PsiPolyVariantReference)
            psiReference as PsiPolyVariantReference

            val results = wrapReference(psiReference).multiResolve(true)

            val actualResolvedTo = Lists.newArrayList<String>()
            for (result in results) {
                actualResolvedTo.add(result.element!!.renderAsGotoImplementation())
            }

            UsefulTestCase.assertOrderedEquals("Not matching for reference #$index", actualResolvedTo.sorted(), expectedReferences.sorted())
        }
    }

    private fun forEachCaret(action: (index: Int, offset: Int) -> Unit) {
        val offsets = myFixture.editor.caretModel.allCarets.map { it.offset }
        val singleCaret = offsets.size == 1
        for ((index, offset) in offsets.withIndex()) {
            action(if (singleCaret) -1 else index + 1, offset)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor? = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    open val refMarkerText: String = "REF"

    companion object {
        val MULTIRESOLVE: String = "MULTIRESOLVE"
        val REF_EMPTY: String = "REF_EMPTY"

        fun readResolveData(fileText: String, index: Int, refMarkerText: String = "REF"): ExpectedResolveData {
            val shouldBeUnresolved = InTextDirectivesUtils.isDirectiveDefined(fileText, REF_EMPTY)
            val refs = getExpectedReferences(fileText, index, refMarkerText)

            val referenceToString: String
            if (shouldBeUnresolved) {
                Assert.assertTrue("REF: directives will be ignored for $REF_EMPTY test: $refs", refs.isEmpty())
                referenceToString = "<empty>"
            }
            else {
                assertTrue(refs.size == 1, "Must be a single ref: $refs.\nUse $MULTIRESOLVE if you need multiple refs\nUse $REF_EMPTY for an unresolved reference")
                referenceToString = refs.get(0)
                Assert.assertNotNull("Test data wasn't found, use \"// REF: \" directive", referenceToString)
            }

            return ExpectedResolveData(shouldBeUnresolved, referenceToString)
        }

        // purpose of this helper is to deal with the case when navigation element is a file
        // see ReferenceResolveInJavaTestGenerated.testPackageFacade()
        private fun getExpectedReferences(text: String, index: Int, refMarkerText: String): List<String> {
            val prefix = if (index > 0) "// $refMarkerText$index:" else "// $refMarkerText:"
            return InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, prefix)
        }

        fun checkReferenceResolve(expectedResolveData: ExpectedResolveData, offset: Int, psiReference: PsiReference?, checkResolvedTo: (PsiElement) -> Unit = {}) {
            val expectedString = expectedResolveData.referenceString
            if (psiReference != null) {
                val resolvedTo = psiReference.resolve()
                if (resolvedTo != null) {
                    checkResolvedTo(resolvedTo)
                    val resolvedToElementStr = replacePlaceholders(resolvedTo.renderAsGotoImplementation())
                    assertEquals("Found reference to '$resolvedToElementStr', but '$expectedString' was expected", expectedString, resolvedToElementStr)
                }
                else {
                    if (!expectedResolveData.shouldBeUnresolved()) {
                        assertNull("Element $psiReference (${psiReference.element.text}) wasn't resolved to anything, but $expectedString was expected", expectedString)
                    }
                }
            }
            else {
                assertNull("No reference found at offset: $offset, but one resolved to $expectedString was expected", expectedString)
            }
        }

        private fun replacePlaceholders(actualString: String): String {
            val replaced = PathUtil.toSystemIndependentName(actualString)
                    .replace(PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE, "/<test dir>")
                    .replace("//", "/") // additional slashes to fix discrepancy between windows and unix
            if ("!/" in replaced) {
                return replaced.replace(replaced.substringBefore("!/"), "<jar>")
            }
            return replaced
        }
    }
}
