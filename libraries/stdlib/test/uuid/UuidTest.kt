/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.uuid

import kotlin.random.Random
import kotlin.test.*
import kotlin.uuid.Uuid

class UuidTest {
    private val mostSignificantBits = 0x550e8400e29b41d4uL
    private val leastSignificantBits = 0xa716446655440000uL
    private val uuidByteArray = byteArrayOf(
        0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
        0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
    )
    private val uuid = Uuid.fromULongs(mostSignificantBits, leastSignificantBits)
    private val uuidString = "550e8400-e29b-41d4-a716-446655440000"
    private val uuidHexString = "550e8400e29b41d4a716446655440000"

    private val uuidStringNil = "00000000-0000-0000-0000-000000000000"
    private val uuidHexStringNil = "00000000000000000000000000000000"

    private val uuidStringMax = "ffffffff-ffff-ffff-ffff-ffffffffffff"
    private val uuidHexStringMax = "ffffffffffffffffffffffffffffffff"

    private val Uuid.isIetfVariant: Boolean
        get() = toULongs { _, leastSignificantBits -> (leastSignificantBits shr 62) == 2uL }

    private val Uuid.version: Int
        get() = toULongs { mostSignificantBits, _ -> ((mostSignificantBits shr 12) and 0xF.toULong()).toInt() }

    @Test
    fun fromLongs() {
        assertEquals(
            uuidString,
            Uuid.fromLongs(mostSignificantBits.toLong(), leastSignificantBits.toLong()).toString()
        )
        assertSame(
            Uuid.NIL,
            Uuid.fromLongs(0, 0)
        )
        assertEquals(
            uuidStringMax,
            Uuid.fromLongs(-1, -1).toString()
        )
    }

    @Test
    fun fromULongs() {
        assertEquals(
            uuidString,
            Uuid.fromULongs(mostSignificantBits, leastSignificantBits).toString()
        )
        assertSame(
            Uuid.NIL,
            Uuid.fromULongs(0uL, 0uL)
        )
        assertEquals(
            uuidStringMax,
            Uuid.fromULongs(ULong.MAX_VALUE, ULong.MAX_VALUE).toString()
        )
    }

    @Test
    fun fromByteArray() {
        assertEquals(
            uuidString,
            Uuid.fromByteArray(uuidByteArray).toString()
        )
        assertSame(
            Uuid.NIL,
            Uuid.fromByteArray(ByteArray(16))
        )
        assertEquals(
            uuidStringMax,
            Uuid.fromByteArray(ByteArray(16) { -1 }).toString()
        )

        // Illegal ByteArray size
        assertFailsWith<IllegalArgumentException> { Uuid.fromByteArray(ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { Uuid.fromByteArray(ByteArray(15)) }
        assertFailsWith<IllegalArgumentException> { Uuid.fromByteArray(ByteArray(17)) }
    }

    private fun String.mixedcase(): String = map {
        if (Random.nextBoolean()) it.uppercase() else it.lowercase()
    }.joinToString("")

    @Test
    fun parse() {
        // lower case
        assertEquals(uuidString, Uuid.parse(uuidString).toString())
        assertSame(Uuid.NIL, Uuid.parse(uuidStringNil))
        assertEquals(uuidStringMax, Uuid.parse(uuidStringMax).toString())

        // upper case
        assertEquals(uuidString, Uuid.parse(uuidString.uppercase()).toString())
        assertEquals(uuidStringMax, Uuid.parse(uuidStringMax.uppercase()).toString())

        // mixed case
        assertEquals(uuidString, Uuid.parse(uuidString.mixedcase()).toString())
        assertEquals(uuidStringMax, Uuid.parse(uuidStringMax.mixedcase()).toString())

        // Illegal String format
        assertFailsWith<IllegalArgumentException> { Uuid.parse(uuidHexString) }
        assertFailsWith<IllegalArgumentException> { Uuid.parse("$uuidString-") }
        assertFailsWith<IllegalArgumentException> { Uuid.parse("-$uuidString") }
        assertFailsWith<IllegalArgumentException> { Uuid.parse("${uuidString}0") }
        assertFailsWith<IllegalArgumentException> { Uuid.parse("0${uuidString}") }
        assertFailsWith<IllegalArgumentException> { Uuid.parse(uuidString.replace("d", "g")) }
        assertFailsWith<IllegalArgumentException> { Uuid.parse(uuidString.drop(1)) }
        assertFailsWith<IllegalArgumentException> { Uuid.parse(uuidString.dropLast(1)) }

        for (i in uuidString.indices) {
            if (uuidString[i] == '-') {
                assertFailsWith<IllegalArgumentException> {
                    Uuid.parse(uuidString.substring(0..<i) + "+" + uuidString.substring(i + 1))
                }
                assertFailsWith<IllegalArgumentException> {
                    Uuid.parse(uuidString.substring(0..<i) + "0" + uuidString.substring(i + 1))
                }
            }
        }
    }

    @Test
    fun parseHex() {
        // lower case
        assertEquals(uuidString, Uuid.parseHex(uuidHexString).toString())
        assertSame(Uuid.NIL, Uuid.parseHex(uuidHexStringNil))
        assertEquals(uuidStringMax, Uuid.parseHex(uuidHexStringMax).toString())

        // upper case
        assertEquals(uuidString, Uuid.parseHex(uuidHexString.uppercase()).toString())
        assertEquals(uuidStringMax, Uuid.parseHex(uuidHexStringMax.uppercase()).toString())

        // mixed case
        assertEquals(uuidString, Uuid.parseHex(uuidHexString.mixedcase()).toString())
        assertEquals(uuidStringMax, Uuid.parseHex(uuidHexStringMax.mixedcase()).toString())

        // Illegal String format
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex(uuidString) }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex("$uuidHexString-") }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex("-$uuidHexString") }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex("${uuidHexString}0") }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex("0${uuidHexString}") }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex(uuidHexString.replace("d", "g")) }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex(uuidHexString.drop(1)) }
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex(uuidHexString.dropLast(1)) }
    }

    @Test
    fun random() {
        val randomUuid = Uuid.random()
        assertTrue(randomUuid.isIetfVariant)
        assertEquals(4, randomUuid.version)
        println("Random uuid: $randomUuid")
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

        Uuid.NIL.toLongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(0, mostSignificantBits)
            assertEquals(0, leastSignificantBits)
        }

        Uuid.parse(uuidStringMax).toLongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(-1, mostSignificantBits)
            assertEquals(-1, leastSignificantBits)
        }
    }

    @Test
    fun toULongs() {
        val onceInPlace: Boolean
        val isIetfVariant = uuid.toULongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(this.mostSignificantBits, mostSignificantBits)
            assertEquals(this.leastSignificantBits, leastSignificantBits)
            onceInPlace = true
            (leastSignificantBits shr 62) == 2uL
        }
        assertTrue(onceInPlace)
        assertEquals(uuid.isIetfVariant, isIetfVariant)

        Uuid.NIL.toULongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(0uL, mostSignificantBits)
            assertEquals(0uL, leastSignificantBits)
        }

        Uuid.parse(uuidStringMax).toULongs { mostSignificantBits, leastSignificantBits ->
            assertEquals(ULong.MAX_VALUE, mostSignificantBits)
            assertEquals(ULong.MAX_VALUE, leastSignificantBits)
        }
    }

    @Test
    fun toStringTest() {
        assertEquals(uuidString, uuid.toString())
        assertEquals(uuidStringNil, Uuid.NIL.toString())
        assertEquals(uuidStringMax, Uuid.parse(uuidStringMax).toString())
    }

    @Test
    fun toHexString() {
        assertEquals(uuidHexString, uuid.toHexString())
        assertEquals(uuidHexStringNil, Uuid.NIL.toHexString())
        assertEquals(uuidHexStringMax, Uuid.parse(uuidStringMax).toHexString())
    }

    @Test
    fun toByteArray() {
        assertContentEquals(uuidByteArray, uuid.toByteArray())
        assertContentEquals(ByteArray(16), Uuid.NIL.toByteArray())
        assertContentEquals(ByteArray(16) { -1 }, Uuid.parse(uuidStringMax).toByteArray())
    }

    @Test
    fun testEquals() {
        assertEquals(uuid, Uuid.parse(uuidString))
        assertEquals(Uuid.NIL, Uuid.parse(uuidStringNil))
        assertEquals(Uuid.fromLongs(-1, -1), Uuid.parse(uuidStringMax))
    }

    @Test
    fun testHashCode() {
        assertEquals(uuid.hashCode(), Uuid.parse(uuidString).hashCode())
        assertEquals(Uuid.NIL.hashCode(), Uuid.parse(uuidStringNil).hashCode())
        assertEquals(Uuid.fromLongs(-1, -1).hashCode(), Uuid.parse(uuidStringMax).hashCode())
    }
}
