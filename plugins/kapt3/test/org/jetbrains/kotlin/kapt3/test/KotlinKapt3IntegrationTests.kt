/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3.test

import org.junit.Test
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

class KotlinKapt3IntegrationTests : AbstractKotlinKapt3IntegrationTest() {
    @Test
    fun testSimple() = test("Simple", "test.MyAnnotation") { set, roundEnv, env ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
        assertEquals(1, annotatedElements.size)
        assertEquals("myMethod", annotatedElements.single().simpleName.toString())
    }

    @Test
    fun testOptions() = test(
            "Simple", "test.MyAnnotation",
            options = mapOf("firstKey" to "firstValue", "secondKey" to "")
    ) { set, roundEnv, env ->
        val options = env.options
        assertEquals("firstValue", options["firstKey"])
        assertTrue("secondKey" in options)
    }

    @Test
    fun testOverloads() = test("Overloads", "test.MyAnnotation") { set, roundEnv, env ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
        assertEquals(1, annotatedElements.size)

        val constructors = annotatedElements
                .first()
                .enclosedElements
                .filter { it.kind == ElementKind.CONSTRUCTOR }
                .map { it as ExecutableElement }
                .sortedBy { it.parameters.size }

        val overloadedMethods = annotatedElements
                .first()
                .enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .filter { it.simpleName.contentEquals("overloadedMethod") }
                .map { it as ExecutableElement }
                .sortedBy { it.parameters.size }

        val staticMethods = annotatedElements
                .first()
                .enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .filter { it.simpleName.contentEquals("staticMethod") }
                .map { it as ExecutableElement }
                .sortedBy { it.parameters.size }

        val testOverloads = fun(elements: List<ExecutableElement>) {
            assertEquals(2, elements.size)
            assertEquals(2, elements[0].parameters.size)
            assertEquals(3, elements[1].parameters.size)
            assertEquals("int", elements[0].parameters[0].asType().toString())
            assertEquals("long", elements[0].parameters[1].asType().toString())
            assertEquals("int", elements[1].parameters[0].asType().toString())
            assertEquals("long", elements[1].parameters[1].asType().toString())
            assertEquals("java.lang.String", elements[1].parameters[2].asType().toString())
            assertEquals("someInt", elements[0].parameters[0].simpleName.toString())
            assertEquals("someLong", elements[0].parameters[1].simpleName.toString())
            assertEquals("someInt", elements[1].parameters[0].simpleName.toString())
            assertEquals("someLong", elements[1].parameters[1].simpleName.toString())
            assertEquals("someString", elements[1].parameters[2].simpleName.toString())
        }

        testOverloads(constructors)
        testOverloads(overloadedMethods)
        testOverloads(staticMethods)
    }
}
