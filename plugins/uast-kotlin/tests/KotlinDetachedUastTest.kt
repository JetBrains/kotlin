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

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class KotlinDetachedUastTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE


    fun testStringAnnotationWithEmpty() {

        val psiFile = myFixture.configureByText("AnnotatedClass.kt", """
            class AnnotatedClass {
                    @JvmName(name = "")
                    fun bar(param: String) = null
            }
        """.trimIndent())

        fun psiElement(file: PsiFile): Any? = (file.findElementAt(file.text.indexOf("\"\"")) ?: error("not found"))
                .getParentOfType<PsiLanguageInjectionHost>(false).orFail("host")
                .toUElement().orFail("uelement").getParentOfType<UClass>(false)
                .orFail("UClass").psi.orFail("psi").also {
        }

        TestCase.assertNotNull("base", psiElement(psiFile))
        val copied = psiFile.copied()
        TestCase.assertNull("vf for $copied", copied.virtualFile)
        TestCase.assertNotNull("copy", psiElement(copied))
    }
}

fun <T> T?.orFail(msg: String): T = this ?: error(msg)