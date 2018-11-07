/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiClassType
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.test.env.findUElementByTextFromPsi

class KotlinUastResolveApiTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testResolveStringFromUast() {
        val file = myFixture.addFileToProject(
            "s.kt", """fun foo(){
                val s = "abc"
                s.toUpperCase()
                }
            ""${'"'}"""
        )

        val refs = file.findUElementByTextFromPsi<UQualifiedReferenceExpression>("s.toUpperCase()")
        TestCase.assertNotNull((refs.receiver.getExpressionType() as PsiClassType).resolve())
    }

}
