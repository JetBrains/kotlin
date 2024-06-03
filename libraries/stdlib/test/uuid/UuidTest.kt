/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.uuid

import kotlin.random.Random
import kotlin.test.*
import kotlin.uuid.UUID

class UuidTest {
    private val mostSignificantBits = 0x550e8400e29b41d4uL
    private val leastSignificantBits = 0xa716446655440000uL
    private val uuidByteArray = byteArrayOf(
        0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
        0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
    )
    private val uuid = UUID.fromULongs(mostSignificantBits, leastSignificantBits)
    private val uuidString = "550e8400-e29b-41d4-a716-446655440000"
    private val uuidHexString = "550e8400e29b41d4a716446655440000"

    private val uuidStringNil = "00000000-0000-0000-0000-000000000000"
    private val uuidHexStringNil = "00000000000000000000000000000000"

    private val uuidStringMax = "ffffffff-ffff-ffff-ffff-ffffffffffff"
    private val uuidHexStringMax = "ffffffffffffffffffffffffffffffff"

    private val UUID.isIETFVariant: Boolean
        get() = toULongs { _, leastSignificantBits -> (leastSignificantBits shr 62) == 2uL }

    private val UUID.version: Int
        get() = toULongs { mostSignificantBits, _ -> ((mostSignificantBits shr 12) and 0xF.toULong()).toInt() }

    @Test
    fun fromLongs() {
        assertEquals(
            uuidString,
            UUID.fromLongs(mostSignificantBits.toLong(), leastSignificantBits.toLong()).toString()
        )
        assertEquals(
            uuidStringNil,
            UUID.fromLongs(0, 0).toString()
        )
        assertEquals(
            uuidStringMax,
            UUID.fromLongs(-1, -1).toString()
        )
    }

    @Test
    fun fromULongs() {
        assertEquals(
            uuidString,
            UUID.fromULongs(mostSignificantBits, leastSignificantBits).toString()
        )
        assertEquals(
            uuidStringNil,
            UUID.fromULongs(0uL, 0uL).toString()
        )
        assertEquals(
            uuidStringMax,
            UUID.fromULongs(ULong.MAX_VALUE, ULong.MAX_VALUE).toString()
        )
    }

    @Test
    fun fromByteArray() {
        assertEquals(
            uuidString,
            UUID.fromByteArray(uuidByteArray).toString()
        )
        assertEquals(
            uuidStringNil,
            UUID.fromByteArray(ByteArray(16)).toString()
        )
        assertEquals(
            uuidStringMax,
            UUID.fromByteArray(ByteArray(16) { -1 }).toString()
        )

        // Illegal ByteArray size
        assertFailsWith<IllegalArgumentException> { UUID.fromByteArray(ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { UUID.fromByteArray(ByteArray(15)) }
        assertFailsWith<IllegalArgumentException> { UUID.fromByteArray(ByteArray(17)) }
    }

    private fun String.mixedcase(): String = map {
        if (Random.nextBoolean()) it.uppercase() else it.lowercase()
    }.joinToString("")

    @Test
    fun parse() {
        // lower case
        assertEquals(uuidString, UUID.parse(uuidString).toString())
        assertEquals(uuidStringNil, UUID.parse(uuidStringNil).toString())
        assertEquals(uuidStringMax, UUID.parse(uuidStringMax).toString())

        // upper case
        assertEquals(uuidString, UUID.parse(uuidString.uppercase()).toString())
        assertEquals(uuidStringMax, UUID.parse(uuidStringMax.uppercase()).toString())

        // mixed case
        assertEquals(uuidString, UUID.parse(uuidString.mixedcase()).toString())
        assertEquals(uuidStringMax, UUID.parse(uuidStringMax.mixedcase()).toString())

        // Illegal String format
        assertFailsWith<IllegalArgumentException> { UUID.parse(uuidHexString) }
        assertFailsWith<IllegalArgumentException> { UUID.parse("$uuidString-") }
        assertFailsWith<IllegalArgumentException> { UUID.parse("-$uuidString") }
        assertFailsWith<IllegalArgumentException> { UUID.parse("${uuidString}0") }
        assertFailsWith<IllegalArgumentException> { UUID.parse("0${uuidString}") }
        assertFailsWith<IllegalArgumentException> { UUID.parse(uuidString.replace("d", "g")) }
        assertFailsWith<IllegalArgumentException> { UUID.parse(uuidString.drop(1)) }
        assertFailsWith<IllegalArgumentException> { UUID.parse(uuidString.dropLast(1)) }

        for (i in uuidString.indices) {
            if (uuidString[i] == '-') {
                assertFailsWith<IllegalArgumentException> {
                    UUID.parse(uuidString.substring(0..<i) + "+" + uuidString.substring(i + 1))
                }
                assertFailsWith<IllegalArgumentException> {
                    UUID.parse(uuidString.substring(0..<i) + "0" + uuidString.substring(i + 1))
                }
            }
        }
    }

    @Test
    fun parseHex() {
        // lower case
        assertEquals(uuidString, UUID.parseHex(uuidHexString).toString())
        assertEquals(uuidStringNil, UUID.parseHex(uuidHexStringNil).toString())
        assertEquals(uuidStringMax, UUID.parseHex(uuidHexStringMax).toString())

        // upper case
        assertEquals(uuidString, UUID.parseHex(uuidHexString.uppercase()).toString())
        assertEquals(uuidStringMax, UUID.parseHex(uuidHexStringMax.uppercase()).toString())

        // mixed case
        assertEquals(uuidString, UUID.parseHex(uuidHexString.mixedcase()).toString())
        assertEquals(uuidStringMax, UUID.parseHex(uuidHexStringMax.mixedcase()).toString())

        // Illegal String format
        assertFailsWith<IllegalArgumentException> { UUID.parseHex(uuidString) }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex("$uuidHexString-") }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex("-$uuidHexString") }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex("${uuidHexString}0") }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex("0${uuidHexString}") }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex(uuidHexString.replace("d", "g")) }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex(uuidHexString.drop(1)) }
        assertFailsWith<IllegalArgumentException> { UUID.parseHex(uuidHexString.dropLast(1)) }
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
        val onceInPlace: Boolean
        val version = uuid.toLongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(this.mostSignificantBits.toLong(), mostSignificantBits)
            assertEquals(this.leastSignificantBits.toLong(), leastSignificantBits)
            onceInPlace = true
            ((mostSignificantBits shr 12) and 0xF).toInt()
        }
        assertTrue(onceInPlace)
        assertEquals(uuid.version, version)

        UUID.NIL.toLongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(0, mostSignificantBits)
            assertEquals(0, leastSignificantBits)
        }

        UUID.parse(uuidStringMax).toLongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(-1, mostSignificantBits)
            assertEquals(-1, leastSignificantBits)
        }
    }

    @Test
    fun toULongs() {
        val onceInPlace: Boolean
        val isIETFVariant = uuid.toULongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(this.mostSignificantBits, mostSignificantBits)
            assertEquals(this.leastSignificantBits, leastSignificantBits)
            onceInPlace = true
            (leastSignificantBits shr 62) == 2uL
        }
        assertTrue(onceInPlace)
        assertEquals(uuid.isIETFVariant, isIETFVariant)

        UUID.NIL.toULongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(0uL, mostSignificantBits)
            assertEquals(0uL, leastSignificantBits)
        }

        UUID.parse(uuidStringMax).toULongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(ULong.MAX_VALUE, mostSignificantBits)
            assertEquals(ULong.MAX_VALUE, leastSignificantBits)
        }
    }

    @Test
    fun toStringTest() {
        assertEquals(uuidString, uuid.toString())
        assertEquals(uuidStringNil, UUID.NIL.toString())
        assertEquals(uuidStringMax, UUID.parse(uuidStringMax).toString())
    }

    @Test
    fun toHexString() {
        assertEquals(uuidHexString, uuid.toHexString())
        assertEquals(uuidHexStringNil, UUID.NIL.toHexString())
        assertEquals(uuidHexStringMax, UUID.parse(uuidStringMax).toHexString())
    }

    @Test
    fun toByteArray() {
        assertContentEquals(uuidByteArray, uuid.toByteArray())
        assertContentEquals(ByteArray(16), UUID.NIL.toByteArray())
        assertContentEquals(ByteArray(16) { -1 }, UUID.parse(uuidStringMax).toByteArray())
    }

    @Test
    fun testEquals() {
        assertEquals(uuid, UUID.parse(uuidString))
        assertEquals(UUID.NIL, UUID.parse(uuidStringNil))
        assertEquals(UUID.fromLongs(-1, -1), UUID.parse(uuidStringMax))
    }

    @Test
    fun testHashCode() {
        assertEquals(uuid.hashCode(), UUID.parse(uuidString).hashCode())
        assertEquals(UUID.NIL.hashCode(), UUID.parse(uuidStringNil).hashCode())
        assertEquals(UUID.fromLongs(-1, -1).hashCode(), UUID.parse(uuidStringMax).hashCode())
    }
}
