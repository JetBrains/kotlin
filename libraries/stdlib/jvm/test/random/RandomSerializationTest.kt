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
import kotlin.test.assertTrue

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
    fun deserializeDefault() = assertSame(
        expected = Random,
        actual = deserializeFromHex("ac ed 00 05 73 72 00 22 6b 6f 74 6c 69 6e 2e 72 61 6e 64 6f 6d 2e 52 61 6e 64 6f 6d 24 44 65 66 61 75 6c 74 24 44 75 6d 6d 79 00 00 00 00 00 00 00 00 02 00 00 78 70"),
    )

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
