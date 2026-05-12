/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import kotlin.test.*

class CustomBitSetTest {

    // ---- Construction -------------------------------------------------------

    @Test
    fun defaultConstructorIsEmptyAndSparse() {
        val bs = CustomBitSet()
        assertTrue(bs.isEmpty)
        assertEquals(0, bs.cardinality())
        assertEquals(0, bs.size)
        assertSparse(bs)
    }

    @Test
    fun nodesCountConstructorIsEmptyAndDense() {
        for (nodesCount in listOf(0, 1, 64, 100, 1_000)) {
            val bs = CustomBitSet(nodesCount)
            assertTrue(bs.isEmpty, "nodesCount=$nodesCount")
            assertEquals(0, bs.cardinality(), "nodesCount=$nodesCount")
            assertEquals(0, bs.size, "nodesCount=$nodesCount")
            assertDense(bs, "nodesCount=$nodesCount")
        }
    }

    @Test
    fun emptyBitsetIsEmpty() {
        forEachMode(intArrayOf()) { (bs) ->
            assertTrue(bs.isEmpty)
            assertEquals(0, bs.cardinality())
            assertEquals(0, bs.size)
        }
    }

    @Test
    fun emptyConstructorVariantsAreEqual() {
        // CustomBitSet(), CustomBitSet(N), and valueOf(longArrayOf()) all denote ∅.
        val variants = listOf(
                CustomBitSet(),
                CustomBitSet(0),
                CustomBitSet(64),
                CustomBitSet.valueOf(longArrayOf()),
                CustomBitSet.valueOf(longArrayOf(0L, 0L)),
        )
        for (a in variants) {
            for (b in variants) {
                assertEquals(a, b)
                assertEquals(a.hashCode(), b.hashCode())
            }
        }
    }

    // ---- Bit operations -----------------------------------------------------
    // These tests run in both sparse and dense representations.

    @Test
    fun setAndGet() {
        forEachMode(intArrayOf(0, 63)) { (bs) ->
            assertTrue(bs[0])
            assertTrue(bs[63])
            assertFalse(bs[1])
            assertFalse(bs[64])
            assertEquals(2, bs.cardinality())
        }
    }

    @Test
    fun setAndClear() {
        forEachMode(intArrayOf(5)) { (bs) ->
            assertTrue(bs[5])
            bs.clear(5)
            assertFalse(bs[5])
            assertTrue(bs.isEmpty)
            assertEquals(0, bs.cardinality())
            assertEquals(0, bs.size)
        }
    }

    @Test
    fun setIsIdempotent() {
        forEachMode(intArrayOf()) { (bs) ->
            bs.set(5)
            bs.set(5)
            assertEquals(1, bs.cardinality())
            assertTrue(bs[5])
        }
    }

    @Test
    fun clearUnsetBitIsNoOp() {
        forEachMode(intArrayOf(0)) { (bs) ->
            bs.clear(5)   // bit 5 not set; bit 5 is in-range (word 0)
            assertTrue(bs[0])
            assertFalse(bs[5])
            assertEquals(1, bs.cardinality())
        }
    }

    @Test
    fun isNotEmptyWhenBitSet() {
        forEachMode(intArrayOf(7)) { (bs) ->
            assertFalse(bs.isEmpty)
        }
    }

    @Test
    fun copyIsIndependent() {
        forEachMode(intArrayOf(3, 42)) { (bs) ->
            val copy = bs.copy()
            assertEquals(bs, copy)
            // mutate copy: original unaffected
            copy.set(100)
            assertFalse(bs[100])
            // mutate original: copy unaffected
            bs.clear(3)
            assertTrue(copy[3])
        }
    }

    @Test
    fun copyOfEmptyIsEmpty() {
        forEachMode(intArrayOf()) { (bs) ->
            val copy = bs.copy()
            assertTrue(copy.isEmpty)
            assertEquals(bs, copy)
        }
    }

    @Test
    fun booleanSetOperator() {
        forEachMode(intArrayOf()) { (bs) ->
            bs[5] = true
            assertTrue(bs[5])
            bs[5] = false
            assertFalse(bs[5])
        }
    }

    @Test
    fun setAndGetAcrossWords() {
        forEachMode(intArrayOf(0, 63, 64, 127, 128, 191, 192)) { (bs) ->
            for (bit in listOf(0, 63, 64, 127, 128, 191, 192)) {
                assertTrue(bs[bit], "bit $bit should be set")
            }
            assertEquals(7, bs.cardinality())
        }
    }

    @Test
    fun fullClearResetsAll() {
        forEachMode(intArrayOf(0, 64, 128)) { (bs) ->
            bs.clear()
            assertTrue(bs.isEmpty)
            assertEquals(0, bs.cardinality())
            assertEquals(0, bs.toBitList().size)
        }
    }

    @Test
    fun clearBitUpdatesCardinality() {
        forEachMode(intArrayOf(0, 128)) { (bs) ->
            bs.clear(128)
            assertEquals(1, bs.cardinality())
            assertTrue(bs[0])
            assertFalse(bs[128])
        }
    }

    @Test
    fun forEachWordVisitsSetWords() {
        forEachMode(intArrayOf(0, 64)) { (bs) ->
            val words = mutableListOf<Long>()
            bs.forEachWord { words.add(it) }
            assertTrue(words.size >= 2)
            assertTrue(words[0] and 1L != 0L, "word 0 bit 0")
            assertTrue(words[1] and 1L != 0L, "word 1 bit 0")
        }
    }

    // ---- Threshold transition -----------------------------------------------

    @Test
    fun staysSparseUntilThreshold() {
        val bs = CustomBitSet()
        bs.set(0)
        bs.set(64)   // second word, still sparse (2 < threshold)
        assertSparse(bs)
        assertEquals(2, bs.cardinality())
        assertTrue(bs[0])
        assertTrue(bs[64])
    }

    @Test
    fun transitionAt8BitsAllBitsRetained() {
        val bs = CustomBitSet()
        for (i in 0 until 7) bs.set(i * 10)
        assertSparse(bs, "premature dense conversion at 7 bits (threshold is 8)")
        assertEquals(7, bs.cardinality())
        bs.set(70)   // 8th bit — triggers buildFromLazy
        assertDense(bs, "8th set did not trigger dense conversion")
        assertEquals(8, bs.cardinality())
        for (i in 0 until 7) assertTrue(bs[i * 10], "bit ${i * 10} should survive transition")
        assertTrue(bs[70])
    }

    @Test
    fun hashCodeConsistentAcrossTransition() {
        val bits = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70)
        val converted = CustomBitSet()
        bits.forEach { converted.set(it) }   // 8th set triggers conversion
        assertDense(converted, "8th set did not trigger dense conversion")

        val neverSparse = denseOf(*bits)

        assertEquals(converted, neverSparse)
        assertEquals(converted.hashCode(), neverSparse.hashCode())
        assertEquals(converted.hashCodeLong(), neverSparse.hashCodeLong())
    }

    @Test
    fun copyBeforeAndAfterTransitionAreCorrect() {
        val bs = CustomBitSet()
        for (i in 0 until 7) bs.set(i)
        val beforeTransition = bs.copy()
        assertSparse(beforeTransition, "copy of sparse bitset is unexpectedly dense")

        bs.set(7)   // transition
        assertDense(bs, "8th set did not trigger dense conversion")
        val afterTransition = bs.copy()
        assertDense(afterTransition, "copy of dense bitset is unexpectedly sparse")

        assertEquals(7, beforeTransition.cardinality())
        for (i in 0 until 7) assertTrue(beforeTransition[i])

        assertEquals(bs, afterTransition)
        assertEquals(8, afterTransition.cardinality())
    }

    // ---- Logical operations -------------------------------------------------
    // Each test runs through every (sparse, dense) × (sparse, dense) combination
    // for the two operands. The mode of either input — and of the result — is an
    // implementation detail; only the bit-level outcome is asserted.

    @Test
    fun or() {
        forEachMode(intArrayOf(0, 64), intArrayOf(64, 128)) { (a, b) ->
            a.or(b)
            assertEquals(listOf(0, 64, 128), a.toBitList())
        }
    }

    @Test
    fun orGrowsWhenNeeded() {
        forEachMode(intArrayOf(0), intArrayOf(0, 200)) { (a, b) ->
            a.or(b)
            assertEquals(listOf(0, 200), a.toBitList())
        }
    }

    @Test
    fun and() {
        forEachMode(intArrayOf(0, 64, 128), intArrayOf(64, 128, 192)) { (a, b) ->
            a.and(b)
            assertEquals(listOf(64, 128), a.toBitList())
        }
    }

    @Test
    fun andNot() {
        forEachMode(intArrayOf(0, 64, 128), intArrayOf(64)) { (a, b) ->
            a.andNot(b)
            assertEquals(listOf(0, 128), a.toBitList())
        }
    }

    @Test
    fun intersectsOverlapping() {
        forEachMode(intArrayOf(0, 64), intArrayOf(64, 128)) { (a, b) ->
            assertTrue(a.intersects(b))
        }
    }

    @Test
    fun intersectsDisjoint() {
        forEachMode(intArrayOf(0, 64), intArrayOf(128, 192)) { (a, b) ->
            assertFalse(a.intersects(b))
        }
    }

    @Test
    fun containsSubset() {
        forEachMode(intArrayOf(0, 64, 128), intArrayOf(64, 128)) { (a, b) ->
            assertTrue(a.contains(b))
        }
    }

    @Test
    fun containsNotSubset() {
        forEachMode(intArrayOf(64, 128), intArrayOf(0, 64, 128)) { (a, b) ->
            assertFalse(a.contains(b))
        }
    }

    @Test
    fun intersectsSelfWhenNonEmpty() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertTrue(a.intersects(a))
        }
    }

    @Test
    fun containsSelf() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertTrue(a.contains(a))
        }
    }

    @Test
    fun orWithEmptyIsNoOp() {
        forEachMode(intArrayOf(0, 64), intArrayOf()) { (a, empty) ->
            a.or(empty)
            assertEquals(listOf(0, 64), a.toBitList())
        }
    }

    @Test
    fun intersectsEmptyIsFalse() {
        forEachMode(intArrayOf(0, 64), intArrayOf()) { (a, empty) ->
            assertFalse(a.intersects(empty))
        }
    }

    @Test
    fun orHasChangedWithEmptyReturnsFalse() {
        forEachMode(intArrayOf(0, 64), intArrayOf()) { (a, empty) ->
            assertFalse(a.orHasChanged(empty))
            assertEquals(listOf(0, 64), a.toBitList())
        }
    }

    @Test
    fun orHasChangedNoChange() {
        forEachMode(intArrayOf(0, 64), intArrayOf(0)) { (a, b) ->
            assertFalse(a.orHasChanged(b))
            assertEquals(listOf(0, 64), a.toBitList())
        }
    }

    @Test
    fun orHasChangedAddsBit() {
        forEachMode(intArrayOf(0), intArrayOf(0, 64)) { (a, b) ->
            assertTrue(a.orHasChanged(b))
            assertEquals(listOf(0, 64), a.toBitList())
        }
    }

    @Test
    fun orWithFilterArgOnlyAllowsFilteredBits() {
        // only bit 64 passes the filter; bit 128 must not be added
        forEachMode(intArrayOf(0), intArrayOf(0, 64, 128), intArrayOf(64)) { (a, b, filter) ->
            assertTrue(a.orWithFilterHasChanged(b, filter))
            assertTrue(a[64])
            assertFalse(a[128])
        }
    }

    @Test
    fun orWithFilterArgNoChangeWhenFilteredBitsAlreadySet() {
        // 128 excluded by filter
        forEachMode(intArrayOf(0, 64), intArrayOf(0, 64, 128), intArrayOf(0, 64)) { (a, b, filter) ->
            assertFalse(a.orWithFilterHasChanged(b, filter))
            assertFalse(a[128])
        }
    }

    @Test
    fun orWithFilterArgSizeAfterTrim() {
        // ensureCapacity bounded by min(asize, fsize), so words past filter.size
        // are never touched. size must reflect the actual highest set bit.
        forEachMode(intArrayOf(0), intArrayOf(0, 64, 128), intArrayOf(64)) { (a, b, filter) ->
            a.orWithFilterHasChanged(b, filter)
            assertEquals(listOf(0, 64), a.toBitList())
            assertEquals(2, a.size)
        }
    }

    @Test
    fun orWithFilterArgEmptyIntersectionLeavesSizeZero() {
        // Even with the right ensureCapacity bound, the AND of another and filter
        // can be all zeros at the highest covered word. shrinkDenseSize catches it.
        forEachMode(intArrayOf(), intArrayOf(0), intArrayOf(64)) { (a, b, filter) ->
            a.orWithFilterHasChanged(b, filter)
            assertTrue(a.isEmpty)
            assertEquals(0, a.size)
        }
    }

    @Test
    fun orWithFilterArgEmptyFilterBlocksEverything() {
        forEachMode(intArrayOf(0), intArrayOf(0, 64, 128), intArrayOf()) { (a, b, filter) ->
            assertFalse(a.orWithFilterHasChanged(b, filter))
            assertEquals(listOf(0), a.toBitList())
        }
    }

    @Test
    fun orWithFilterArgFilterMatchingAnotherAddsAllItsBits() {
        // When filter ⊇ another, every bit in another passes — equivalent to orHasChanged
        forEachMode(intArrayOf(0), intArrayOf(0, 64, 128), intArrayOf(0, 64, 128)) { (a, b, filter) ->
            a.orWithFilterHasChanged(b, filter)
            assertEquals(listOf(0, 64, 128), a.toBitList())
        }
    }

    // ---- equals / hashCode --------------------------------------------------

    @Test
    fun equalsReflexive() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertEquals(a, a)
            assertEquals(a.hashCode(), a.hashCode())
        }
    }

    @Test
    fun equalsContractAcrossModes() {
        // Symmetry, equals/hashCode consistency, all four mode pairs.
        forEachMode(intArrayOf(0, 64, 128), intArrayOf(0, 64, 128)) { (a, b) ->
            assertEquals(a, b)
            assertEquals(b, a)
            assertEquals(a.hashCode(), b.hashCode())
            assertEquals(a.hashCodeLong(), b.hashCodeLong())
        }
    }

    @Test
    fun hashCodeLongRelationToHashCode() {
        forEachMode(intArrayOf(0, 63, 64, 127)) { (bs) ->
            val h = bs.hashCodeLong()
            assertEquals(((h ushr 32) xor h).toInt(), bs.hashCode())
        }
    }

    @Test
    fun differentBitsMeansNotEqual() {
        forEachMode(intArrayOf(0, 1), intArrayOf(0, 2)) { (a, b) ->
            assertNotEquals(a, b)
        }
        forEachMode(intArrayOf(0, 64), intArrayOf(0, 128)) { (a, b) ->
            assertNotEquals(a, b)
        }
    }

    @Test
    fun equalsAgainstNullAndOtherType() {
        forEachMode(intArrayOf(0, 64)) { (bs) ->
            assertFalse(bs.equals(null))
            assertFalse(bs.equals("not a bitset"))
            assertFalse(bs.equals(42))
        }
    }

    @Test
    fun hashCodeLongOfEmptyIsSeed() {
        forEachMode(intArrayOf()) { (bs) ->
            assertEquals(1234L, bs.hashCodeLong())
        }
    }

    // ---- Size-tracking regressions ------------------------------------------
    // Each scenario must hold across all sparse/dense combinations of the inputs.

    @Test
    fun andSizeAfterTrim() {
        // a clears the high word of b, so b.size must shrink
        forEachMode(intArrayOf(0, 64), intArrayOf(0)) { (a, b) ->
            a.and(b)
            assertEquals(listOf(0), a.toBitList())
            assertEquals(1, a.size)
        }
    }

    @Test
    fun andWithEmptyResultsInEmpty() {
        forEachMode(intArrayOf(0, 64, 128), intArrayOf()) { (a, b) ->
            a.and(b)
            assertTrue(a.isEmpty)
            assertEquals(0, a.cardinality())
            assertEquals(0, a.size)
        }
    }

    @Test
    fun andNotSizeAfterTrim() {
        // andNot clears the high word of a, so a.size must shrink
        forEachMode(intArrayOf(0, 64), intArrayOf(64)) { (a, b) ->
            a.andNot(b)
            assertEquals(listOf(0), a.toBitList())
            assertEquals(1, a.size)
        }
    }

    // ---- Edge cases ---------------------------------------------------------

    @Test
    fun orRejectsSelfArgument() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertFailsWith<IllegalArgumentException> { a.or(a) }
        }
    }

    @Test
    fun orHasChangedRejectsSelfArgument() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertFailsWith<IllegalArgumentException> { a.orHasChanged(a) }
        }
    }

    @Test
    fun orWithFilterHasChangedRejectsSelfArgument() {
        forEachMode(intArrayOf(0, 64), intArrayOf(0)) { (a, b) ->
            assertFailsWith<IllegalArgumentException> { a.orWithFilterHasChanged(a, b) }
            assertFailsWith<IllegalArgumentException> { a.orWithFilterHasChanged(b, a) }
        }
    }

    @Test
    fun andRejectsSelfArgument() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertFailsWith<IllegalArgumentException> { a.and(a) }
        }
    }

    @Test
    fun andNotRejectsSelfArgument() {
        forEachMode(intArrayOf(0, 64, 128)) { (a) ->
            assertFailsWith<IllegalArgumentException> { a.andNot(a) }
        }
    }

    @Test
    fun anyBitsetContainsEmpty() {
        forEachMode(intArrayOf(0, 64), intArrayOf()) { (a, empty) ->
            assertTrue(a.contains(empty))
        }
    }

    @Test
    fun emptyDoesNotContainNonEmpty() {
        forEachMode(intArrayOf(), intArrayOf(0)) { (empty, bs) ->
            assertFalse(empty.contains(bs))
        }
    }

    @Test
    fun emptyBitsetOperations() {
        forEachMode(intArrayOf(), intArrayOf()) { (a, b) ->
            a.or(b)
            assertTrue(a.isEmpty)
            a.and(b)
            assertTrue(a.isEmpty)
            a.andNot(b)
            assertTrue(a.isEmpty)
            assertFalse(a.intersects(b))
            assertTrue(a.contains(b))
            assertTrue(b.contains(a))
            assertEquals(a, b)
        }
    }

    @Test
    fun wordBoundaryBitsSetAndGet() {
        for (bit in listOf(0, 63, 64, 127, 128)) {
            forEachMode(intArrayOf(bit)) { (bs) ->
                assertTrue(bs[bit], "bit $bit should be set")
                if (bit > 0) assertFalse(bs[bit - 1], "bit ${bit - 1} should not be set")
                assertEquals(1, bs.cardinality(), "cardinality for single bit $bit")
            }
        }
    }

    // ---- valueOf ------------------------------------------------------------

    @Test
    fun valueOfProducesCorrectBits() {
        val bs = CustomBitSet.valueOf(longArrayOf(0b101L))
        assertTrue(bs[0])
        assertFalse(bs[1])
        assertTrue(bs[2])
        assertEquals(2, bs.cardinality())
    }

    @Test
    fun valueOfPreservesInteriorZeros() {
        val bs = CustomBitSet.valueOf(longArrayOf(1L, 0L, 1L))
        assertEquals(3, bs.size)
        assertTrue(bs[0])
        assertFalse(bs[64])
        assertTrue(bs[128])
        assertEquals(2, bs.cardinality())
    }

    @Test
    fun valueOfTrimsTrailingZeros() {
        // empty input
        assertEquals(CustomBitSet(), CustomBitSet.valueOf(longArrayOf()))
        // single all-zero word
        assertEquals(CustomBitSet(), CustomBitSet.valueOf(longArrayOf(0L)))
        // multiple trailing zeros are stripped
        assertEquals(denseOf(0), CustomBitSet.valueOf(longArrayOf(1L, 0L, 0L)))
        assertEquals(1, CustomBitSet.valueOf(longArrayOf(1L, 0L, 0L)).size)
    }

    @Test
    fun valueOfSharesCallerArray() {
        // valueOf transfers ownership of the array to the bitset (no defensive copy).
        // Caller-visible mutations propagate to the bitset.
        val data = longArrayOf(1L)
        val bs = CustomBitSet.valueOf(data)
        assertTrue(bs[0])
        data[0] = 0L
        assertFalse(bs[0], "valueOf must share storage with caller")
    }

    // ---- No-spurious-allocation guarantees ----------------------------------
    // These tests verify that operations don't allocate a data array proportional
    // to a far-out bit index in `another` — checked via [CustomBitSet.dataCapacity].

    @Test
    fun clearOutOfRangeBitDoesNotGrow() {
        val bs = denseOf(0)
        val capacityBefore = bs.dataCapacity
        bs.clear(1_000_000)   // huge index, never set
        assertEquals(1, bs.size)
        assertEquals(capacityBefore, bs.dataCapacity, "data array grew on out-of-range clear")
        assertTrue(bs[0])
    }

    @Test
    fun andWithLargerOperandSharedBits() {
        // and result is just the shared bits, regardless of operand sizes.
        // Capacity may *shrink* here: when `this` is dense and `another` is sparse,
        // and() switches `this` to lazy mode and resets `data` to the shared EMPTY
        // array, so we only assert "did not grow".
        forEachMode(intArrayOf(0), intArrayOf(0, 1_000)) { (a, b) ->
            val capacityBefore = a.dataCapacity
            a.and(b)
            assertEquals(listOf(0), a.toBitList())
            assertEquals(1, a.size)
            assertTrue(a.dataCapacity <= capacityBefore, "data array grew from $capacityBefore to ${a.dataCapacity}")
        }
    }

    @Test
    fun andWithLargerDisjointOperand() {
        // See comment in andWithLargerOperandSharedBits — capacity may shrink.
        forEachMode(intArrayOf(0), intArrayOf(1_000)) { (a, b) ->
            val capacityBefore = a.dataCapacity
            a.and(b)
            assertTrue(a.isEmpty)
            assertEquals(0, a.size)
            assertTrue(a.dataCapacity <= capacityBefore, "data array grew from $capacityBefore to ${a.dataCapacity}")
        }
    }

    @Test
    fun andNotWithLargerSharedOperand() {
        // andNot removes the shared bits; far-out bits in b are ignored without growth.
        // andNot never switches modes or resets `data`, so capacity is invariant.
        forEachMode(intArrayOf(0), intArrayOf(0, 1_000)) { (a, b) ->
            val capacityBefore = a.dataCapacity
            a.andNot(b)
            assertTrue(a.isEmpty)
            assertEquals(0, a.size)
            assertEquals(capacityBefore, a.dataCapacity, "data array capacity changed")
        }
    }

    @Test
    fun andNotWithLargerDisjointOperand() {
        forEachMode(intArrayOf(0), intArrayOf(1_000)) { (a, b) ->
            val capacityBefore = a.dataCapacity
            a.andNot(b)
            assertEquals(listOf(0), a.toBitList())
            assertEquals(1, a.size)
            assertEquals(capacityBefore, a.dataCapacity, "data array capacity changed")
        }
    }

    // ---- Full-word (-1L) bit iteration --------------------------------------
    // forEachBit uses `t = d and -d; d -= t`. For bit 63, -Long.MIN_VALUE
    // overflows back to Long.MIN_VALUE in two's complement, so the algorithm
    // must still isolate and clear the bit correctly.

    @Test
    fun fullWordCardinalityIs64() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L))
        assertEquals(64, bs.cardinality())
    }

    @Test
    fun fullWordForEachBitVisitsAllBits() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L))
        val visited = mutableListOf<Int>()
        bs.forEachBit { visited.add(it) }
        assertEquals((0 until 64).toList(), visited.sorted())
    }

    @Test
    fun fullWordGetBit63() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L))
        assertTrue(bs[63])
        assertTrue(bs[0])
    }

    @Test
    fun bit63IteratesCorrectly() {
        // 1L shl 63 == Long.MIN_VALUE; in dense forEachBit, -Long.MIN_VALUE overflows
        // to Long.MIN_VALUE so the bit-isolation arithmetic must still terminate.
        forEachMode(intArrayOf(63)) { (bs) ->
            assertEquals(1, bs.cardinality())
            val bits = mutableListOf<Int>()
            bs.forEachBit { bits.add(it) }
            assertEquals(listOf(63), bits)
        }
    }

    @Test
    fun twoFullWordsCardinality() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L, -1L))
        assertEquals(128, bs.cardinality())
        assertEquals((0 until 128).toList(), bs.toBitList())
    }

    @Test
    fun fullWordEqualsSetBitByBit() {
        val fromValOf = CustomBitSet.valueOf(longArrayOf(-1L))
        val bySet = CustomBitSet(1)
        for (i in 0 until 64) bySet.set(i)
        assertEquals(fromValOf, bySet)
        assertEquals(fromValOf.hashCode(), bySet.hashCode())
        assertEquals(fromValOf.hashCodeLong(), bySet.hashCodeLong())
    }

    @Test
    fun fullWordOrAndAndNot() {
        val full = CustomBitSet.valueOf(longArrayOf(-1L))
        val single = denseOf(0)

        val orResult = full.copy().also { it.or(single) }
        assertEquals(full, orResult)

        val andResult = full.copy().also { it.and(single) }
        assertEquals(single, andResult)

        val andNotResult = full.copy().also { it.andNot(single) }
        val expected = CustomBitSet(1)
        for (i in 1 until 64) expected.set(i)
        assertEquals(expected, andNotResult)
        assertEquals(63, andNotResult.cardinality())
    }

    // ---- Helpers ------------------------------------------------------------

    private fun sparseOf(vararg bits: Int): CustomBitSet {
        require(bits.size < LAZY_CONVERSION_THRESHOLD) { "Too many bits for sparse helper; use denseOf instead" }
        val bs = CustomBitSet()
        bits.forEach { bs.set(it) }
        assertSparse(bs)
        return bs
    }

    private fun denseOf(vararg bits: Int): CustomBitSet {
        val bs = CustomBitSet(1)
        bits.forEach { bs.set(it) }
        assertDense(bs)
        return bs
    }

    private fun assertSparse(bs: CustomBitSet, message: String? = null) {
        assertTrue(bs.isLazy, message ?: "bitset is dense, expected sparse (lazy)")
    }

    private fun assertDense(bs: CustomBitSet, message: String? = null) {
        assertFalse(bs.isLazy, message ?: "bitset is sparse (lazy), expected dense")
    }

    private fun CustomBitSet.toBitList(): List<Int> {
        val result = mutableListOf<Int>()
        forEachBit { result.add(it) }
        // Free invariant check on every test that uses toBitList():
        // forEachBit must visit exactly cardinality() bits.
        assertEquals(cardinality(), result.size, "forEachBit and cardinality() disagree")
        return result.sorted()
    }

    /**
     * Run [block] with all sparse/dense combinations of bitsets built from [bitsArrays].
     * Each operand is constructed independently in each of the two representations, so 2^N
     * combinations are exercised. Sparse mode is skipped when the bit count is at or above
     * the threshold. The lambda receives a [List] of bitsets that callers typically
     * destructure with `(a) ->`, `(a, b) ->`, or `(a, b, c) ->`.
     */
    private fun forEachMode(
            vararg bitsArrays: IntArray,
            block: (List<CustomBitSet>) -> Unit,
    ) {
        val modeNames = listOf("sparse", "dense")
        fun enumerate(index: Int, modes: List<String>) {
            if (index == bitsArrays.size) {
                // Build fresh bitsets at the leaf so mutations in one branch don't leak.
                val bitsets = modes.mapIndexed { i, mode ->
                    if (mode == "sparse") sparseOf(*bitsArrays[i]) else denseOf(*bitsArrays[i])
                }
                try {
                    block(bitsets)
                } catch (e: AssertionError) {
                    val ctx = bitsArrays.indices.joinToString(", ") { i ->
                        "${modes[i]}${bitsArrays[i].toList()}"
                    }
                    throw AssertionError("[$ctx] ${e.message}", e)
                }
                return
            }
            val bits = bitsArrays[index]
            for (mode in modeNames) {
                if (mode == "sparse" && bits.size >= LAZY_CONVERSION_THRESHOLD) continue
                enumerate(index + 1, modes + mode)
            }
        }
        enumerate(0, emptyList())
    }
}