/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

    fun testRainbowNestedVal() {
        checkRainbow(
            """
             fun some() {
                 run {
                     val <rainbow color='ff000001'>name</rainbow> = 1
                     <rainbow color='ff000001'>name</rainbow>
                     run {
                         val <rainbow color='ff000004'>name</rainbow> = 2
                         <rainbow color='ff000004'>name</rainbow>
                     }
                 }
             }
            """
        )
    }

    fun testAssignmentIt() {
        checkRainbow("""
            val f : (Int) -> Unit = {
                val <rainbow color='ff000004'>t</rainbow> = <rainbow color='ff000002'>it</rainbow>
                <rainbow color='ff000002'>it</rainbow>
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

    fun testInitBlock() {
        checkRainbow("""
            class Some {
                init {
                    val <rainbow color='ff000004'>x</rainbow> = 128
                    println(<rainbow color='ff000004'>x</rainbow>)

                    run {
                        println(<rainbow color='ff000004'>x</rainbow>)
                    }
                    fun some() {
                        val <rainbow color='ff000003'>b</rainbow> = 299
                        println(<rainbow color='ff000003'>b</rainbow> + <rainbow color='ff000004'>x</rainbow>)
                    }
                }
            }""")
    }

    private fun checkRainbow(code: String) {
        myFixture.testRainbow("rainbow.kt", code, true, true)
    }
}
