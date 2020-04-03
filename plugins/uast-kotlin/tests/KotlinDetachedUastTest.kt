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
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.findFunctionByName
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.uast.*
import org.jetbrains.uast.test.env.kotlin.findUElementByTextFromPsi
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
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

    fun testDetachedResolve() {
        val psiFile = myFixture.configureByText(
            "AnnotatedClass.kt", """
            class AnnotatedClass {
                    @JvmName(name = "")
                    fun bar(param: String) { unknownFunc(param) }
            }
        """.trimIndent()
        ) as KtFile

        val detachedCall = psiFile.findDescendantOfType<KtCallExpression>()!!.copied()
        val uCallExpression = detachedCall.toUElementOfType<UCallExpression>()!!
        // at least it should not throw exceptions
        TestCase.assertNull(uCallExpression.methodName)
    }

    fun testCapturedTypeInExtensionReceiverOfCall() {
        val psiFile = myFixture.configureByText(
            "foo.kt", """
            class Foo<T>

            fun <K> K.extensionFunc() {}

            fun test(f: Foo<*>) {
                f.extensionFunc()
            }
        """.trimIndent()
        ) as KtFile

        val extensionFunctionCall = psiFile.findDescendantOfType<KtCallExpression>()!!
        val uCallExpression = extensionFunctionCall.toUElementOfType<UCallExpression>()!!

        TestCase.assertNotNull(uCallExpression.receiverType)
        TestCase.assertNotNull(uCallExpression.methodName)
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

    fun testLiteralInClassInitializerFromFactory() {

        val detachedClass = KtPsiFactory(project).createClass("""
        class MyAnnotation(){
            init {
                "default"
            }
        }
        """)

        val literalInside = detachedClass.findUElementByTextFromPsi<UElement>("default")
        generateSequence(literalInside, { it.uastParent }).count().let {
            TestCase.assertTrue("it should have some parents $it actually", it > 1)
        }

    }

    fun testAnonymousInnerClassWithIDELightClasses() {

        val detachedClass = myFixture.configureByText(
            "MyClass.kt", """
            class MyClass() {
              private val obj = object : MyClass() {}
            }
        """
        )

        val anonymousClass = detachedClass.findUElementByTextFromPsi<UObjectLiteralExpression>("object : MyClass() {}")
            .let { uObjectLiteralExpression -> uObjectLiteralExpression.declaration }
        TestCase.assertEquals(
            "UClass (name = null), UObjectLiteralExpression, UField (name = obj), UClass (name = MyClass), UFile (package = )",
            generateSequence<UElement>(anonymousClass, { it.uastParent }).joinToString { it.asLogString() })

    }


    fun testDontConvertDetachedFunctions() {
        val ktFile = myFixture.configureByText(
            "MyClass.kt", """
            class MyClass() {
              fun foo1() = 42
            }
        """
        ) as KtFile
        val ktClass = ktFile.declarations.filterIsInstance<KtClass>().single()//.copied()
        val ktFunctionDetached = ktClass.findFunctionByName("foo1")!!
        runWriteAction { ktClass.delete() }
        TestCase.assertNull(ktFunctionDetached.toUElementOfType<UMethod>())
    }

    fun testRenameHandlers() {
        myFixture.configureByText(
            "JavaClass.java", """
            class JavaClass {
              void foo(){
                 new MyClass().getBar();
              }
            }
        """
        )

        myFixture.configureByText(
            "MyClass.kt", """
            class MyClass() {
              val b<caret>ar = 42
            }
        """
        )

        val element = myFixture.elementAtCaret

        val substitution =
            RenamePsiElementProcessor.forElement(element).substituteElementToRename(element, editor).orFail("no element")
        val linkedMapOf = linkedMapOf<PsiElement, String>()
        RenameProcessor(project, substitution, "newName", false, false)
            .prepareRenaming(element, "newName", linkedMapOf)

        UsefulTestCase.assertTrue(linkedMapOf.any())

        for ((k, _) in linkedMapOf) {
            TestCase.assertEquals(element, k.toUElement()?.sourcePsi)
        }
    }

}


fun <T> T?.orFail(msg: String): T = this ?: error(msg)