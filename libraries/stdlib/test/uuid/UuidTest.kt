/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.uuid

import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
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

    private val uuidMax = Uuid.fromLongs(-1, -1)
    private val uuidStringMax = "ffffffff-ffff-ffff-ffff-ffffffffffff"
    private val uuidHexStringMax = "ffffffffffffffffffffffffffffffff"

    private val Uuid.isIetfVariant: Boolean
        get() = toULongs { _, leastSignificantBits -> (leastSignificantBits shr 62) == 2uL }

    private val Uuid.version: Int
        get() = toULongs { mostSignificantBits, _ -> ((mostSignificantBits shr 12) and 0xFuL).toInt() }

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

        // Truncating the illegal ByteArray
        val byteArray = ByteArray(32) { it.toByte() }
        val byteArrayContent = byteArray.joinToString()
        assertFailsWith<IllegalArgumentException> {
            Uuid.fromByteArray(byteArray)
        }.also { exception ->
            assertContains(exception.message!!, "but was [$byteArrayContent] of size 32")
        }
        assertFailsWith<IllegalArgumentException> {
            Uuid.fromByteArray(byteArray + 32)
        }.also { exception ->
            assertContains(exception.message!!, "but was [$byteArrayContent, ...] of size 33")
        }
    }

    @Test
    fun fromUByteArray() {
        assertEquals(
            uuidString,
            Uuid.fromUByteArray(uuidByteArray.asUByteArray()).toString()
        )
        assertSame(
            Uuid.NIL,
            Uuid.fromUByteArray(UByteArray(16))
        )
        assertEquals(
            uuidStringMax,
            Uuid.fromUByteArray(UByteArray(16) { 0xFFu }).toString()
        )

        // Illegal UByteArray size
        assertFailsWith<IllegalArgumentException> { Uuid.fromUByteArray(UByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { Uuid.fromUByteArray(UByteArray(15)) }
        assertFailsWith<IllegalArgumentException> { Uuid.fromUByteArray(UByteArray(17)) }

        // Truncating the illegal UByteArray
        val ubyteArray = UByteArray(32) { it.toUByte() }
        val ubyteArrayContent = ubyteArray.joinToString()
        assertFailsWith<IllegalArgumentException> {
            Uuid.fromUByteArray(ubyteArray)
        }.also { exception ->
            assertContains(exception.message!!, "but was [$ubyteArrayContent] of size 32")
        }
        assertFailsWith<IllegalArgumentException> {
            Uuid.fromUByteArray(ubyteArray + 32.toUByte())
        }.also { exception ->
            assertContains(exception.message!!, "but was [$ubyteArrayContent, ...] of size 33")
        }
    }

    private fun String.mixedcase(): String = map {
        if (Random.nextBoolean()) it.uppercase() else it.lowercase()
    }.joinToString("")

    @Test
    fun parse() {
        fun test(hexDash: String? = null, hex: String? = null, check: (parse: () -> Uuid) -> Unit) {
            require(hexDash != null || hex != null)

            if (hexDash != null) {
                check { Uuid.parseHexDash(hexDash) }
                check { Uuid.parse(hexDash) }
                check { Uuid.parseHexDashOrNull(hexDash)!! }
                check { Uuid.parseOrNull(hexDash)!! }
            }
            if (hex != null) {
                check { Uuid.parseHex(hex) }
                check { Uuid.parse(hex) }
                check { Uuid.parseHexOrNull(hex)!! }
                check { Uuid.parseOrNull(hex)!! }
            }
        }

        // lower case
        test(uuidString, uuidHexString) { assertEquals(uuid, it()) }
        test(uuidStringNil, uuidHexStringNil) { assertSame(Uuid.NIL, it()) }
        test(uuidStringMax, uuidHexStringMax) { assertEquals(uuidMax, it()) }

        // upper case
        test(uuidString.uppercase(), uuidHexString.uppercase()) { assertEquals(uuid, it()) }
        test(uuidStringMax.uppercase(), uuidHexStringMax.uppercase()) { assertEquals(uuidMax, it()) }

        // mixed case
        test(uuidString.mixedcase(), uuidHexString.mixedcase()) { assertEquals(uuid, it()) }
        test(uuidStringMax.mixedcase(), uuidHexStringMax.mixedcase()) { assertEquals(uuidMax, it()) }
    }

    @Test
    fun parseInvalid() {
        fun test(hexDash: String? = null, hex: String? = null, check: (parse: () -> Uuid) -> Unit) {
            require(hexDash != null || hex != null)

            if (hexDash != null) {
                check { Uuid.parseHexDash(hexDash) }
                check { Uuid.parse(hexDash) }
                assertNull(Uuid.parseHexDashOrNull(hexDash))
                assertNull(Uuid.parseOrNull(hexDash))
            }
            if (hex != null) {
                check { Uuid.parseHex(hex) }
                check { Uuid.parse(hex) }
                assertNull(Uuid.parseHexOrNull(hex))
                assertNull(Uuid.parseOrNull(hex))
            }
        }

        // Illegal String format
        assertFailsWith<IllegalArgumentException> { Uuid.parseHexDash(uuidHexString) }
        assertNull(Uuid.parseHexDashOrNull(uuidHexString))
        assertFailsWith<IllegalArgumentException> { Uuid.parseHex(uuidString) }
        assertNull(Uuid.parseHexOrNull(uuidString))
        test("$uuidString-", "$uuidHexString-") { assertFailsWith<IllegalArgumentException> { it() } }
        test("-$uuidString", "-$uuidHexString") { assertFailsWith<IllegalArgumentException> { it() } }
        test("${uuidString}0", "${uuidHexString}0") { assertFailsWith<IllegalArgumentException> { it() } }
        test("0${uuidString}", "0${uuidHexString}") { assertFailsWith<IllegalArgumentException> { it() } }
        test(uuidString.replace("d", "g"), uuidHexString.replace("d", "g")) { assertFailsWith<IllegalArgumentException> { it() } }
        test(uuidString.drop(1), uuidHexString.drop(1)) { assertFailsWith<IllegalArgumentException> { it() } }
        test(uuidString.dropLast(1), uuidHexString.dropLast(1)) { assertFailsWith<IllegalArgumentException> { it() } }

        for (i in uuidString.indices) {
            if (uuidString[i] == '-') {
                test(hexDash = uuidString.substring(0..<i) + "+" + uuidString.substring(i + 1)) {
                    assertFailsWith<IllegalArgumentException> {
                        it()
                    }.also { exception ->
                        assertEquals("Expected '-' (hyphen) at index $i, but was '+'", exception.message)
                    }
                }

                test(hexDash = uuidString.substring(0..<i) + "0" + uuidString.substring(i + 1)) {
                    assertFailsWith<IllegalArgumentException> {
                        it()
                    }.also { exception ->
                        assertEquals("Expected '-' (hyphen) at index $i, but was '0'", exception.message)
                    }
                }
            }
        }

        // Truncating the illegal String
        val longString = "x".repeat(64)
        test(longString, longString) {
            assertFailsWith<IllegalArgumentException> {
                it()
            }.also { exception ->
                assertContains(exception.message!!, "but was \"$longString\" of length 64")
            }
        }
        test(longString + "0", longString + "0") {
            assertFailsWith<IllegalArgumentException> {
                it()
            }.also { exception ->
                assertContains(exception.message!!, "but was \"$longString...\" of length 65")
            }
        }
    }

    @Test
    fun random() {
        val set = hashSetOf<Uuid>()
        repeat(10) {
            val randomUuid = Uuid.random()
            assertTrue(randomUuid.isIetfVariant)
            assertEquals(4, randomUuid.version)
            assertFalse(set.contains(randomUuid))
            set.add(randomUuid)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun lexicalOrder() {
        val maxUuid = Uuid.parse(uuidStringMax)

        // TODO: Once LEXICAL_ORDER is dropped, test Uuid.compareTo directly instead
        for (lexicalOrder in listOf(Uuid.LEXICAL_ORDER, naturalOrder())) {
            for (id in listOf(uuid, Uuid.NIL, maxUuid)) {
                assertEquals(0, lexicalOrder.compare(id, id), id.toString())
            }

            assertTrue(lexicalOrder.compare(Uuid.NIL, uuid) < 0)
            assertTrue(lexicalOrder.compare(Uuid.NIL, maxUuid) < 0)

            assertTrue(lexicalOrder.compare(uuid, Uuid.NIL) > 0)
            assertTrue(lexicalOrder.compare(uuid, maxUuid) < 0)

            assertTrue(lexicalOrder.compare(maxUuid, Uuid.NIL) > 0)
            assertTrue(lexicalOrder.compare(maxUuid, uuid) > 0)
        }
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
    fun toHexDashString() {
        assertEquals(uuidString, uuid.toHexDashString())
        assertEquals(uuidStringNil, Uuid.NIL.toHexDashString())
        assertEquals(uuidStringMax, Uuid.parse(uuidStringMax).toHexDashString())
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
    fun toUByteArray() {
        assertContentEquals(uuidByteArray.asUByteArray(), uuid.toUByteArray())
        assertContentEquals(UByteArray(16), Uuid.NIL.toUByteArray())
        assertContentEquals(UByteArray(16) { 0xFFu }, Uuid.parse(uuidStringMax).toUByteArray())
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

    @Test
    fun testV7UuidStructure() {
        val uuid = Uuid.generateV7()
        assertEquals(7, uuid.version, "Generated Uuid has a wrong version: ${uuid.toHexDashString()}")
        assertTrue(uuid.isIetfVariant, "Generated Uuid has a wrong variant: ${uuid.toHexDashString()}")
    }

    @Test
    fun testConsequentV7Uuids() {
        val uuids = Array(100) { Uuid.generateV7() }

        for (idx in 1 until uuids.size) {
            val prev = uuids[idx - 1]
            val curr = uuids[idx]
            assertTrue(
                prev < curr,
                "Uuids has to be monotonic, but following two are not: ${prev.toHexDashString()} and ${curr.toHexDashString()}"
            )
        }
    }

    @Ignore // temporary ignored, see KTI-2674
    @Test
    fun testV7UuidGenerationForNonMonotonicClock() {
        var clock: NonMonotonicClock
        var baseUuid: Uuid

        // wait until we get a counter value small enough for the test
        while (true) {
            clock = NonMonotonicClock()
            baseUuid = Uuid.generateV7(clock)
            if (baseUuid.mostSignificantBits.and(0x03FFL) <= 0x03FAL) {
                break
            }
        }

        fun checkUuidsMonotonicity(prev: Uuid, curr: Uuid) {
            assertTrue(
                prev < curr,
                "Uuids should be monotonic, but they are not: ${prev.toHexDashString()}, ${curr.toHexDashString()}"
            )

            assertTrue(prev.leastSignificantBits != curr.leastSignificantBits)
            assertEquals(1, curr.mostSignificantBits - prev.mostSignificantBits)
        }

        // clocks are frozen, they will return the same timestamp and Uuids MSB should differ by 1
        val nextUuid = Uuid.generateV7(clock)
        checkUuidsMonotonicity(baseUuid, nextUuid)

        // tick one ms back
        // clocks return a timestamp from the past, so we have to increment the counter field (rand_a) again
        clock.inc(-1L)
        val uuidFromPast = Uuid.generateV7(clock)
        checkUuidsMonotonicity(nextUuid, uuidFromPast)

        // unfreeze the clocks
        // clocks are no longer frozen, so timestamps should be different, as well as counter field's (rand_a) values
        clock.inc(42L)
        val uuidFromTheFuture = Uuid.generateV7(clock)
        assertTrue(
            uuidFromPast < uuidFromTheFuture,
            "Uuids should be monotonic, but they are not: ${uuidFromPast.toHexDashString()}, ${uuidFromTheFuture.toHexDashString()}"
        )
        assertTrue(
            uuidFromPast.mostSignificantBits.and(0x03FFL.inv()) < uuidFromTheFuture.mostSignificantBits.and(0x03FFL.inv()),
            "Uuids should have different timestamps, but they do not: ${uuidFromPast.toHexDashString()}, ${uuidFromTheFuture.toHexDashString()}"
        )

        assertTrue(
            uuidFromPast.mostSignificantBits.and(0x03FFL) != uuidFromTheFuture.mostSignificantBits.and(0x03FFL),
            "Uuids should have different rand_a values, but they do not: ${uuidFromPast.toHexDashString()}, ${uuidFromTheFuture.toHexDashString()}"
        )

        // now let's check that generation won't stuck if clocks will stuck forever
        var previousUuid = Uuid.generateV7(clock)
        // we're using 12-bit wide counter, so let's generate more uuids
        repeat(1.shl(14)) {
            val uuid = Uuid.generateV7(clock)
            assertTrue(
                previousUuid < uuid,
                "Uuids should be monotonic, but they are not: ${previousUuid.toHexDashString()}, ${uuid.toHexDashString()}"
            )
            previousUuid = uuid
        }
    }

    @Test
    fun testV7GenerateUnordered() {
        val uuid = Uuid.generateV7NonMonotonicAt(Instant.fromEpochMilliseconds(0L))

        assertEquals(7, uuid.version, "Generated Uuid has a wrong version: ${uuid.toHexDashString()}")
        assertTrue(uuid.isIetfVariant, "Generated Uuid has a wrong variant: ${uuid.toHexDashString()}")
        uuid.toLongs { msb, _ ->
            assertEquals(msb.shr(16), 0L, "Timestamp field has a wrong value")
        }

        val anotherUuid = Uuid.generateV7NonMonotonicAt(Instant.fromEpochMilliseconds(0L))
        assertNotEquals(uuid, anotherUuid, "Two uuids generated for the same timestamp should not be equal")

        val tsValue = 1757440583123L
        val nonZeroTs = Uuid.generateV7NonMonotonicAt(Instant.fromEpochMilliseconds(tsValue))
        nonZeroTs.toLongs { msb, _ ->
            assertEquals(msb.shr(16), tsValue, "Timestamp field has a wrong value")
        }
    }

    private class NonMonotonicClock : Clock {
        private var currentTime = Clock.System.now()

        override fun now(): Instant = currentTime

        fun inc(millis: Long = 1L) {
            currentTime = currentTime + millis.milliseconds
        }
    }
}
