/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package random

import test.io.deserializeFromHex
import test.io.serializeAndDeserialize
import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.random.asKotlinRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RandomSerializationTest {
    @Test
    fun defaultIsSerializable() {
        val instance = Random
        assertSame(instance, serializeAndDeserialize(instance))
    }

    private fun testPersistedDeserialization(hexValue: String, expected: Any) =
        assertEquals(expected = expected, actual = deserializeFromHex(hexValue))

    private fun testRandomsHaveSameState(first: Random, second: Random) {
        assertEquals(first.nextInt(), second.nextInt())
        assertEquals(first.nextDouble(), second.nextDouble())
        assertEquals(first.nextLong(), second.nextLong())
        assertEquals(first.nextBytes(64), second.nextBytes(64))
    }

    @Test
    fun deserializeDefault() = testPersistedDeserialization(
        "ac ed 00 05 73 72 00 1c 6b 6f 74 6c 69 6e 2e 72 61 6e 64 6f 6d 2e 52 61 6e 64 6f 6d 24 44 65 66 61 75 6c 74 59 81 49 e9 14 4e a8 28 02 00 00 78 70",
        Random
    )

    @Test
    fun xorwowIsSerializable() {
        val instance = Random(0)
        val deserialized = serializeAndDeserialize(instance)
        testRandomsHaveSameState(instance, deserialized)
    }

    @Test
    fun wrapperOfKotlinRandomIsSerializable() {
        val java = Random(0).asJavaRandom()
        val deserialized = serializeAndDeserialize(java)
        testRandomsHaveSameState(java.asKotlinRandom(), deserialized.asKotlinRandom())
    }

    @Test
    fun wrapperOfJavaRandomIsSerializable() {
        val kotlin = java.util.Random(0).asKotlinRandom()
        val deserialized = serializeAndDeserialize(kotlin)
        testRandomsHaveSameState(kotlin, deserialized)
    }
}
