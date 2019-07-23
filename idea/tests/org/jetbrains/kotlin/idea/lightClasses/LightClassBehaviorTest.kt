/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.lightClasses

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LightClassBehaviorTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testIdentifierOffsets() {
        myFixture.configureByText(
            "test.kt", """
            class A {
                fun foo() {}
            }
            """.trimIndent()
        )

        val aClass = myFixture.javaFacade.findClass("A")!!
        val fooMethodName = (aClass.findMethodsByName("foo").single() as PsiMethod).nameIdentifier!!

        val offset = fooMethodName.textOffset
        val range = fooMethodName.textRange

        assert(offset > 0)
        assert(offset == range.startOffset)
    }

    fun testPropertyAccessorOffsets() {
        myFixture.configureByText(
            "test.kt", """
            class A {
                var a: Int
                    get() = 5
                    set(v) {}
            }
            """.trimIndent()
        )

        val aClass = myFixture.javaFacade.findClass("A") as KtLightClass
        val getAMethod = aClass.findMethodsByName("getA").single() as PsiMethod
        val setAMethod = aClass.findMethodsByName("setA").single() as PsiMethod

        val ktClass = aClass.kotlinOrigin!!
        val ktProperty = ktClass.declarations.filterIsInstance<KtProperty>().single()

        assert(getAMethod.textOffset != setAMethod.textOffset)

        assert(getAMethod.textOffset > 0)
        assert(getAMethod.textOffset != ktProperty.textOffset)
        assert(getAMethod.textOffset == getAMethod.textRange.startOffset)

        assert(setAMethod.textOffset > 0)
        assert(setAMethod.textOffset != ktProperty.textOffset)
        assert(setAMethod.textOffset == setAMethod.textRange.startOffset)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }
}