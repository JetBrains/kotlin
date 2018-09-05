/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

import kotlin.experimental.*

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public inline class ULong internal constructor(private val data: Long) : Comparable<ULong> {

    companion object {
        /**
         * A constant holding the minimum value an instance of ULong can have.
         */
        public const val MIN_VALUE: ULong = ULong(0)

        /**
         * A constant holding the maximum value an instance of ULong can have.
         */
        public const val MAX_VALUE: ULong = ULong(-1)

        /**
         * The number of bytes used to represent an instance of ULong in a binary form.
         */
        public const val SIZE_BYTES: Int = 8

        /**
         * The number of bits used to represent an instance of ULong in a binary form.
         */
        public const val SIZE_BITS: Int = 64
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UByte): Int = this.compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UShort): Int = this.compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UInt): Int = this.compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: ULong): Int = ulongCompare(this.data, other.data)

    /** Adds the other value to this value. */
    public operator fun plus(other: UByte): ULong = this.plus(other.toULong())
    /** Adds the other value to this value. */
    public operator fun plus(other: UShort): ULong = this.plus(other.toULong())
    /** Adds the other value to this value. */
    public operator fun plus(other: UInt): ULong = this.plus(other.toULong())
    /** Adds the other value to this value. */
    public operator fun plus(other: ULong): ULong = ULong(this.data.plus(other.data))

    /** Subtracts the other value from this value. */
    public operator fun minus(other: UByte): ULong = this.minus(other.toULong())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UShort): ULong = this.minus(other.toULong())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UInt): ULong = this.minus(other.toULong())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: ULong): ULong = ULong(this.data.minus(other.data))

    /** Multiplies this value by the other value. */
    public operator fun times(other: UByte): ULong = this.times(other.toULong())
    /** Multiplies this value by the other value. */
    public operator fun times(other: UShort): ULong = this.times(other.toULong())
    /** Multiplies this value by the other value. */
    public operator fun times(other: UInt): ULong = this.times(other.toULong())
    /** Multiplies this value by the other value. */
    public operator fun times(other: ULong): ULong = ULong(this.data.times(other.data))

    /** Divides this value by the other value. */
    public operator fun div(other: UByte): ULong = this.div(other.toULong())
    /** Divides this value by the other value. */
    public operator fun div(other: UShort): ULong = this.div(other.toULong())
    /** Divides this value by the other value. */
    public operator fun div(other: UInt): ULong = this.div(other.toULong())
    /** Divides this value by the other value. */
    public operator fun div(other: ULong): ULong = ulongDivide(this, other)

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UByte): ULong = this.rem(other.toULong())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UShort): ULong = this.rem(other.toULong())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UInt): ULong = this.rem(other.toULong())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: ULong): ULong = ulongRemainder(this, other)

    /** Increments this value. */
    public operator fun inc(): ULong = ULong(data.inc())
    /** Decrements this value. */
    public operator fun dec(): ULong = ULong(data.dec())

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: ULong): ULongRange = ULongRange(this, other)

    /** Shifts this value left by the [bitCount] number of bits. */
    public infix fun shl(bitCount: Int): ULong = ULong(data shl bitCount)
    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros. */
    public infix fun shr(bitCount: Int): ULong = ULong(data ushr bitCount)
    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: ULong): ULong = ULong(this.data and other.data)
    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: ULong): ULong = ULong(this.data or other.data)
    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: ULong): ULong = ULong(this.data xor other.data)
    /** Inverts the bits in this value. */
    public fun inv(): ULong = ULong(data.inv())

    public fun toByte(): Byte = data.toByte()
    public fun toShort(): Short = data.toShort()
    public fun toInt(): Int = data.toInt()
    public fun toLong(): Long = data

    public fun toUByte(): UByte = data.toUByte()
    public fun toUShort(): UShort = data.toUShort()
    public fun toUInt(): UInt = data.toUInt()
    public fun toULong(): ULong = this

    public override fun toString(): String = ulongToString(data)

}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Byte.toULong(): ULong = ULong(this.toLong())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Short.toULong(): ULong = ULong(this.toLong())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Int.toULong(): ULong = ULong(this.toLong())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Long.toULong(): ULong = ULong(this)
