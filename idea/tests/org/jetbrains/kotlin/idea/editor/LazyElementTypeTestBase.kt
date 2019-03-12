/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil.*
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.junit.Assert

abstract class LazyElementTypeTestBase<T>(private val lazyElementClass: Class<T>) :
    KotlinLightCodeInsightFixtureTestCaseBase() where T : PsiElement {

    protected fun reparse(text: String, char: Char): Unit = doTest(text, char, true)
    protected fun noReparse(text: String, char: Char): Unit = doTest(text, char, false)

    fun doTest(text: String, char: Char, reparse: Boolean) {
        val file = myFixture.configureByText("a.kt", text.trimMargin())

        val expressionBefore = PsiTreeUtil.findChildOfType(file, lazyElementClass)

        performTypingAction(myFixture.editor, char)
        PsiDocumentManager.getInstance(LightPlatformTestCase.getProject()).commitDocument(myFixture.getDocument(file))

        val expressionAfter = PsiTreeUtil.findChildOfType(file, lazyElementClass)

        val actualReparse = expressionAfter != expressionBefore

        Assert.assertEquals("Lazy element behaviour was unexpected", reparse, actualReparse)
    }

    protected fun inIf(then: String) = "val t: Unit = if (true) $then else {}"

    override fun getProjectDescriptor() = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
}