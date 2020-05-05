/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.codeInsight.hints.HintType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
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

    private fun checkLocalVariable(text: String) = check(text.trimIndent(), HintType.LOCAL_VARIABLE_HINT)
    private fun checkPropertyHint(text: String) = check(text.trimIndent(), HintType.PROPERTY_HINT)
    private fun checkFunctionHint(text: String) = check(text, HintType.FUNCTION_HINT)
    private fun checkParameterTypeHint(text: String) = check(text, HintType.PARAMETER_TYPE_HINT)

    fun testLocalVariableType() {
        checkLocalVariable("""fun foo() { val a<hint text=": List<String>" /> = listOf("a") }""")
    }

    fun testDestructuringType() {
        checkLocalVariable("""fun foo() { val (i<hint text=": Int" />, s<hint text=": String" />) = 1 to "" }""")
    }

    fun testQualifiedReferences() {
        checkLocalVariable(
            """
            package p
            class A {
                class B {
                    class C {
                        class D
                    }
                }
                inner class E
                enum class F { enumCase }
            }
            fun foo() {
                val v1 = A.B.C.D()
                val v2 = p.A.B.C.D()
                val v3<hint text=": A.E"/> = A().E()
                val v4 = p.A.F.enumCase
                val v5 = A.F.enumCase
                val v6 = p.A()
            }
        """
        )
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
            """
        )
    }

    fun testEnumEntry() {
        checkPropertyHint(
            """
            enum class E { ENTRY }
            val test = E.ENTRY
            """
        )
    }

    fun testEnumEntryLikeProperty() {
        checkPropertyHint(
            """
            enum class E {
                ENTRY;
                companion object {
                    val test: E = ENTRY
                }
            }

            val test<hint text=": E"/> = E.test
            """
        )
    }

    fun testEnumEntryLikeFunction() {
        checkPropertyHint(
            """
            enum class E { ENTRY;
                companion object {
                    fun test(): E = ENTRY
                }
            }

            val test<hint text=": E"/> = E.test()
            """
        )
    }

    fun testImportedEnumEntry() {
        checkPropertyHint(
            """
            import E.ENTRY
            enum class E { ENTRY }
            val test<hint text=": E"/> = ENTRY
            """
        )
    }

    fun testEnumEntryCompanion() {
        checkPropertyHint(
            """
            enum class E {
                ENTRY;
                companion object {}
            }
            val test<hint text=": E"/> = E.Companion
            """
        )
    }

    fun testEnumEntryQualified() {
        checkPropertyHint(
            """
            package a
            enum class E { ENTRY }
            val test = a.E.ENTRY
            """
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
            """
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
            """
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
            """
        )
    }

    fun testUnitLocalVariable() {
        checkLocalVariable(
            """
            fun foo() {
                val x =

                println("Foo")
            }
            """
        )
    }

    fun testUnitLocalVariable2() {
        checkLocalVariable(
            """
            fun foo() {
                val x<hint text=": Unit"/> =
                    println("Foo")
            }
            """
        )
    }

    fun testUnitLocalVariable3() {
        checkLocalVariable(
            """
            fun foo() {
                val x<hint text=": Unit"/> = println("Foo")
            }
            """
        )
    }

    fun testParameterType() {
        checkParameterTypeHint(
            """
            fun <T> T.wrap(lambda: (T) -> T) {}
            fun foo() {
                12.wrap { elem<hint text=": Int"/> ->
                    elem
                }
            }
            """
        )
    }
}
