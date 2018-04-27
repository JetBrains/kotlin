/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

import kotlin.experimental.*

public inline class UByte internal constructor(private val data: Byte) : Comparable<UByte> {

    companion object {
        /**
         * A constant holding the minimum value an instance of UByte can have.
         */
        public /*const*/ val MIN_VALUE: UByte = UByte(0)

        /**
         * A constant holding the maximum value an instance of UByte can have.
         */
        public /*const*/ val MAX_VALUE: UByte = UByte(-1)
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: UByte): Int = TODO()

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
    public operator fun inc(): UByte = TODO()
    /** Decrements this value. */
    public operator fun dec(): UByte = TODO()

    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: UByte): UByte = UByte(this.data and other.data)
    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: UByte): UByte = UByte(this.data or other.data)
    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: UByte): UByte = UByte(this.data xor other.data)
    /** Inverts the bits in this value. */
    public fun inv(): UByte = UByte(data.inv())

    public fun toByte(): Byte = TODO()
    public fun toShort(): Short = TODO()
    public fun toInt(): Int = TODO()
    public fun toLong(): Long = TODO()

    public fun toUByte(): UByte = TODO()
    public fun toUShort(): UShort = TODO()
    public fun toUInt(): UInt = TODO()
    public fun toULong(): ULong = TODO()

}

public fun Byte.toUByte(): UByte = UByte(this.toByte())
public fun Short.toUByte(): UByte = UByte(this.toByte())
public fun Int.toUByte(): UByte = UByte(this.toByte())
public fun Long.toUByte(): UByte = UByte(this.toByte())
