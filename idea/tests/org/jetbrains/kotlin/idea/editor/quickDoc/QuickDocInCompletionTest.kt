/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.quickDoc

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinQuickDocumentationProvider
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.junit.Assert

class QuickDocInCompletionTest: KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/inCompletion/"
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testSimple() {
        val element = getElementFromLookup()
        Assert.assertTrue(element is KtClass)
    }

    fun testProp() {
        val element = getElementFromLookup()
        Assert.assertTrue(element is KtProperty)
    }

    private fun getElementFromLookup(): PsiElement? {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val lookupElements = myFixture.completeBasic()
        val lookupObject = lookupElements.first().`object`
        return KotlinQuickDocumentationProvider().getDocumentationElementForLookupItem(
                myFixture.psiManager, lookupObject, null)
    }
}