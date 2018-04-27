/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

import kotlin.experimental.*

public inline class UInt internal constructor(private val data: Int) : Comparable<UInt> {

    companion object {
        /**
         * A constant holding the minimum value an instance of UInt can have.
         */
        public /*const*/ val MIN_VALUE: UInt = UInt(0)

        /**
         * A constant holding the maximum value an instance of UInt can have.
         */
        public /*const*/ val MAX_VALUE: UInt = UInt(-1)
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
    public override operator fun compareTo(other: UInt): Int = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: ULong): Int = TODO()

    /** Adds the other value to this value. */
    public operator fun plus(other: UByte): UInt = TODO()
    /** Adds the other value to this value. */
    public operator fun plus(other: UShort): UInt = TODO()
    /** Adds the other value to this value. */
    public operator fun plus(other: UInt): UInt = TODO()
    /** Adds the other value to this value. */
    public operator fun plus(other: ULong): ULong = TODO()

    /** Subtracts the other value from this value. */
    public operator fun minus(other: UByte): UInt = TODO()
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UShort): UInt = TODO()
    /** Subtracts the other value from this value. */
    public operator fun minus(other: UInt): UInt = TODO()
    /** Subtracts the other value from this value. */
    public operator fun minus(other: ULong): ULong = TODO()

    /** Multiplies this value by the other value. */
    public operator fun times(other: UByte): UInt = TODO()
    /** Multiplies this value by the other value. */
    public operator fun times(other: UShort): UInt = TODO()
    /** Multiplies this value by the other value. */
    public operator fun times(other: UInt): UInt = TODO()
    /** Multiplies this value by the other value. */
    public operator fun times(other: ULong): ULong = TODO()

    /** Divides this value by the other value. */
    public operator fun div(other: UByte): UInt = TODO()
    /** Divides this value by the other value. */
    public operator fun div(other: UShort): UInt = TODO()
    /** Divides this value by the other value. */
    public operator fun div(other: UInt): UInt = TODO()
    /** Divides this value by the other value. */
    public operator fun div(other: ULong): ULong = TODO()

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UByte): UInt = TODO()
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UShort): UInt = TODO()
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: UInt): UInt = TODO()
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun rem(other: ULong): ULong = TODO()

    /** Increments this value. */
    public operator fun inc(): UInt = TODO()
    /** Decrements this value. */
    public operator fun dec(): UInt = TODO()

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

    public fun toByte(): Byte = TODO()
    public fun toShort(): Short = TODO()
    public fun toInt(): Int = TODO()
    public fun toLong(): Long = TODO()

    public fun toUByte(): UByte = TODO()
    public fun toUShort(): UShort = TODO()
    public fun toUInt(): UInt = TODO()
    public fun toULong(): ULong = TODO()

}

public fun Byte.toUInt(): UInt = UInt(this.toInt())
public fun Short.toUInt(): UInt = UInt(this.toInt())
public fun Int.toUInt(): UInt = UInt(this.toInt())
public fun Long.toUInt(): UInt = UInt(this.toInt())
