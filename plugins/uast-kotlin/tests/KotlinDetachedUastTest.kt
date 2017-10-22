/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.*
import org.jetbrains.uast.test.env.findUElementByTextFromPsi

class KotlinDetachedUastTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testLiteralInAnnotation() {

        val psiFile = myFixture.configureByText("AnnotatedClass.kt", """
            class AnnotatedClass {
                    @JvmName(name = "")
                    fun bar(param: String) = null
            }
        """.trimIndent())

        fun psiElement(file: PsiFile): PsiElement = file.findElementAt(file.text.indexOf("\"\"")).orFail("literal")
                .getParentOfType<PsiLanguageInjectionHost>(false).orFail("host")
                .toUElement().orFail("uElement").getParentOfType<UClass>(false)
                .orFail("UClass").psi.orFail("psi")

        psiElement(psiFile).let {
            // Otherwise following asserts have no sense
            TestCase.assertTrue("psi element should be light ", it is KtLightElement<*, *>)
        }
        val copied = psiFile.copied()
        TestCase.assertNull("virtual file for copy should be null", copied.virtualFile)
        TestCase.assertNotNull("psi element in copy", psiElement(copied))
        TestCase.assertSame("copy.originalFile should be psiFile", copied.originalFile, psiFile)
        TestCase.assertSame("virtualFiles of element and file itself should be the same",
                            psiElement(copied).containingFile.originalFile.virtualFile,
                            copied.originalFile.virtualFile)
    }

    fun testParameterInAnnotationClassFromFactory() {

        val detachedClass = KtPsiFactory(project).createClass("""
        annotation class MyAnnotation(val myParam: String = "default")
        """)

        detachedClass.findUElementByTextFromPsi<UElement>("default")
                .getParentOfType<UExpression>().let {
            TestCase.assertNotNull("it should return something at least", it)
        }

    }

}

fun <T> T?.orFail(msg: String): T = this ?: error(msg)