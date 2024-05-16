/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.native

@ObsoleteNativeApi
internal const val BIT_SET_ELEMENT_SIZE: Int = 64

/**
 * A vector of bits growing if necessary and allowing one to set/clear/read bits from it by a bit index.
 * (this is the stripped copy of K/N implementation for Regex)
 *
 * @constructor creates an empty bit set with the specified [size]
 * @param size the size of one element in the array used to store bits.
 */
@ObsoleteNativeApi
internal expect class BitSet constructor(size: Int = BIT_SET_ELEMENT_SIZE) {
    /** True if this BitSet contains no bits set to true. */
    val isEmpty: Boolean
    var size: Int
        private set
    /** Set the bit specified to the specified value. */
    fun set(index: Int, value: Boolean = true)

    /** Sets the bits with indices between [from] (inclusive) and [to] (exclusive) to the specified value. */
    fun set(from : Int, to: Int, value: Boolean = true)

    /** Sets the bits from the range specified to the specified value. */
    fun set(range: IntRange, value: Boolean = true)

    /**
     * Returns an index of a next bit which value is `true` after [startIndex] (inclusive).
     * Returns -1 if there is no such bits after [startIndex].
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    fun nextSetBit(startIndex: Int = 0): Int

    /**
     * Returns an index of a next bit which value is `false` after [startIndex] (inclusive).
     * Returns [size] if there is no such bits between [startIndex] and [size] - 1 assuming that the set has an infinite
     * sequence of `false` bits after (size - 1)-th.
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    fun nextClearBit(startIndex: Int = 0): Int

    /** Returns a value of a bit with the [index] specified. */
    operator fun get(index: Int): Boolean

    /** Performs a logical and operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun and(another: BitSet)

    /** Performs a logical or operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun or(another: BitSet)

    /** Performs a logical xor operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun xor(another: BitSet)

    /** Performs a logical and + not operations over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun andNot(another: BitSet)

    /** Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet. */
    fun intersects(another: BitSet): Boolean
}
