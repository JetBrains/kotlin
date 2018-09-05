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
public inline class UInt internal constructor(private val data: Int) : Comparable<UInt> {

    companion object {
        /**
         * A constant holding the minimum value an instance of UInt can have.
         */
        public const val MIN_VALUE: UInt = UInt(0)

        /**
         * A constant holding the maximum value an instance of UInt can have.
         */
        public const val MAX_VALUE: UInt = UInt(-1)

        /**
         * The number of bytes used to represent an instance of UInt in a binary form.
         */
        public const val SIZE_BYTES: Int = 4

        /**
         * The number of bits used to represent an instance of UInt in a binary form.
         */
        public const val SIZE_BITS: Int = 32
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UByte): Int = this.compareTo(other.toUInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UShort): Int = this.compareTo(other.toUInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: UInt): Int = uintCompare(this.data, other.data)

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)

    /** Adds the other value to this value. */
    public operator fun plus(other: UByte): UInt = this.plus(other.toUInt())
    /** Adds the other value to this value. */
    public operator fun plus(other: UShort): UInt = this.plus(other.toUInt())
    /** Adds the other value to this value. */
    public operator fun plus(other: UInt): UInt = UInt(this.data.plus(other.data))
    /** Adds the other value to this value. */
    public operator fun plus(other: ULong): ULong = this.toULong().plus(other)

    /** Subtracts the other value from this value. */
    public operator fun minus(other: UByte): UInt = this.minus(other.toUInt())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UShort): UInt = this.minus(other.toUInt())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UInt): UInt = UInt(this.data.minus(other.data))
    /** Subtracts the other value from this value. */
    public operator fun minus(other: ULong): ULong = this.toULong().minus(other)

    /** Multiplies this value by the other value. */
    public operator fun times(other: UByte): UInt = this.times(other.toUInt())
    /** Multiplies this value by the other value. */
    public operator fun times(other: UShort): UInt = this.times(other.toUInt())
    /** Multiplies this value by the other value. */
    public operator fun times(other: UInt): UInt = UInt(this.data.times(other.data))
    /** Multiplies this value by the other value. */
    public operator fun times(other: ULong): ULong = this.toULong().times(other)

    /** Divides this value by the other value. */
    public operator fun div(other: UByte): UInt = this.div(other.toUInt())
    /** Divides this value by the other value. */
    public operator fun div(other: UShort): UInt = this.div(other.toUInt())
    /** Divides this value by the other value. */
    public operator fun div(other: UInt): UInt = uintDivide(this, other)
    /** Divides this value by the other value. */
    public operator fun div(other: ULong): ULong = this.toULong().div(other)

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UByte): UInt = this.rem(other.toUInt())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UShort): UInt = this.rem(other.toUInt())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UInt): UInt = uintRemainder(this, other)
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: ULong): ULong = this.toULong().rem(other)

    /** Increments this value. */
    public operator fun inc(): UInt = UInt(data.inc())
    /** Decrements this value. */
    public operator fun dec(): UInt = UInt(data.dec())

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: UInt): UIntRange = UIntRange(this, other)

    /** Shifts this value left by the [bitCount] number of bits. */
    public infix fun shl(bitCount: Int): UInt = UInt(data shl bitCount)
    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros. */
    public infix fun shr(bitCount: Int): UInt = UInt(data ushr bitCount)
    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: UInt): UInt = UInt(this.data and other.data)
    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: UInt): UInt = UInt(this.data or other.data)
    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: UInt): UInt = UInt(this.data xor other.data)
    /** Inverts the bits in this value. */
    public fun inv(): UInt = UInt(data.inv())

    public fun toByte(): Byte = data.toByte()
    public fun toShort(): Short = data.toShort()
    public fun toInt(): Int = data
    public fun toLong(): Long = data.toLong() and 0xFFFF_FFFF

    public fun toUByte(): UByte = data.toUByte()
    public fun toUShort(): UShort = data.toUShort()
    public fun toUInt(): UInt = this
    public fun toULong(): ULong = data.toULong() and 0xFFFF_FFFFu

    public override fun toString(): String = toLong().toString()

}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Byte.toUInt(): UInt = UInt(this.toInt())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Short.toUInt(): UInt = UInt(this.toInt())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Int.toUInt(): UInt = UInt(this)
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Long.toUInt(): UInt = UInt(this.toInt())
