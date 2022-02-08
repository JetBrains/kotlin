/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.random

import test.io.*
import kotlin.random.*
import kotlin.test.*

class RandomSerializationTest {
    @Test
    fun defaultIsSerializable() {
        val instance = Random
        discardSomeValues(instance)
        assertSame(instance, serializeAndDeserialize(instance))
    }

    private fun discardSomeValues(instance: Random) {
        instance.nextInt()
        instance.nextDouble()
        instance.nextLong()
        instance.nextBytes(64)
    }

    private fun testRandomsHaveSameState(first: Random, second: Random) {
        assertEquals(first.nextInt(), second.nextInt())
        assertEquals(first.nextDouble(), second.nextDouble())
        assertEquals(first.nextLong(), second.nextLong())
        assertTrue(first.nextBytes(64).contentEquals(second.nextBytes(64)))
    }

    @Test
    fun deserializeDefault() {
        val randomSerialized = serializeToHex(Random)
        try {
            assertSame(
                    expected = Random,
                    actual = deserializeFromHex("ac ed 00 05 73 72 00 27 6b 6f 74 6c 69 6e 2e 72 61 6e 64 6f 6d 2e 52 61 6e 64 6f 6d 24 44 65 66 61 75 6c 74 24 53 65 72 69 61 6c 69 7a 65 64 00 00 00 00 00 00 00 00 02 00 00 78 70")
            )
        } catch (e: Throwable) {
            fail("Actual serialized form: $randomSerialized", e)
        }
    }

    @Test
    fun deserializeXorWow() {
        fun checkEquivalentToSerialized(instance: Random, serializedHex: String) {
            val serialized = serializeToHex(instance)

            try {
                val deserialized = deserializeFromHex<Random>(serializedHex)
                testRandomsHaveSameState(instance, deserialized)
            } catch (e: Throwable) {
                fail("Actual serialized from: $serialized", e)
            }
        }

        checkEquivalentToSerialized(
                instance = Random(0),
                serializedHex = "ac ed 00 05 73 72 00 1a 6b 6f 74 6c 69 6e 2e 72 61 6e 64 6f 6d 2e 58 6f 72 57 6f 77 52 61 6e 64 6f 6d 00 00 00 00 00 00 00 00 02 00 06 49 00 06 61 64 64 65 6e 64 49 00 01 76 49 00 01 77 49 00 01 78 49 00 01 79 49 00 01 7a 78 70 01 61 f1 40 23 9d 3d b3 c9 07 82 1d 6a 67 48 b0 e3 f9 bd 0c b6 5b de 32"
        )

        checkEquivalentToSerialized(
                instance = Random(0).apply { repeat(64) { nextLong() } }, // advance state by discarding values
                serializedHex = "ac ed 00 05 73 72 00 1a 6b 6f 74 6c 69 6e 2e 72 61 6e 64 6f 6d 2e 58 6f 72 57 6f 77 52 61 6e 64 6f 6d 00 00 00 00 00 00 00 00 02 00 06 49 00 06 61 64 64 65 6e 64 49 00 01 76 49 00 01 77 49 00 01 78 49 00 01 79 49 00 01 7a 78 70 04 25 d3 c0 be 0e 05 94 fd be 13 de a4 17 b2 90 b0 de a4 64 9c 72 81 64"
        )
    }

    @Test
    fun xorwowIsSerializable() {
        val instance = Random(0)
        discardSomeValues(instance)
        val deserialized = serializeAndDeserialize(instance)
        testRandomsHaveSameState(instance, deserialized)
    }

    @Test
    fun wrapperOfKotlinRandomIsSerializable() {
        val java = Random(0).asJavaRandom()
        java.nextInt()
        java.nextDouble()
        java.nextLong()
        java.nextBytes(ByteArray(64) { 0 })
        val deserialized = serializeAndDeserialize(java)
        testRandomsHaveSameState(java.asKotlinRandom(), deserialized.asKotlinRandom())
    }

    @Test
    fun wrapperOfJavaRandomIsSerializable() {
        val kotlin = java.util.Random(0).asKotlinRandom()
        discardSomeValues(kotlin)
        val deserialized = serializeAndDeserialize(kotlin)
        testRandomsHaveSameState(kotlin, deserialized)
    }
}
