/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class LambdaReturnValueHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        myFixture.configureByText("A.kt", text)
        myFixture.testInlays()
    }

    fun testSimple() {
        check(
            """val x = run {
                println("foo")
                <hint text="^run" />1
             }
             """
        )
    }

    fun testQualified() {
        check(
            """val x = run {
                var s = "abc"
                <hint text="^run" />s.length
             }
             """
        )
    }

    fun testIf() {
        check(
            """val x = run {
                if (true) {
                    <hint text="^run" />1
                } else {
                    <hint text="^run" />0
                }
             }
             """
        )
    }

    fun testWhen() {
        check(
            """val x = run {
                when (true) {
                     true -> <hint text="^run" />1
                     false -><hint text="^run" />0
                }
             }
             """
        )
    }

    fun testNoHintForSingleExpression() {
        check(
            """val x = run {
                1
             }
             """
        )
    }

    fun testLabel() {
        check(
            """val x = run foo@{
                println("foo")
                <hint text="^foo" />1
             }
             """
        )
    }

    fun testNested() {
        check(
            """val x = run hello@{
                if (true) {
                }

                <hint text="^hello" />run { // Two hints here
                    when (true) {
                        true -> <hint text="^run" />1
                        false -> <hint text="^run" />0
                    }
                }
            }"""
        )
    }
}
