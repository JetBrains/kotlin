/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native

/**
 * A vector of bits growing if necessary and allowing one to set/clear/read bits from it by a bit index.
 *
 * @constructor creates an empty bit set with the specified [size]
 * @param size the size of one element in the array used to store bits.
 */
@ObsoleteNativeApi
public class BitSet(size: Int = ELEMENT_SIZE) {

    @kotlin.native.internal.CanBePrecreated
    companion object {
        // Default size of one element in the array used to store bits.
        private const val ELEMENT_SIZE = 64
        private const val MAX_BIT_OFFSET = ELEMENT_SIZE - 1
        private const val ALL_TRUE = -1L // 0xFFFF_FFFF_FFFF_FFFF
        private const val ALL_FALSE = 0L // 0x0000_0000_0000_0000
    }

    private var bits: LongArray = LongArray(bitToElementSize(size))

    private val lastIndex: Int
        get() = size - 1

    /** Returns an index of the last bit that has `true` value. Returns -1 if the set is empty. */
    val lastTrueIndex: Int
        get() = previousSetBit(size)

    /** True if this BitSet contains no bits set to true. */
    val isEmpty: Boolean
        get() = bits.all { it == ALL_FALSE }

    /** Actual number of bits available in the set. All bits with indices >= size assumed to be 0 */
    var size: Int = size
        private set

    /**
     * Creates a bit set of given [length] filling elements using [initializer]
     */
    constructor(length: Int, initializer: (Int) -> Boolean): this(length) {
        for (i in 0 until length) {
            set(i, initializer(i))
        }
    }

    // Transforms a bit index into an element index in the `bits` array.
    private val Int.elementIndex: Int
        get() = this / ELEMENT_SIZE

    // Transforms a bit index in the set into a bit in the element of the `bits` array.
    private val Int.bitOffset: Int
        get() = this % ELEMENT_SIZE

    // Transforms a bit index in the set into pair of a `bits` element index and a bit index in the element.
    private val Int.asBitCoordinates: Pair<Int, Int>
        get() = Pair(elementIndex, bitOffset)

    // Transforms a bit offset to the mask with only bit set corresponding to the offset.
    private val Int.asMask: Long
        get() = 0x1L shl this

    // Transforms a bit offset to the mask with only bits before the index (inclusive) set.
    private val Int.asMaskBefore: Long
        get() = getMaskBetween(0, this)

    // Transforms a bit offset to the mask with only bits after the index (inclusive) set.
    private val Int.asMaskAfter: Long
        get() = getMaskBetween(this, MAX_BIT_OFFSET)

    // Builds a masks with 1 between fromOffset and toOffset (both inclusive).
    private fun getMaskBetween(fromOffset: Int, toOffset: Int): Long {
        var res = 0L
        val maskToAdd = fromOffset.asMask
        for (i in fromOffset..toOffset) {
            res = (res shl 1) or maskToAdd
        }
        return res
    }

    // Transforms a size in bits to a size in elements of the `bits` array.
    private fun bitToElementSize(bitSize: Int): Int = (bitSize + ELEMENT_SIZE - 1) / ELEMENT_SIZE

    // Transforms a pair of an element index and a bit offset to a bit index.
    private fun bitIndex(elementIndex: Int, bitOffset: Int) =
            elementIndex * ELEMENT_SIZE + bitOffset

    // Sets all bits after the last available bit (size - 1) to 0.
    private fun clearUnusedTail() {
        val (lastElementIndex, lastBitOffset) = lastIndex.asBitCoordinates
        bits[bits.lastIndex] = bits[bits.lastIndex] and lastBitOffset.asMaskBefore
        for (i in lastElementIndex + 1 until bits.size) {
            bits[i] = ALL_FALSE
        }
    }

    // Internal function. Sets bits specified by the element index and the given mask to value.
    private fun setBitsWithMask(elementIndex: Int, mask: Long, value: Boolean) {
        val element = bits[elementIndex]
        if (value) {
            bits[elementIndex] = element or mask
        } else {
            bits[elementIndex] = element and mask.inv()
        }
    }

    // Internal function. Flips bits specified by the element index and the given mask.
    private fun flipBitsWithMask(elementIndex: Int, mask: Long) {
        val element = bits[elementIndex]
        bits[elementIndex] = element xor mask
    }

    /**
     * Checks if index is valid and extends the `bits` array if the index exceeds its size.
     * @throws [IndexOutOfBoundsException] if [index] < 0.
     */
    private fun ensureCapacity(index: Int) {
        if (index < 0) {
            throw IndexOutOfBoundsException()
        }
        if (index >= size) {
            size = index + 1
            if (index.elementIndex >= bits.size) {
                // Create a new array containing the index-th bit.
                bits = bits.copyOf(bitToElementSize(index + 1))
            }
            // Set all bits after the index to 0. TODO: We can remove it.
            clearUnusedTail()
        }
    }

    /** Set the bit specified to the specified value. */
    fun set(index: Int, value: Boolean = true) {
        ensureCapacity(index)
        val (elementIndex, offset) = index.asBitCoordinates
        setBitsWithMask(elementIndex, offset.asMask, value)
    }

    /** Sets the bits with indices between [from] (inclusive) and [to] (exclusive) to the specified value. */
    fun set(from : Int, to: Int, value: Boolean = true) = set(from until to, value)

    /** Sets the bits from the range specified to the specified value. */
    fun set(range: IntRange, value: Boolean = true) {
        if (range.start < 0 || range.endInclusive < 0) {
            throw IndexOutOfBoundsException()
        }
        if (range.start > range.endInclusive) { // Empty range.
            return
        }
        ensureCapacity(range.endInclusive)
        val (fromIndex, fromOffset) = range.start.asBitCoordinates
        val (toIndex, toOffset) = range.endInclusive.asBitCoordinates
        if (toIndex == fromIndex) {
            val mask = getMaskBetween(fromOffset, toOffset)
            setBitsWithMask(fromIndex, mask, value)
        } else {
            // Set bits in the first element.
            setBitsWithMask(fromIndex, fromOffset.asMaskAfter, value)
            // Set all bits of all elements (excluding border ones) to 0 or 1 depending.
            for (index in fromIndex + 1 until toIndex) {
                bits[index] = if (value) ALL_TRUE else ALL_FALSE
            }
            // Set bits in the last element
            setBitsWithMask(toIndex, toOffset.asMaskBefore, value)
        }
    }


    /** Clears the bit specified */
    fun clear(index: Int) = set(index, false)
    /** Clears the bits with indices between [from] (inclusive) and [to] (exclusive) to the specified value. */
    fun clear(from : Int, to: Int) = set(from, to, false)
    /** Clears the bit specified */
    fun clear(range: IntRange) = set(range, false)

    /** Sets all bits in the BitSet to `false`. */
    fun clear() {
        for (i in bits.indices) {
            bits[i] = ALL_FALSE
        }
    }

    /** Reverses the bit specified. */
    fun flip(index: Int) {
        ensureCapacity(index)
        val (elementIndex, offset) = index.asBitCoordinates
        flipBitsWithMask(elementIndex, offset.asMask)
    }

    /** Reverses the bits with indices between [from] (inclusive) and [to] (exclusive). */
    fun flip(from: Int, to: Int) = flip(from until to)

    /** Reverses the bits from the range specified. */
    fun flip(range: IntRange) {
        if (range.start < 0 || range.endInclusive < 0) {
            throw IndexOutOfBoundsException()
        }
        if (range.start > range.endInclusive) { // Empty range.
            return
        }
        ensureCapacity(range.endInclusive)
        val (fromIndex, fromOffset) = range.start.asBitCoordinates
        val (toIndex, toOffset) = range.endInclusive.asBitCoordinates
        if (toIndex == fromIndex) {
            val mask = getMaskBetween(fromOffset, toOffset)
            flipBitsWithMask(fromIndex, mask)
        } else {
            // Flip bits in the first element.
            flipBitsWithMask(toIndex, toOffset.asMaskAfter)
            // Flip bits between the first and the last elements.
            for (index in fromIndex + 1 until toIndex) {
                bits[index] = bits[index].inv()
            }
            // Flip bits in the last element.
            flipBitsWithMask(toIndex, toOffset.asMaskBefore)
        }
    }

    /**
     * Returns an index of a next set (if [lookFor] == true) or clear
     * (if [lookFor] == false) bit after [startIndex] (inclusive).
     * Returns -1 (for [lookFor] == true) or [size] (for lookFor == false)
     * if there is no such bits between [startIndex] and [size] - 1.
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    private fun nextBit(startIndex: Int, lookFor: Boolean): Int {
        if (startIndex < 0) {
            throw IndexOutOfBoundsException()
        }
        if (startIndex >= size) {
            return if (lookFor) -1 else startIndex
        }
        val (startElementIndex, startOffset) = startIndex.asBitCoordinates
        // Look for the next set bit in the first element.
        var element = bits[startElementIndex]
        for (offset in startOffset..MAX_BIT_OFFSET) {
            val bit = element and (0x1L shl offset) != 0L
            if (bit == lookFor) {  // Look for not 0 if we need a set bit and look for 0 otherwise.
                return bitIndex(startElementIndex, offset)
            }
        }
        // Look for in the remaining elements.
        for (index in startElementIndex + 1..bits.lastIndex) {
            element = bits[index]
            for (offset in 0..MAX_BIT_OFFSET) {
                val bit = element and (0x1L shl offset) != 0L
                if (bit == lookFor) { // Look for not 0 if we need a set bit and look for 0 otherwise.
                    return bitIndex(index, offset)
                }
            }
        }
        return if (lookFor) -1 else size
    }

    /**
     * Returns an index of a next bit which value is `true` after [startIndex] (inclusive).
     * Returns -1 if there is no such bits after [startIndex].
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    fun nextSetBit(startIndex: Int = 0): Int = nextBit(startIndex, true)

    /**
     * Returns an index of a next bit which value is `false` after [startIndex] (inclusive).
     * Returns [size] if there is no such bits between [startIndex] and [size] - 1 assuming that the set has an infinite
     * sequence of `false` bits after (size - 1)-th.
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    fun nextClearBit(startIndex: Int = 0): Int = nextBit(startIndex, false)

    /**
     * Returns the biggest index of a bit which value is [lookFor] before [startIndex] (inclusive).
     * Returns -1 if there is no such bits before [startIndex].
     * If [startIndex] >= [size] returns -1
     */
    fun previousBit(startIndex: Int, lookFor: Boolean): Int {
        var correctStartIndex = startIndex
        if (startIndex >= size) {
            // We assume that all bits after `size - 1` are 0. So we can return the start index if we are looking for 0.
            if (!lookFor) {
                return startIndex
            } else {
                // If we are looking for 1 we can skip all these 0 after `size - 1`.
                correctStartIndex = size - 1
            }
        }
        if (correctStartIndex < -1) {
            throw IndexOutOfBoundsException()
        }
        if (correctStartIndex == -1) {
            return -1
        }

        val (startElementIndex, startOffset) = correctStartIndex.asBitCoordinates
        // Look for the next set bit in the first element.
        var element = bits[startElementIndex]
        for (offset in startOffset downTo 0) {
            val bit = element and (0x1L shl offset) != 0L
            if (bit == lookFor) {  // Look for not 0 if we need a set bit and look for 0 otherwise.
                return bitIndex(startElementIndex, offset)
            }
        }
        // Look for in the remaining elements.
        for (index in startElementIndex - 1 downTo 0) {
            element = bits[index]
            for (offset in MAX_BIT_OFFSET downTo 0) {
                val bit = element and (0x1L shl offset) != 0L
                if (bit == lookFor) {  // Look for not 0 if we need a set bit and look for 0 otherwise.
                    return bitIndex(index, offset)
                }
            }
        }
        return -1
    }

    /**
     * Returns the biggest index of a bit which value is `true` before [startIndex] (inclusive).
     * Returns -1 if there is no such bits before [startIndex] or if [startIndex] == -1.
     * If [startIndex] >= size will search from (size - 1)-th bit.
     * @throws IndexOutOfBoundException if [startIndex] < -1.
     */
    fun previousSetBit(startIndex: Int): Int = previousBit(startIndex, true)

    /**
     * Returns the biggest index of a bit which value is `false` before [startIndex] (inclusive).
     * Returns -1 if there is no such bits before [startIndex] or if [startIndex] == -1.
     * If [startIndex] >= size will return [startIndex] assuming that the set has an infinite
     * sequence of `false` bits after (size - 1)-th.
     * @throws IndexOutOfBoundException if [startIndex] < -1.
     */
    fun previousClearBit(startIndex: Int): Int = previousBit(startIndex, false)

    /** Returns a value of a bit with the [index] specified. */
    operator fun get(index: Int): Boolean {
        if (index < 0) {
            throw IndexOutOfBoundsException()
        }
        if (index >= size) {
            return false
        }
        val (elementIndex, offset) = index.asBitCoordinates
        return bits[elementIndex] and offset.asMask != 0L
    }

    private inline fun doOperation(another: BitSet, operation: Long.(Long) -> Long) {
        ensureCapacity(another.lastIndex)
        var index = 0
        while (index < another.bits.size) {
            bits[index] = operation(bits[index], another.bits[index])
            index++
        }
        while (index < bits.size) {
            bits[index] = operation(bits[index], ALL_FALSE)
            index++
        }
    }

    /** Performs a logical and operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun and(another: BitSet) = doOperation(another, Long::and)

    /** Performs a logical or operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun or(another: BitSet) = doOperation(another, Long::or)

    /** Performs a logical xor operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun xor(another: BitSet) = doOperation(another, Long::xor)

    /** Performs a logical and + not operations over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun andNot(another: BitSet) {
        ensureCapacity(another.lastIndex)
        var index = 0
        while (index < another.bits.size) {
            bits[index] = bits[index] and another.bits[index].inv()
            index++
        }
        while (index < bits.size) {
            bits[index] = bits[index] and ALL_TRUE
            index++
        }
    }

    /** Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet. */
    fun intersects(another: BitSet): Boolean =
            (0 until minOf(bits.size, another.bits.size)).any { bits[it] and another.bits[it] != 0L }

    override fun toString(): String {
        val sb = StringBuilder()
        var first = true
        sb.append('[')
        var index = nextSetBit(0)
        while (index != -1) {
            if (!first) {
                sb.append('|')
            } else {
                first = false
            }
            sb.append(index)
            index = nextSetBit(index + 1)
        }
        sb.append(']')
        return sb.toString()
    }

    override fun hashCode(): Int {
        var x: Long = 1234
        for (i in 0..bits.lastIndex) {
            x = x xor bits[i] * (i + 1)
        }
        return (x shr 32 xor x).toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BitSet) {
            return false
        }
        var index = 0
        while (index < minOf(bits.size, other.bits.size)) {
            if (bits[index] != other.bits[index]) {
                return false
            }
            index++
        }
        val longestBits = if (bits.size > other.bits.size) bits else other.bits
        while (index < longestBits.size) {
            if (longestBits[index] != ALL_FALSE) {
                return false
            }
            index++
        }
        return true
    }
}
