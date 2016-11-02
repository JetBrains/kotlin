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
}