/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReifiedTypeSignatureTest {

    class Box<T>

    class Outer {
        inner class Inner
    }

    open class Recursive<T : Recursive<T>>

    class AnyRecursive : Recursive<AnyRecursive>()

    @Test
    fun `test - sample 0`() {
        assertEquals(
            "kotlin.collections.List<kotlin.Int>", reifiedTypeSignatureOf<List<Int>>().signature
        )
    }

    @Test
    fun `test - sample 1`() {
        assertEquals(
            "kotlin.collections.List<*>", reifiedTypeSignatureOf<List<*>>().signature
        )
    }

    @Test
    fun `test - sample 2`() {
        assertEquals(
            "kotlin.String", reifiedTypeSignatureOf<String>().signature
        )
    }

    @Test
    fun `test - sample 3`() {
        assertEquals(
            "kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Pair<kotlin.Int, kotlin.String>>>",
            reifiedTypeSignatureOf<Map<String, List<Pair<Int, String>>>>().signature
        )
    }


    @Test
    fun `test - sample 5 - inner class`() {
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest.Outer.Inner",
            reifiedTypeSignatureOf<Outer.Inner>().signature
        )
    }

    @Test
    fun `test - sample 6 - recursive types`() {
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest.Recursive<*>",
            reifiedTypeSignatureOf<Recursive<*>>().signature
        )

        assertEquals(
            "org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest.AnyRecursive",
            reifiedTypeSignatureOf<AnyRecursive>().signature
        )

        assertEquals(
            "org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest" +
                    ".Recursive<org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest.AnyRecursive>",
            reifiedTypeSignatureOf<Recursive<AnyRecursive>>().signature
        )
    }

    @Test
    fun `test sample 7 - nullable`() {
        assertEquals("kotlin.collections.List<*>?", reifiedTypeSignatureOf<List<*>?>().signature)
        assertEquals("kotlin.collections.List<kotlin.Int?>", reifiedTypeSignatureOf<List<Int?>>().signature)
    }

    @Test
    fun `test sample 8 - variance`() {
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest.Box<out kotlin.Int>",
            reifiedTypeSignatureOf<Box<out Int>>().signature
        )
        assertEquals(
            "org.jetbrains.kotlin.tooling.core.ReifiedTypeSignatureTest.Box<in kotlin.Int>",
            reifiedTypeSignatureOf<Box<in Int>>().signature
        )
    }

    @Suppress("RemoveExplicitTypeArguments", "TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
    @Test
    fun `test - equals`() {
        assertEquals(reifiedTypeSignatureOf<List<Int>>(), reifiedTypeSignatureOf<List<Int>>())
        assertNotEquals(reifiedTypeSignatureOf<List<Int?>>(), reifiedTypeSignatureOf<List<Int>>())
    }
}
