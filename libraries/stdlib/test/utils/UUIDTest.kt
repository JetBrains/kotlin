/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

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
    fun randomUUID() {
        val randomUUID = UUID.randomUUIDv4()
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
    fun isIETFVariant() {
        // bits at 64..65 are 0b10
        assertTrue(uuid.isIETFVariant)

        // bits at 64..65 are 0b01
        val b = UUID.fromULongs(mostSignificantBits, leastSignificantBits xor (0x3uL shl 62))
        assertFalse(b.isIETFVariant)

        // bits at 64..65 are 0b00
        val c = UUID.fromULongs(mostSignificantBits, leastSignificantBits xor (0x2uL shl 62))
        assertFalse(c.isIETFVariant)

        // bits at 64..65 are 0b11
        val d = UUID.fromULongs(mostSignificantBits, leastSignificantBits xor (0x1uL shl 62))
        assertFalse(d.isIETFVariant)
    }

    @Test
    fun version() {
        // bits at 48..51 are 0b0100
        assertEquals(4, uuid.version)

        // bits at 48..51 are 0b0000
        val b = UUID.fromULongs(mostSignificantBits xor (0x4uL shl 12), leastSignificantBits)
        assertEquals(0, b.version)

        // bits at 48..51 are 0b1111
        val c = UUID.fromULongs(mostSignificantBits xor (0xBuL shl 12), leastSignificantBits)
        assertEquals(15, c.version)
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