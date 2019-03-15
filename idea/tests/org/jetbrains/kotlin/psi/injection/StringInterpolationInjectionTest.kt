/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.injection

import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.injection.Injectable
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.intellij.plugins.intelliLang.inject.InjectLanguageAction
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class StringInterpolationInjectionTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testInterpolationSimpleName() = doTest(
        """
                        fun foo(){
                            val body = "Hello!"
                            "<html>${"$"}body</html><caret>"
                        }
                    """,
        "<html>Hello!</html>"
    )

    fun testInterpolationBlock() = doTest(
        """
            const val a = "Hello "

            fun foo(){
                val body = "World!"
                "<html>${"$"}{a + body}</html><caret>"
            }
                    """,
        "<html>Hello World!</html>"
    )

    private fun doTest(text: String, expectedText: String) {
        myFixture.configureByText("Injected.kt", text)

        InjectLanguageAction.invokeImpl(
            project,
            myFixture.editor,
            myFixture.file,
            Injectable.fromLanguage(HTMLLanguage.INSTANCE)
        )

        val injectedElement = injectedElement ?: kotlin.test.fail("no injection")
        TestCase.assertEquals(HTMLLanguage.INSTANCE, injectedElement.language)
        val containingFile = injectedElement.containingFile
        TestCase.assertEquals(expectedText, containingFile.text)
        TestCase.assertFalse(
            "Shouldn't be FRANKENSTEIN",
            java.lang.Boolean.TRUE == containingFile.getUserData(InjectedLanguageManager.FRANKENSTEIN_INJECTION)
        )
    }

    private val injectedLanguageManager: InjectedLanguageManager
        get() = InjectedLanguageManager.getInstance(project)

    private val injectedElement: PsiElement?
        get() = injectedLanguageManager.findInjectedElementAt(topLevelFile, topLevelCaretPosition)

    private val topLevelFile: PsiFile get() = file.let { injectedLanguageManager.getTopLevelFile(it) }

    private val topLevelCaretPosition get() = editor.caretModel.offset

}