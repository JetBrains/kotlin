/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.uuid

import kotlin.test.*
import kotlin.uuid.UUID

class UUIDTest {
    private val mostSignificantBits = 0x550e8400e29b41d4uL
    private val leastSignificantBits = 0xa716446655440000uL
    private val uuidByteArray = byteArrayOf(
        0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
        0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
    )
    private val uuid = UUID.fromULongs(mostSignificantBits, leastSignificantBits)
    private val uuidString = "550e8400-e29b-41d4-a716-446655440000"
    private val uuidHexString = "550e8400e29b41d4a716446655440000"

    private val UUID.isIETFVariant: Boolean
        get() = toLongs { _, leastSignificantBits -> (leastSignificantBits ushr 62).toInt() == 2 }

    private val UUID.version: Int
        get() = toLongs { mostSignificantBits, _ -> ((mostSignificantBits shr 12) and 0xF).toInt() }

    @Test
    fun fromLongs() {
        val a = UUID.fromLongs(mostSignificantBits.toLong(), leastSignificantBits.toLong())
        assertEquals(mostSignificantBits.toLong(), a.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), a.leastSignificantBits)
    }

    @Test
    fun fromULongs() {
        val a = UUID.fromULongs(mostSignificantBits, leastSignificantBits)
        assertEquals(mostSignificantBits, a.mostSignificantBits.toULong())
        assertEquals(leastSignificantBits, a.leastSignificantBits.toULong())
    }

    @Test
    fun fromByteArray() {
        val a = UUID.fromByteArray(uuidByteArray)
        assertEquals(mostSignificantBits.toLong(), a.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), a.leastSignificantBits)
    }

    @Test
    fun parse() {
        // lower case
        val a = UUID.parse(uuidString)
        assertEquals(mostSignificantBits.toLong(), a.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), a.leastSignificantBits)

        // upper case
        val b = UUID.parse(uuidString.uppercase())
        assertEquals(mostSignificantBits.toLong(), b.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), b.leastSignificantBits)

        // mixed case
        val c = UUID.parse("550e8400-E29b-41D4-a716-446655440000")
        assertEquals(mostSignificantBits.toLong(), c.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), c.leastSignificantBits)
    }

    @Test
    fun parseHex() {
        // lower case
        val a = UUID.parseHex(uuidHexString)
        assertEquals(mostSignificantBits.toLong(), a.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), a.leastSignificantBits)

        // upper case
        val b = UUID.parseHex(uuidHexString.uppercase())
        assertEquals(mostSignificantBits.toLong(), b.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), b.leastSignificantBits)

        // mixed case
        val c = UUID.parseHex("550E8400e29B41d4A716446655440000")
        assertEquals(mostSignificantBits.toLong(), c.mostSignificantBits)
        assertEquals(leastSignificantBits.toLong(), c.leastSignificantBits)
    }

    @Test
    fun random() {
        val randomUUID = UUID.random()
        assertTrue(randomUUID.isIETFVariant)
        assertEquals(4, randomUUID.version)
        println("Random UUID: $randomUUID")
    }

    @Test
    fun toLongs() {
        uuid.toLongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(this.mostSignificantBits.toLong(), mostSignificantBits)
            assertEquals(this.leastSignificantBits.toLong(), leastSignificantBits)
        }
    }

    @Test
    fun toStringTest() {
        assertEquals(uuidString, uuid.toString())
    }

    @Test
    fun toHexString() {
        assertEquals(uuidHexString, uuid.toHexString())
    }

    @Test
    fun toByteArray() {
        assertContentEquals(uuidByteArray, uuid.toByteArray())
    }
}
