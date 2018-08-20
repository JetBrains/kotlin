/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class InlayTypeHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        myFixture.configureByText("A.kt", text)
        myFixture.testInlays()
    }

    fun check(text: String, hintType: HintType) {
        hintType.option.set(true)
        check(text)
    }

    private fun checkLocalVariable(text: String) = check(text, HintType.LOCAL_VARIABLE_HINT)
    private fun checkPropertyHint(text: String) = check(text, HintType.PROPERTY_HINT)
    private fun checkFunctionHint(text: String) = check(text, HintType.FUNCTION_HINT)

    fun testLocalVariableType() {
        checkLocalVariable("""fun foo() { val a<hint text=": List<String>" /> = listOf("a") }""")
    }

    fun testDestructuringType() {
        checkLocalVariable("""fun foo() { val (i<hint text=": Int" />, s<hint text=": String" />) = 1 to "" }""")
    }

    fun testPropertyType() {
        checkPropertyHint("""val a<hint text=": List<String>" /> = listOf("a")""")
    }

    fun testConstInitializerType() {
        checkPropertyHint("""val a = 1""")
    }

    fun testUnaryConstInitializerType() {
        checkPropertyHint("""val a = -1; val b = +1""")
    }

    fun testConstructorWithoutTypeParametersType() {
        checkPropertyHint("""val a = Any()""")
    }

    fun testConstructorWithExplicitTypeParametersType() {
        checkPropertyHint("""class Bar<T>; val a = Bar<String>()""")
    }

    fun testConstructorWithoutExplicitTypeParametersType() {
        checkPropertyHint("""class Bar<T>(val t: T); val a<hint text=": Bar<String>" /> = Bar(<hint text="t:" />"")""")
    }

    fun testLoopParameter() {
        checkLocalVariable("""fun foo() { for (x<hint text=": String" /> in listOf("a")) { } }""")
    }

    fun testLoopParameterWithExplicitType() {
        checkLocalVariable("""fun foo() { for (x: String in listOf("a")) { } }""")
    }

    fun testErrorType() {
        checkPropertyHint("""val x = arrayListOf<>()""")
    }

    fun testExpandedTypeAlias() {
        checkPropertyHint("""val x<hint text=": ArrayList<Int>" /> = arrayListOf(1)""")
    }

    fun testAnonymousObject() {
        checkFunctionHint(
            """
            val o = object : Iterable<Int> {
                override fun iterator()<hint text=": Iterator<Int>" /> = object : Iterator<Int> {
                    override fun next()<hint text=": Int" /> = 1
                    override fun hasNext()<hint text=": Boolean" /> = true
                }
            }
            """.trimIndent()
        )
    }

    fun testAnonymousObjectNoBaseType() {
        checkLocalVariable(
            """
            fun foo() {
                val o = object {
                    val x: Int = 0
                }
            }
            """.trimIndent()
        )
    }

    fun testDestructuring() {
        checkLocalVariable(
            """
            fun main(args: Array<String>) {
                val (a: String, b: String, c: String) = x()
            }

            fun x() :Triple<String, String,String> {
                return Triple(<hint text="first:" />"A", <hint text="second:" />"B", <hint text="third:" />"C")
            }
            """.trimIndent()
        )
    }

    fun testSAMConstructor() {
        checkPropertyHint("""val x = Runnable { }""")
    }

    fun testNestedClassImports() {
        checkPropertyHint(
            """import kotlin.collections.Map.Entry
                    val entries<hint text=": Set<Entry<Int, String>>" /> = mapOf(1 to "1").entries"""
        )
    }

    fun testNestedClassWithoutImport() {
        checkPropertyHint(
            """val entries<hint text=": Set<Map.Entry<Int, String>>" /> = mapOf(1 to "1").entries"""
        )
    }

    fun testTypeInCompanion() {
        checkPropertyHint(
            """
            class A {
                companion object {
                    class InA
                    fun provideInA() = InA()
                }
            }
            val inA<hint text=": A.InA"/> = A.provideInA()
            """.trimIndent()
        )
    }

    fun testTypeInNonDefaultCompanion() {
        checkPropertyHint(
            """
            class A {
                companion object N {
                    class InA
                    fun provideInA() = InA()
                }
            }
            val inA<hint text=": A.N.InA"/> = A.provideInA()
            """.trimIndent()
        )
    }
}
