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

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class KotlinRainbowHighlighterTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testRainbowSimple() {
        checkRainbow("""
             fun main(<rainbow color='ff000003'>args</rainbow>: Array<String>) {
                  val <rainbow color='ff000004'>local1</rainbow> = ""
                  println(<rainbow color='ff000004'>local1</rainbow> + <rainbow color='ff000003'>args</rainbow>)
             }
        """)
    }

    fun testRainbowNestedIt() {
        checkRainbow("""
             fun main(<rainbow color='ff000003'>args</rainbow>: Array<String>) {
                  listOf("abc", "def").filter { <rainbow color='ff000002'>it</rainbow>.any { <rainbow color='ff000003'>it</rainbow> == 'a' } }
             }
        """)
    }

    fun testRainbowNestedAnonymousFunction() {
        checkRainbow("""
            fun main() {
                listOf("abc", "def").filter(fun(<rainbow color='ff000002'>it</rainbow>): Boolean {
                    return <rainbow color='ff000002'>it</rainbow>.any(fun(<rainbow color='ff000003'>it</rainbow>): Boolean {
                        return <rainbow color='ff000003'>it</rainbow> == 'a'
                    })
                })
            }
        """)
    }

    fun testKDoc() {
        checkRainbow("""
             /**
              * @param <rainbow color='ff000003'>args</rainbow> foo
              */
             fun main(<rainbow color='ff000003'>args</rainbow>: Array<String>) {
             }
        """)
    }

    fun testQualified() {
        checkRainbow("""
             data class Foo(val bar: String)

             fun main(<rainbow color='ff000004'>foo</rainbow>: Foo) {
                  println(<rainbow color='ff000004'>foo</rainbow>.bar)
                  System.out.println(<rainbow color='ff000004'>foo</rainbow>)
             }
        """)
    }

    fun testDestructuring() {
        checkRainbow("""
            fun foo() {
                val (<rainbow color='ff000003'>a</rainbow>, <rainbow color='ff000004'>b</rainbow>) = 1 to 2

                println(<rainbow color='ff000003'>a</rainbow>)
                println(<rainbow color='ff000004'>b</rainbow>)
            }
        """)
    }

    private fun checkRainbow(code: String) {
        myFixture.testRainbow("rainbow.kt", code, true, true)
    }
}
