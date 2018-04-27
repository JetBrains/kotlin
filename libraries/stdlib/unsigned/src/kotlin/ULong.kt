/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

import kotlin.experimental.*

public inline class ULong internal constructor(private val data: Long) : Comparable<ULong> {

    companion object {
        /**
         * A constant holding the minimum value an instance of ULong can have.
         */
        public /*const*/ val MIN_VALUE: ULong = ULong(0)

        /**
         * A constant holding the maximum value an instance of ULong can have.
         */
        public /*const*/ val MAX_VALUE: ULong = ULong(-1)
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UByte): Int = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UShort): Int = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: UInt): Int = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: ULong): Int = TODO()

    /** Adds the other value to this value. */
    public operator fun plus(other: UByte): ULong = TODO()
    /** Adds the other value to this value. */
    public operator fun plus(other: UShort): ULong = TODO()
    /** Adds the other value to this value. */
    public operator fun plus(other: UInt): ULong = TODO()
    /** Adds the other value to this value. */
    public operator fun plus(other: ULong): ULong = TODO()

    /** Subtracts the other value from this value. */
    public operator fun minus(other: UByte): ULong = TODO()
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UShort): ULong = TODO()
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UInt): ULong = TODO()
    /** Subtracts the other value from this value. */
    public operator fun minus(other: ULong): ULong = TODO()

    /** Multiplies this value by the other value. */
    public operator fun times(other: UByte): ULong = TODO()
    /** Multiplies this value by the other value. */
    public operator fun times(other: UShort): ULong = TODO()
    /** Multiplies this value by the other value. */
    public operator fun times(other: UInt): ULong = TODO()
    /** Multiplies this value by the other value. */
    public operator fun times(other: ULong): ULong = TODO()

    /** Divides this value by the other value. */
    public operator fun div(other: UByte): ULong = TODO()
    /** Divides this value by the other value. */
    public operator fun div(other: UShort): ULong = TODO()
    /** Divides this value by the other value. */
    public operator fun div(other: UInt): ULong = TODO()
    /** Divides this value by the other value. */
    public operator fun div(other: ULong): ULong = TODO()

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UByte): ULong = TODO()
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UShort): ULong = TODO()
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UInt): ULong = TODO()
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: ULong): ULong = TODO()

    /** Increments this value. */
    public operator fun inc(): ULong = TODO()
    /** Decrements this value. */
    public operator fun dec(): ULong = TODO()

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

    public fun toByte(): Byte = TODO()
    public fun toShort(): Short = TODO()
    public fun toInt(): Int = TODO()
    public fun toLong(): Long = TODO()

    public fun toUByte(): UByte = TODO()
    public fun toUShort(): UShort = TODO()
    public fun toUInt(): UInt = TODO()
    public fun toULong(): ULong = TODO()

}

public fun Byte.toULong(): ULong = ULong(this.toLong())
public fun Short.toULong(): ULong = ULong(this.toLong())
public fun Int.toULong(): ULong = ULong(this.toLong())
public fun Long.toULong(): ULong = ULong(this.toLong())
