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
}
