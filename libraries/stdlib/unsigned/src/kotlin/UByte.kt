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
public inline class UByte internal constructor(private val data: Byte) : Comparable<UByte> {

    companion object {
        /**
         * A constant holding the minimum value an instance of UByte can have.
         */
        public const val MIN_VALUE: UByte = UByte(0)

        /**
         * A constant holding the maximum value an instance of UByte can have.
         */
        public const val MAX_VALUE: UByte = UByte(-1)

        /**
         * The number of bytes used to represent an instance of UByte in a binary form.
         */
        public const val SIZE_BYTES: Int = 1

        /**
         * The number of bits used to represent an instance of UByte in a binary form.
         */
        public const val SIZE_BITS: Int = 8
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: UByte): Int = this.toUInt().compareTo(other.toUInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UShort): Int = this.toUInt().compareTo(other.toUInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UInt): Int = this.toUInt().compareTo(other)

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)

    /** Adds the other value to this value. */
    public operator fun plus(other: UByte): UInt = this.toUInt().plus(other.toUInt())
    /** Adds the other value to this value. */
    public operator fun plus(other: UShort): UInt = this.toUInt().plus(other.toUInt())
    /** Adds the other value to this value. */
    public operator fun plus(other: UInt): UInt = this.toUInt().plus(other)
    /** Adds the other value to this value. */
    public operator fun plus(other: ULong): ULong = this.toULong().plus(other)

    /** Subtracts the other value from this value. */
    public operator fun minus(other: UByte): UInt = this.toUInt().minus(other.toUInt())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UShort): UInt = this.toUInt().minus(other.toUInt())
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UInt): UInt = this.toUInt().minus(other)
    /** Subtracts the other value from this value. */
    public operator fun minus(other: ULong): ULong = this.toULong().minus(other)

    /** Multiplies this value by the other value. */
    public operator fun times(other: UByte): UInt = this.toUInt().times(other.toUInt())
    /** Multiplies this value by the other value. */
    public operator fun times(other: UShort): UInt = this.toUInt().times(other.toUInt())
    /** Multiplies this value by the other value. */
    public operator fun times(other: UInt): UInt = this.toUInt().times(other)
    /** Multiplies this value by the other value. */
    public operator fun times(other: ULong): ULong = this.toULong().times(other)

    /** Divides this value by the other value. */
    public operator fun div(other: UByte): UInt = this.toUInt().div(other.toUInt())
    /** Divides this value by the other value. */
    public operator fun div(other: UShort): UInt = this.toUInt().div(other.toUInt())
    /** Divides this value by the other value. */
    public operator fun div(other: UInt): UInt = this.toUInt().div(other)
    /** Divides this value by the other value. */
    public operator fun div(other: ULong): ULong = this.toULong().div(other)

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UByte): UInt = this.toUInt().rem(other.toUInt())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UShort): UInt = this.toUInt().rem(other.toUInt())
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UInt): UInt = this.toUInt().rem(other)
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: ULong): ULong = this.toULong().rem(other)

    /** Increments this value. */
    public operator fun inc(): UByte = UByte(data.inc())
    /** Decrements this value. */
    public operator fun dec(): UByte = UByte(data.dec())

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: UByte): UIntRange = UIntRange(this.toUInt(), other.toUInt())

    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: UByte): UByte = UByte(this.data and other.data)
    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: UByte): UByte = UByte(this.data or other.data)
    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: UByte): UByte = UByte(this.data xor other.data)
    /** Inverts the bits in this value. */
    public fun inv(): UByte = UByte(data.inv())

    public fun toByte(): Byte = data
    public fun toShort(): Short = data.toShort() and 0xFF
    public fun toInt(): Int = data.toInt() and 0xFF
    public fun toLong(): Long = data.toLong() and 0xFF

    public fun toUByte(): UByte = this
    public fun toUShort(): UShort = data.toUShort()
    public fun toUInt(): UInt = data.toUInt()
    public fun toULong(): ULong = data.toULong()

    public override fun toString(): String = toInt().toString()

}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Byte.toUByte(): UByte = UByte(this)
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Short.toUByte(): UByte = UByte(this.toByte())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Int.toUByte(): UByte = UByte(this.toByte())
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun Long.toUByte(): UByte = UByte(this.toByte())
