/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.lightClasses

import com.intellij.psi.*
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtElement
import java.lang.System.identityHashCode as idh


class LightElementsEqualsTest : KotlinLightCodeInsightFixtureTestCase() {

    private val SAMPLE_SOURCE = """
                    class A(i: Int) {
                        val b: String

                        @Synchronized
                        fun foo(param: String){}
                    }
                """.trimIndent()

    fun testEqualityOfTwiceConvertedLightElements() {
        val psiFile = myFixture.configureByText("a.kt", SAMPLE_SOURCE)
        psiFile.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is KtElement) {
                    val firstConversion = element.toLightElements()
                    val secondConversion = element.toLightElements()
                    TestCase.assertEquals(
                            "LightElements from ${element.text} should be equal if they retrieved twice",
                            firstConversion, secondConversion
                    )
                    for ((e1, e2) in firstConversion zip secondConversion) {
                        TestCase.assertEquals(
                                "LightElements '$e1'(${e1.javaClass}) and '$e2'(${e2.javaClass}) from `${element.text}` should have equal hashcode as long as they are equal",
                                e1.hashCode(), e2.hashCode()
                        )
                    }

                }
                element.acceptChildren(this)
            }
        })

    }

    fun testToLightMethodsConvertedEquality() {
        myFixture.configureByText("a.kt", SAMPLE_SOURCE)

        val theAPsiClass = myFixture.javaFacade.findClass("A")!!

        val directlyFlattenMethods = theAPsiClass.methods.asSequence().flatMap { psiElementsFlatten(it) }
        val contentOfToLightMethods = (theAPsiClass as KtLightClass).kotlinOrigin!!.declarations.asSequence()
                .flatMap { it.toLightMethods().asSequence() }
                .flatMap { psiElementsFlatten(it) }
        for ((e1, e2) in directlyFlattenMethods zip contentOfToLightMethods) {
            TestCase.assertEquals(e1, e2)
            TestCase.assertEquals(e1.hashCode(), e2.hashCode())
        }
    }

    private fun psiElementsFlatten(psiElement: PsiElement): Sequence<PsiElement> {
        return when (psiElement) {
            is PsiClass -> sequenceOf(psiElement, psiElement.modifierList).filterNotNull() +
                           psiElement.methods.asSequence().flatMap { psiElementsFlatten(it) } +
                           psiElement.fields.asSequence().flatMap { psiElementsFlatten(it) }
            is PsiMethod -> sequenceOf(psiElement, psiElement.parameterList) + psiElementsFlatten(psiElement.parameterList)
            is PsiParameterList -> sequenceOf(psiElement) +
                                   psiElement.parameters.asSequence().flatMap { psiElementsFlatten(it) }
            is PsiModifierList -> sequenceOf(psiElement) +
                                  psiElement.annotations.asSequence().flatMap { psiElementsFlatten(it) }
            is PsiAnnotation -> sequenceOf(psiElement, psiElement.parameterList) + psiElementsFlatten(psiElement.parameterList)

            else -> sequenceOf(psiElement)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

}