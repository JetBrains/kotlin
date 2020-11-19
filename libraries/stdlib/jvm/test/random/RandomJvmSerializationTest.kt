/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package random

import test.io.serializeAndDeserialize
import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.random.asKotlinRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RandomJvmSerializationTest {
    @Test
    fun default() {
        val instance = Random
        val deserialized = serializeAndDeserialize(instance)
        assertSame(instance, deserialized)
    }

    @Test
    fun xorwow() {
        val instance = Random(0)
        // Discard some values
        repeat(1000) { instance.nextInt() }
        val deserialized = serializeAndDeserialize(instance)
        assertEquals(instance.nextInt(), deserialized.nextInt())
    }

    @Test
    fun kotlinRandomWrappedAsJava() {
        val java = Random(0).asJavaRandom()
        val deserialized = serializeAndDeserialize(java)
        assertEquals(java.nextInt(), deserialized.nextInt())
    }

    @Test
    fun javaRandomWrappedAsKotlin() {
        val kotlin = java.util.Random(0).asKotlinRandom()
        val deserialized = serializeAndDeserialize(kotlin)
        assertEquals(kotlin.nextInt(), deserialized.nextInt())
    }
}
