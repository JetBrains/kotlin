/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class LambdaImplicitHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        myFixture.configureByText("A.kt", text)
        myFixture.testInlays()
    }

    fun testSimpleIt() {
        check(
            """
            val x = listOf("").filter {<hint text="it: String" />
                it.startsWith("")
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
            val x = listOf("").filter { it.startsWith("") }
            """
        )
    }
}
