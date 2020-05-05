/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.codeInsight.hints.HintType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LambdaImplicitHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        myFixture.configureByText("A.kt", text)
        myFixture.testInlays()
    }

    fun testHintType() {
        myFixture.checkHintType(
            """
            val x = listOf("").filter { <caret>
            }
            """,
            HintType.LAMBDA_IMPLICIT_PARAMETER_RECEIVER
        )
    }

    fun testSimpleIt() {
        check(
            """
            val x = listOf("").filter {<hint text="it: String" />
                it.startsWith(<hint text="prefix:" />"")
            }"""
        )
    }

    fun testSimpleThis() {
        check(
            """
            val x = buildString {<hint text="this: StringBuilder" />
                append("foo")
            }"""
        )
    }

    fun testSingleLine() {
        check(
            """
            val x = listOf("").filter { it.startsWith(<hint text="prefix:" />"") }
            """
        )
    }
}
