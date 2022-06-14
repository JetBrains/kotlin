/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TypeTest {

    class Box<T>

    class Outer {
        inner class Inner
    }

    open class Recursive<T : Recursive<T>>

    class AnyRecursive : Recursive<AnyRecursive>()

    @Test
    fun `test - sample 0`() {
        assertEquals(
            "kotlin.collections.List<kotlin.Int>", extrasTypeOf<List<Int>>().signature
        )
    }

    @Test
    fun `test - sample 1`() {
        assertEquals(
            "kotlin.collections.List<*>", extrasTypeOf<List<*>>().signature
        )
    }

    @Test
    fun `test - sample 2`() {
        assertEquals(
            "kotlin.String", extrasTypeOf<String>().signature
        )
    }

    @Test
    fun `test - sample 3`() {
        assertEquals(
            "kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Pair<kotlin.Int, kotlin.String>>>",
            extrasTypeOf<Map<String, List<Pair<Int, String>>>>().signature
        )
    }


    @Test
    fun `test - sample 5 - inner class`() {
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.TypeTest.Outer.Inner",
            extrasTypeOf<Outer.Inner>().signature
        )
    }

    @Test
    fun `test - sample 6 - recursive types`() {
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.TypeTest.Recursive<*>",
            extrasTypeOf<Recursive<*>>().signature
        )

        assertEquals(
            "org.jetbrains.kotlin.tooling.core.TypeTest.AnyRecursive",
            extrasTypeOf<AnyRecursive>().signature
        )

        assertEquals(
            "org.jetbrains.kotlin.tooling.core.TypeTest" +
                    ".Recursive<org.jetbrains.kotlin.tooling.core.TypeTest.AnyRecursive>",
            extrasTypeOf<Recursive<AnyRecursive>>().signature
        )
    }

    @Test
    fun `test sample 7 - nullable`() {
        assertEquals("kotlin.collections.List<*>?", extrasTypeOf<List<*>?>().signature)
        assertEquals("kotlin.collections.List<kotlin.Int?>", extrasTypeOf<List<Int?>>().signature)
    }

    @Test
    fun `test sample 8 - variance`() {
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.TypeTest.Box<out kotlin.Int>",
            extrasTypeOf<Box<out Int>>().signature
        )
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.TypeTest.Box<in kotlin.Int>",
            extrasTypeOf<Box<in Int>>().signature
        )
    }

    @Suppress("RemoveExplicitTypeArguments", "TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
    @Test
    fun `test - equals`() {
        assertEquals(extrasTypeOf<List<Int>>(), extrasTypeOf<List<Int>>())
        assertNotEquals(extrasTypeOf<List<Int?>>(), extrasTypeOf<List<Int>>())
    }
}
