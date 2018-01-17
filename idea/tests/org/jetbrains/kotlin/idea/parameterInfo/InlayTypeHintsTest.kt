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

    fun testLocalVariableType() {
        HintType.LOCAL_VARIABLE_HINT.option.set(true)
        check("""fun foo() { val a<hint text=": List<String>" /> = listOf("a") }""")
    }

    fun testDestructuringType() {
        HintType.LOCAL_VARIABLE_HINT.option.set(true)
        check("""fun foo() { val (i<hint text=": Int" />, s<hint text=": String" />) = 1 to "" }""")
    }

    fun testPropertyType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val a<hint text=": List<String>" /> = listOf("a")""")
    }

    fun testConstInitializerType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val a = 1""")
    }

    fun testUnaryConstInitializerType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val a = -1; val b = +1""")
    }

    fun testConstructorWithoutTypeParametersType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val a = Any()""")
    }

    fun testConstructorWithExplicitTypeParametersType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""class Bar<T>; val a = Bar<String>()""")
    }

    fun testConstructorWithoutExplicitTypeParametersType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""class Bar<T>(val t: T); val a<hint text=": Bar<String>" /> = Bar(<hint text="t:" />"")""")
    }

    fun testLoopParameter() {
        HintType.LOCAL_VARIABLE_HINT.option.set(true)
        check("""fun foo() { for (x<hint text=": String" /> in listOf("a")) { } }""")
    }

    fun testLoopParameterWithExplicitType() {
        HintType.LOCAL_VARIABLE_HINT.option.set(true)
        check("""fun foo() { for (x: String in listOf("a")) { } }""")
    }

    fun testErrorType() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val x = arrayListOf<>()""")
    }

    fun testExpandedTypeAlias() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val x<hint text=": ArrayList<Int>" /> = arrayListOf(1)""")
    }

    fun testAnonymousObject() {
        HintType.FUNCTION_HINT.option.set(true)
        check("""val o = object : Iterable<Int> {
                  override fun iterator()<hint text=": Iterator<Int>" /> = object : Iterator<Int> {
                       override fun next()<hint text=": Int" /> = 1
                       override fun hasNext()<hint text=": Boolean" /> = true
                  }
              }""")
    }

    fun testAnonymousObjectNoBaseType() {
        HintType.LOCAL_VARIABLE_HINT.option.set(true)
        check("""fun foo() {
                val o = object {
                    val x: Int = 0
                }
              }""")
    }

    fun testDestructuring() {
        HintType.LOCAL_VARIABLE_HINT.option.set(true)
        check("""fun main(args: Array<String>) {
                val (a: String, b: String, c: String) = x()
            }

            fun x() :Triple<String, String,String> {
                return Triple(<hint text="first:" />"A", <hint text="second:" />"B", <hint text="third:" />"C")
            }""")
    }

    fun testSAMConstructor() {
        HintType.PROPERTY_HINT.option.set(true)
        check("""val x = Runnable { }""")
    }

    fun testNestedClassImports() {
        HintType.PROPERTY_HINT.option.set(true)
        check(
            """import kotlin.collections.Map.Entry
                    val entries<hint text=": Set<Entry<Int, String>>" /> = mapOf(1 to "1").entries"""
        )
    }

    fun testNestedClassWithoutImport() {
        HintType.PROPERTY_HINT.option.set(true)
        check(
            """val entries<hint text=": Set<Map.Entry<Int, String>>" /> = mapOf(1 to "1").entries"""
        )
    }
}
