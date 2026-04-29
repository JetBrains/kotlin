/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import kotlin.test.*

class CustomBitSetTest {

    private fun sparseOf(vararg bits: Int): CustomBitSet {
        require(bits.size < LAZY_CONVERSION_THRESHOLD) { "Too many bits for sparse helper; use denseOf instead" }
        val bs = CustomBitSet()
        bits.forEach { bs.set(it) }
        return bs
    }

    private fun denseOf(vararg bits: Int): CustomBitSet {
        val bs = CustomBitSet(1)
        bits.forEach { bs.set(it) }
        return bs
    }

    private fun CustomBitSet.toBitList(): List<Int> {
        val result = mutableListOf<Int>()
        forEachBit { result.add(it) }
        return result.sorted()
    }

    // ---- Construction -------------------------------------------------------

    @Test fun emptyDefaultConstructorIsEmpty() {
        val bs = CustomBitSet()
        assertTrue(bs.isEmpty)
        assertEquals(0, bs.cardinality())
    }

    @Test fun emptyDenseConstructorIsEmpty() {
        val bs = CustomBitSet(100)
        assertTrue(bs.isEmpty)
        assertEquals(0, bs.cardinality())
    }

    @Test fun valueOfProducesCorrectBits() {
        val bs = CustomBitSet.valueOf(longArrayOf(0b101L))
        assertTrue(bs[0])
        assertFalse(bs[1])
        assertTrue(bs[2])
        assertEquals(2, bs.cardinality())
    }

    @Test fun allEmptyConstructorVariantsAreEqual() {
        val sparse = CustomBitSet()
        val dense = CustomBitSet(64)
        val fromEmpty = CustomBitSet.valueOf(longArrayOf())
        assertEquals(sparse, dense)
        assertEquals(sparse, fromEmpty)
        assertEquals(dense, fromEmpty)
        assertEquals(sparse.hashCode(), dense.hashCode())
    }

    // ---- Sparse mode (< LAZY_CONVERSION_THRESHOLD bits) ---------------------

    @Test fun sparseModeSetAndGet() {
        val bs = CustomBitSet()
        bs.set(0)
        bs.set(63)
        assertTrue(bs[0])
        assertTrue(bs[63])
        assertFalse(bs[1])
        assertFalse(bs[64])
        assertEquals(2, bs.cardinality())
    }

    @Test fun sparseModeSetAndClear() {
        val bs = CustomBitSet()
        bs.set(5)
        assertTrue(bs[5])
        bs.clear(5)
        assertFalse(bs[5])
        assertTrue(bs.isEmpty)
        assertEquals(0, bs.cardinality())
    }

    @Test fun sparseModeBitAcrossWordBoundary() {
        val bs = CustomBitSet()
        bs.set(0)
        bs.set(64)   // second word, still sparse (2 < threshold)
        assertEquals(2, bs.cardinality())
        assertTrue(bs[0])
        assertTrue(bs[64])
    }

    @Test fun sparseModeForEachBit() {
        val bs = sparseOf(30, 5, 10)
        assertEquals(listOf(5, 10, 30), bs.toBitList())
    }

    @Test fun sparseModeIsNotEmpty() {
        val bs = sparseOf(7)
        assertFalse(bs.isEmpty)
    }

    @Test fun sparseModeCopyIsIndependent() {
        val bs = sparseOf(3, 42)
        val copy = bs.copy()
        assertEquals(bs, copy)
        copy.set(100)
        assertFalse(bs[100])
    }

    @Test fun sparseModeBooleanSetOperator() {
        val bs = CustomBitSet()
        bs[5] = true
        assertTrue(bs[5])
        bs[5] = false
        assertFalse(bs[5])
    }

    // ---- Transition at threshold (8th bit triggers dense conversion) --------

    @Test fun transitionAt8BitsAllBitsRetained() {
        val bs = CustomBitSet()
        for (i in 0 until 7) bs.set(i * 10)
        assertEquals(7, bs.cardinality())
        bs.set(70)   // 8th bit — triggers buildFromLazy
        assertEquals(8, bs.cardinality())
        for (i in 0 until 7) assertTrue(bs[i * 10], "bit ${i * 10} should survive transition")
        assertTrue(bs[70])
    }

    @Test fun hashCodeConsistentAcrossTransition() {
        val bits = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70)
        val converted = CustomBitSet()
        bits.forEach { converted.set(it) }   // 8th set triggers conversion

        val neverSparse = denseOf(*bits)

        assertEquals(converted, neverSparse)
        assertEquals(converted.hashCode(), neverSparse.hashCode())
        assertEquals(converted.hashCodeLong(), neverSparse.hashCodeLong())
    }

    @Test fun copyBeforeAndAfterTransitionAreCorrect() {
        val bs = CustomBitSet()
        for (i in 0 until 7) bs.set(i)
        val beforeTransition = bs.copy()

        bs.set(7)   // transition
        val afterTransition = bs.copy()

        assertEquals(7, beforeTransition.cardinality())
        for (i in 0 until 7) assertTrue(beforeTransition[i])

        assertEquals(bs, afterTransition)
        assertEquals(8, afterTransition.cardinality())
    }

    // ---- Dense mode basics --------------------------------------------------

    @Test fun denseModeSetAndGetAcrossWords() {
        val bs = CustomBitSet(200)
        for (bit in listOf(0, 63, 64, 127, 128, 191, 192)) {
            bs.set(bit)
            assertTrue(bs[bit], "bit $bit should be set")
        }
        assertEquals(7, bs.cardinality())
    }

    @Test fun denseModeForEachBitOrder() {
        val bs = denseOf(128, 0, 64)
        assertEquals(listOf(0, 64, 128), bs.toBitList())
    }

    @Test fun denseModeForEachWordVisitsSetWords() {
        val bs = denseOf(0, 64)   // words 0 and 1
        val words = mutableListOf<Long>()
        bs.forEachWord { words.add(it) }
        assertTrue(words.size >= 2)
        assertTrue(words[0] and 1L != 0L, "word 0 bit 0")
        assertTrue(words[1] and 1L != 0L, "word 1 bit 0")
    }

    @Test fun denseModeFullClearResetsAll() {
        val bs = denseOf(0, 64, 128)
        bs.clear()
        assertTrue(bs.isEmpty)
        assertEquals(0, bs.cardinality())
        assertEquals(0, bs.toBitList().size)
    }

    @Test fun denseClearBitUpdatesCardinality() {
        val bs = denseOf(0, 128)
        bs.clear(128)
        assertEquals(1, bs.cardinality())
        assertTrue(bs[0])
        assertFalse(bs[128])
    }

    // ---- Logical operations: same mode --------------------------------------

    @Test fun orSparseMergesBits() {
        val a = sparseOf(0, 10)
        val b = sparseOf(10, 20)
        a.or(b)
        assertEquals(listOf(0, 10, 20), a.toBitList())
    }

    @Test fun orDenseMergesBits() {
        val a = denseOf(0, 64)
        val b = denseOf(64, 128)
        a.or(b)
        assertEquals(listOf(0, 64, 128), a.toBitList())
    }

    @Test fun orDenseGrowsWhenNeeded() {
        val a = denseOf(0)
        val b = denseOf(0, 200)
        a.or(b)
        assertEquals(listOf(0, 200), a.toBitList())
    }

    @Test fun andSparseIntersects() {
        val a = sparseOf(0, 5, 10)
        val b = sparseOf(5, 10)
        a.and(b)
        assertEquals(listOf(5, 10), a.toBitList())
    }

    @Test fun andDenseIntersects() {
        val a = denseOf(0, 64, 128)
        val b = denseOf(64, 128, 192)
        a.and(b)
        assertEquals(listOf(64, 128), a.toBitList())
    }

    @Test fun andNotSparseRemovesBits() {
        val a = sparseOf(0, 5, 10)
        val b = sparseOf(5)
        a.andNot(b)
        assertEquals(listOf(0, 10), a.toBitList())
    }

    @Test fun andNotDenseRemovesBits() {
        val a = denseOf(0, 64, 128)
        val b = denseOf(64)
        a.andNot(b)
        assertEquals(listOf(0, 128), a.toBitList())
    }

    @Test fun intersectsSparse() {
        assertTrue(sparseOf(0, 10).intersects(sparseOf(10, 20)))
        assertFalse(sparseOf(0, 10).intersects(sparseOf(20, 30)))
    }

    @Test fun intersectsDense() {
        assertTrue(denseOf(0, 64).intersects(denseOf(64, 128)))
        assertFalse(denseOf(0, 64).intersects(denseOf(128, 192)))
    }

    @Test fun containsSparse() {
        assertTrue(sparseOf(0, 5, 10).contains(sparseOf(5, 10)))
        assertFalse(sparseOf(5, 10).contains(sparseOf(0, 5, 10)))
    }

    @Test fun containsDense() {
        assertTrue(denseOf(0, 64, 128).contains(denseOf(64, 128)))
        assertFalse(denseOf(64, 128).contains(denseOf(0, 64, 128)))
    }

    // ---- Logical operations: cross-mode (sparse ↔ dense) --------------------

    @Test fun denseOrSparse() {
        val a = denseOf(0, 64)
        val b = sparseOf(64, 128)
        a.or(b)
        assertEquals(listOf(0, 64, 128), a.toBitList())
    }

    @Test fun sparseOrDense() {
        val a = sparseOf(0, 10)
        val b = denseOf(10, 64)
        a.or(b)
        assertEquals(listOf(0, 10, 64), a.toBitList())
    }

    @Test fun denseAndSparse() {
        val a = denseOf(0, 64, 128)
        val b = sparseOf(0, 64)
        a.and(b)
        assertEquals(listOf(0, 64), a.toBitList())
    }

    @Test fun sparseAndDense() {
        val a = sparseOf(0, 5)
        val b = denseOf(5, 64)
        a.and(b)
        assertEquals(listOf(5), a.toBitList())
    }

    @Test fun denseAndNotSparse() {
        val a = denseOf(0, 64, 128)
        val b = sparseOf(64)
        a.andNot(b)
        assertEquals(listOf(0, 128), a.toBitList())
    }

    @Test fun sparseAndNotDense() {
        val a = sparseOf(0, 5, 10)
        val b = denseOf(5)
        a.andNot(b)
        assertEquals(listOf(0, 10), a.toBitList())
    }

    @Test fun denseIntersectsSparse() {
        assertTrue(denseOf(0, 64).intersects(sparseOf(64)))
        assertFalse(denseOf(0, 64).intersects(sparseOf(128)))
    }

    @Test fun sparseIntersectsDense() {
        assertTrue(sparseOf(0, 64).intersects(denseOf(64, 128)))
        assertFalse(sparseOf(0, 64).intersects(denseOf(128, 192)))
    }

    @Test fun denseContainsSparse() {
        assertTrue(denseOf(0, 64, 128).contains(sparseOf(0, 64)))
        assertFalse(denseOf(0, 64).contains(sparseOf(0, 64, 128)))
    }

    @Test fun sparseContainsDense() {
        assertTrue(sparseOf(0, 64).contains(denseOf(0, 64)))
        assertFalse(sparseOf(0, 64).contains(denseOf(0, 64, 128)))
    }

    // ---- orWithFilterHasChanged ---------------------------------------------

    @Test fun orWithFilterNoChangeReturnsFalse() {
        val a = denseOf(0, 64)
        val b = denseOf(0)   // subset of a
        assertFalse(a.orWithFilterHasChanged(b))
        assertEquals(listOf(0, 64), a.toBitList())
    }

    @Test fun orWithFilterChangeReturnsTrueAndSetsBit() {
        val a = denseOf(0)
        val b = denseOf(0, 64)
        assertTrue(a.orWithFilterHasChanged(b))
        assertEquals(listOf(0, 64), a.toBitList())
    }

    @Test fun orWithFilterSparseNoChange() {
        val a = sparseOf(0, 5)
        val b = sparseOf(5)
        assertFalse(a.orWithFilterHasChanged(b))
    }

    @Test fun orWithFilterSparseChange() {
        val a = sparseOf(0)
        val b = sparseOf(0, 5)
        assertTrue(a.orWithFilterHasChanged(b))
        assertTrue(a[5])
    }

    @Test fun orWithFilterArgOnlyAllowsFilteredBits() {
        val a = denseOf(0)
        val b = denseOf(0, 64, 128)
        val filter = denseOf(64)   // only bit 64 passes the filter
        assertTrue(a.orWithFilterHasChanged(b, filter))
        assertTrue(a[64])
        assertFalse(a[128])        // 128 is not in filter — must not be added
    }

    @Test fun orWithFilterArgNoChangeWhenFilteredBitsAlreadySet() {
        val a = denseOf(0, 64)
        val b = denseOf(0, 64, 128)
        val filter = denseOf(0, 64)   // 128 excluded by filter
        assertFalse(a.orWithFilterHasChanged(b, filter))
        assertFalse(a[128])
    }

    // ---- equals / hashCode contracts ----------------------------------------

    @Test fun equalsIsReflexive() {
        val a = sparseOf(1, 2, 3)
        assertEquals(a, a)
        assertEquals(a.hashCode(), a.hashCode())
    }

    @Test fun equalsIsSymmetric() {
        val a = sparseOf(1, 2, 3)
        val b = sparseOf(1, 2, 3)
        assertEquals(a, b)
        assertEquals(b, a)
    }

    @Test fun equalImpliesSameHashCode() {
        val a = sparseOf(0, 64, 128)
        val b = sparseOf(0, 64, 128)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test fun sparseDenseEqualWhenSameBits() {
        val bits = intArrayOf(0, 5, 10, 20)
        val sparse = sparseOf(*bits)
        val dense = denseOf(*bits)
        assertEquals(sparse, dense)
        assertEquals(sparse.hashCode(), dense.hashCode())
        assertEquals(sparse.hashCodeLong(), dense.hashCodeLong())
    }

    @Test fun sparseDenseEqualWithMultiWordBits() {
        val bits = intArrayOf(0, 64, 128)
        val sparse = sparseOf(*bits)
        val dense = denseOf(*bits)
        assertEquals(sparse, dense)
        assertEquals(sparse.hashCode(), dense.hashCode())
    }

    @Test fun hashCodeLongRelationToHashCode() {
        val bs = denseOf(0, 63, 64, 127)
        val h = bs.hashCodeLong()
        assertEquals(((h ushr 32) xor h).toInt(), bs.hashCode())
    }

    @Test fun differentBitsMeansNotEqual() {
        assertNotEquals(sparseOf(0, 1), sparseOf(0, 2))
        assertNotEquals(denseOf(0, 64), denseOf(0, 128))
    }

    // ---- Regression: size-tracking after logical operations -----------------

    @Test fun andDenseBothEqualityAfterSizeLeak() {
        // b.and(a) zeros out word 1 in b but does not shrink b.size
        // b should still equal a afterwards
        val a = denseOf(0)       // size=1
        val b = denseOf(0, 64)   // size=2
        b.and(a)
        assertEquals(listOf(0), b.toBitList())
        assertEquals(a, b)
    }

    @Test fun andDenseWithEmptyResultsInEmpty() {
        val a = denseOf(0, 64, 128)
        val empty = CustomBitSet(100)
        a.and(empty)
        assertTrue(a.isEmpty)
        assertEquals(0, a.cardinality())
        assertEquals(CustomBitSet(), a)
    }

    @Test fun andDensePlusSparseEqualityAfterSizeLeak() {
        // a.and(b) where b is sparse switches a to lazy but leaves a.size unchanged
        val a = denseOf(0, 64)   // size=2
        val b = sparseOf(0)      // size=1
        a.and(b)
        assertEquals(listOf(0), a.toBitList())
        assertEquals(b, a)
    }

    @Test fun andSparsePlusSparseEqualityAfterSizeLeak() {
        // retainAll in lazy path does not recompute size
        val a = sparseOf(0, 64)  // size=2
        val b = sparseOf(0)      // size=1
        a.and(b)
        assertEquals(listOf(0), a.toBitList())
        assertEquals(b, a)
    }

    @Test fun andNotSparseSizeLeakEquality() {
        // retainAll in andNot lazy path does not recompute size
        val a = sparseOf(0, 64)  // size=2; bit 64 in word 1
        val b = sparseOf(64)
        a.andNot(b)
        assertEquals(listOf(0), a.toBitList())
        assertEquals(sparseOf(0), a)
    }

    // ---- Edge cases ---------------------------------------------------------

    @Test fun orWithSelfIsIdempotent() {
        val a = denseOf(0, 64, 128)
        val expected = a.copy()
        a.or(a)
        assertEquals(expected, a)
    }

    @Test fun andWithSelfIsIdempotent() {
        val a = denseOf(0, 64, 128)
        val expected = a.copy()
        a.and(a)
        assertEquals(expected, a)
    }

    @Test fun andNotWithSelfIsEmpty() {
        val a = denseOf(0, 64, 128)
        a.andNot(a)
        assertTrue(a.isEmpty)
    }

    @Test fun anyBitsetContainsEmpty() {
        val a = denseOf(0, 64)
        assertTrue(a.contains(CustomBitSet()))
        assertTrue(a.contains(CustomBitSet(100)))
    }

    @Test fun emptyDoesNotContainNonEmpty() {
        val empty = CustomBitSet()
        assertFalse(empty.contains(sparseOf(0)))
    }

    @Test fun emptyBitsetOperations() {
        val a = CustomBitSet()
        val b = CustomBitSet()
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

    @Test fun wordBoundaryBitsSetAndGet() {
        for (bit in listOf(0, 63, 64, 127, 128)) {
            val bs = CustomBitSet()
            bs.set(bit)
            assertTrue(bs[bit], "bit $bit should be set")
            if (bit > 0) assertFalse(bs[bit - 1], "bit ${bit - 1} should not be set")
            assertEquals(1, bs.cardinality(), "cardinality for single bit $bit")
        }
    }

    // ---- Full-word (-1L) correctness ----------------------------------------
    // forEachBit uses `t = d and -d; d -= t`. For bit 63, -Long.MIN_VALUE
    // overflows back to Long.MIN_VALUE in two's complement, so the algorithm
    // must still isolate and clear the bit correctly.

    @Test fun fullWordCardinalityIs64() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L))
        assertEquals(64, bs.cardinality())
    }

    @Test fun fullWordForEachBitVisitsAllBits() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L))
        val visited = mutableListOf<Int>()
        bs.forEachBit { visited.add(it) }
        assertEquals((0 until 64).toList(), visited.sorted())
    }

    @Test fun fullWordGetBit63() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L))
        assertTrue(bs[63])
        assertTrue(bs[0])
    }

    @Test fun bit63OnlyWordIteratesCorrectly() {
        // 1L shl 63 == Long.MIN_VALUE; -Long.MIN_VALUE overflows to Long.MIN_VALUE
        val bs = CustomBitSet.valueOf(longArrayOf(1L shl 63))
        assertEquals(1, bs.cardinality())
        val bits = mutableListOf<Int>()
        bs.forEachBit { bits.add(it) }
        assertEquals(listOf(63), bits)
    }

    @Test fun twoFullWordsCardinality() {
        val bs = CustomBitSet.valueOf(longArrayOf(-1L, -1L))
        assertEquals(128, bs.cardinality())
        assertEquals((0 until 128).toList(), bs.toBitList())
    }

    @Test fun fullWordEqualsSetBitByBit() {
        val fromValOf = CustomBitSet.valueOf(longArrayOf(-1L))
        val bySet = CustomBitSet(1)
        for (i in 0 until 64) bySet.set(i)
        assertEquals(fromValOf, bySet)
        assertEquals(fromValOf.hashCode(), bySet.hashCode())
        assertEquals(fromValOf.hashCodeLong(), bySet.hashCodeLong())
    }

    @Test fun fullWordOrAndAndNot() {
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
}
