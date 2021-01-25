/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")


package kotlin

import kotlin.native.internal.*
import kotlin.native.internal.GCCritical
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.areEqualByValue

/**
 * Represents a 8-bit signed integer.
 */
public final class Byte private constructor() : Number(), Comparable<Byte> {
    @CanBePrecreated
    companion object {
        /**
         * A constant holding the minimum value an instance of Byte can have.
         */
        public const val MIN_VALUE: Byte = -128

        /**
         * A constant holding the maximum value an instance of Byte can have.
         */
        public const val MAX_VALUE: Byte = 127

        /**
         * The number of bytes used to represent an instance of Byte in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = 1

        /**
         * The number of bits used to represent an instance of Byte in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = 8
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_COMPARE_TO)
    external public override operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Short): Int =
            this.toShort().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Int): Int =
            this.toInt().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Long): Int =
            this.toLong().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Float): Int =
            this.toFloat().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Double): Int =
            this.toDouble().compareTo(other)

    /** Adds the other value to this value. */
    public inline operator fun plus(other: Byte): Int =
            this.toInt() + other.toInt()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Short): Int =
            this.toInt() + other.toInt()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Int): Int =
            this.toInt() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Long): Long =
            this.toLong() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Float): Float =
            this.toFloat() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Double): Double =
            this.toDouble() + other

    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Byte): Int =
            this.toInt() - other.toInt()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Short): Int =
            this.toInt() - other.toInt()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Int): Int =
            this.toInt() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Long): Long =
            this.toLong() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Float): Float =
            this.toFloat() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Double): Double =
            this.toDouble() - other

    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Byte): Int =
            this.toInt() * other.toInt()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Short): Int =
            this.toInt() * other.toInt()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Int): Int =
            this.toInt() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Long): Long =
            this.toLong() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Float): Float =
            this.toFloat() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Double): Double =
            this.toDouble() * other

    /** Divides this value by the other value. */
    public inline operator fun div(other: Byte): Int =
            this.toInt() / other.toInt()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Short): Int =
            this.toInt() / other.toInt()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Int): Int =
            this.toInt() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Long): Long =
            this.toLong() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Float): Float =
            this.toFloat() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Double): Double =
            this.toDouble() / other

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Byte): Int =
            this.toInt() % other.toInt()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Short): Int =
            this.toInt() % other.toInt()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Int): Int =
            this.toInt() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Long): Long =
            this.toLong() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Float): Float =
            this.toFloat() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
            this.toDouble() % other

    /** Increments this value. */
    @TypedIntrinsic(IntrinsicType.INC)
    external public operator fun inc(): Byte
    /** Decrements this value. */
    @TypedIntrinsic(IntrinsicType.DEC)
    external public operator fun dec(): Byte
    /** Returns this value. */
    public inline operator fun unaryPlus(): Int =
            this.toInt()
    /** Returns the negative of this value. */
    public inline operator fun unaryMinus(): Int =
            -this.toInt()

    /** Returns this value. */
    public inline override fun toByte(): Byte =
            this
    /**
     * Converts this [Byte] value to [Char].
     *
     * If this value is non-negative, the resulting `Char` code is equal to this value.
     *
     * The least significant 8 bits of the resulting `Char` code are the same as the bits of this `Byte` value,
     * whereas the most significant 8 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toChar(): Char
    /**
     * Converts this [Byte] value to [Short].
     *
     * The resulting `Short` value represents the same numerical value as this `Byte`.
     *
     * The least significant 8 bits of the resulting `Short` value are the same as the bits of this `Byte` value,
     * whereas the most significant 8 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toShort(): Short
    /**
     * Converts this [Byte] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `Byte`.
     *
     * The least significant 8 bits of the resulting `Int` value are the same as the bits of this `Byte` value,
     * whereas the most significant 24 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toInt(): Int
    /**
     * Converts this [Byte] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `Byte`.
     *
     * The least significant 8 bits of the resulting `Long` value are the same as the bits of this `Byte` value,
     * whereas the most significant 56 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toLong(): Long
    /**
     * Converts this [Byte] value to [Float].
     *
     * The resulting `Float` value represents the same numerical value as this `Byte`.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toFloat(): Float
    /**
     * Converts this [Byte] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Byte`.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toDouble(): Double

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange {
        return LongRange(this.toLong(), other.toLong())
    }

    // Konan-specific.
    public fun equals(other: Byte): Boolean = kotlin.native.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Byte && kotlin.native.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Byte_toString")
    @GCCritical
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt()
    }
}

/**
 * Represents a 16-bit signed integer.
 */
public final class Short private constructor() : Number(), Comparable<Short> {
    @CanBePrecreated
    companion object {
        /**
         * A constant holding the minimum value an instance of Short can have.
         */
        public const val MIN_VALUE: Short = -32768

        /**
         * A constant holding the maximum value an instance of Short can have.
         */
        public const val MAX_VALUE: Short = 32767

        /**
         * The number of bytes used to represent an instance of Short in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = 2

        /**
         * The number of bits used to represent an instance of Short in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = 16
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Byte): Int =
            this.compareTo(other.toShort())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_COMPARE_TO)
    external public override operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Int): Int =
            this.toInt().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Long): Int =
            this.toLong().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Float): Int =
            this.toFloat().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Double): Int =
            this.toDouble().compareTo(other)

    /** Adds the other value to this value. */
    public inline operator fun plus(other: Byte): Int =
            this.toInt() + other.toInt()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Short): Int =
            this.toInt() + other.toInt()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Int): Int =
            this.toInt() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Long): Long =
            this.toLong() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Float): Float =
            this.toFloat() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Double): Double =
            this.toDouble() + other

    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Byte): Int =
            this.toInt() - other.toInt()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Short): Int =
            this.toInt() - other.toInt()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Int): Int =
            this.toInt() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Long): Long =
            this.toLong() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Float): Float =
            this.toFloat() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Double): Double =
            this.toDouble() - other

    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Byte): Int =
            this.toInt() * other.toInt()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Short): Int =
            this.toInt() * other.toInt()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Int): Int =
            this.toInt() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Long): Long =
            this.toLong() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Float): Float =
            this.toFloat() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Double): Double =
            this.toDouble() * other

    /** Divides this value by the other value. */
    public inline operator fun div(other: Byte): Int =
            this.toInt() / other.toInt()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Short): Int =
            this.toInt() / other.toInt()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Int): Int =
            this.toInt() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Long): Long =
            this.toLong() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Float): Float =
            this.toFloat() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Double): Double =
            this.toDouble() / other

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Byte): Int =
            this.toInt() % other.toInt()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Short): Int =
            this.toInt() % other.toInt()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Int): Int =
            this.toInt() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Long): Long =
            this.toLong() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Float): Float =
            this.toFloat() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
            this.toDouble() % other

    /** Increments this value. */
    @TypedIntrinsic(IntrinsicType.INC)
    external public operator fun inc(): Short
    /** Decrements this value. */
    @TypedIntrinsic(IntrinsicType.DEC)
    external public operator fun dec(): Short
    /** Returns this value. */
    public operator fun unaryPlus(): Int =
            this.toInt()
    /** Returns the negative of this value. */
    public inline operator fun unaryMinus(): Int =
            -this.toInt()

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange  {
        return LongRange(this.toLong(), other.toLong())
    }

    /**
     * Converts this [Short] value to [Byte].
     *
     * If this value is in [Byte.MIN_VALUE]..[Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `Short`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `Short` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toByte(): Byte
    /**
     * Converts this [Short] value to [Char].
     *
     * The resulting `Char` code is equal to this value reinterpreted as an unsigned number,
     * i.e. it has the same binary representation as this `Short`.
     */
    @TypedIntrinsic(IntrinsicType.ZERO_EXTEND)
    external public override fun toChar(): Char

    /** Returns this value. */
    public inline override fun toShort(): Short =
            this
    /**
     * Converts this [Short] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `Short`.
     *
     * The least significant 16 bits of the resulting `Int` value are the same as the bits of this `Short` value,
     * whereas the most significant 16 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toInt(): Int
    /**
     * Converts this [Short] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `Short`.
     *
     * The least significant 16 bits of the resulting `Long` value are the same as the bits of this `Short` value,
     * whereas the most significant 48 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toLong(): Long
    /**
     * Converts this [Short] value to [Float].
     *
     * The resulting `Float` value represents the same numerical value as this `Short`.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toFloat(): Float
    /**
     * Converts this [Short] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Short`.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toDouble(): Double

    // Konan-specific.
    public fun equals(other: Short): Boolean = kotlin.native.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
        other is Short && kotlin.native.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Short_toString")
    @GCCritical
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt()
    }
}

/**
 * Represents a 32-bit signed integer.
 */
public final class Int private constructor() : Number(), Comparable<Int> {
    @CanBePrecreated
    companion object {
        /**
         * A constant holding the minimum value an instance of Int can have.
         */
        public const val MIN_VALUE: Int = -2147483648

        /**
         * A constant holding the maximum value an instance of Int can have.
         */
        public const val MAX_VALUE: Int = 2147483647

        /**
         * The number of bytes used to represent an instance of Int in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = 4

        /**
         * The number of bits used to represent an instance of Int in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = 32
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Byte): Int =
            this.compareTo(other.toInt())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Short): Int =
            this.compareTo(other.toInt())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_COMPARE_TO)
    external public override operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Long): Int =
            this.toLong().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Float): Int =
            this.toFloat().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Double): Int =
            this.toDouble().compareTo(other)

    /** Adds the other value to this value. */
    public inline operator fun plus(other: Byte): Int =
            this + other.toInt()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Short): Int =
            this + other.toInt()
    /** Adds the other value to this value. */
    @TypedIntrinsic(IntrinsicType.PLUS)
    external public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Long): Long =
            this.toLong() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Float): Float =
            this.toFloat() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Double): Double =
            this.toDouble() + other

    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Byte): Int =
            this - other.toInt()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Short): Int =
            this - other.toInt()
    /** Subtracts the other value from this value. */
    @TypedIntrinsic(IntrinsicType.MINUS)
    external public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Long): Long =
            this.toLong() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Float): Float =
            this.toFloat() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Double): Double =
            this.toDouble() - other

    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Byte): Int =
            this * other.toInt()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Short): Int =
            this * other.toInt()
    /** Multiplies this value by the other value. */
    @TypedIntrinsic(IntrinsicType.TIMES)
    external public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Long): Long =
            this.toLong() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Float): Float =
            this.toFloat() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Double): Double =
            this.toDouble() * other

    /** Divides this value by the other value. */
    public inline operator fun div(other: Byte): Int =
            this / other.toInt()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Short): Int =
            this / other.toInt()
    /** Divides this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_DIV)
    external public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    public inline operator fun div(other: Long): Long =
            this.toLong() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Float): Float =
            this.toFloat() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Double): Double =
            this.toDouble() / other

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Byte): Int =
            this % other.toInt()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Short): Int =
            this % other.toInt()
    /** Calculates the remainder of dividing this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_REM)
    external public operator fun rem(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Long): Long =
            this.toLong() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Float): Float =
            this.toFloat() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
            this.toDouble() % other

    /** Increments this value. */
    @TypedIntrinsic(IntrinsicType.INC)
    external public operator fun inc(): Int
    /** Decrements this value. */
    @TypedIntrinsic(IntrinsicType.DEC)
    external public operator fun dec(): Int
    /** Returns this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_PLUS)
    external public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_MINUS)
    external public operator fun unaryMinus(): Int

    /** Shifts this value left by the [bitCount] number of bits. */
    @TypedIntrinsic(IntrinsicType.SHL)
    external public infix fun shl(bitCount: Int): Int
    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with copies of the sign bit. */
    @TypedIntrinsic(IntrinsicType.SHR)
    external public infix fun shr(bitCount: Int): Int
    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros. */
    @TypedIntrinsic(IntrinsicType.USHR)
    external public infix fun ushr(bitCount: Int): Int
    /** Performs a bitwise AND operation between the two values. */
    @TypedIntrinsic(IntrinsicType.AND)
    external public infix fun and(other: Int): Int
    /** Performs a bitwise OR operation between the two values. */
    @TypedIntrinsic(IntrinsicType.OR)
    external public infix fun or(other: Int): Int
    /** Performs a bitwise XOR operation between the two values. */
    @TypedIntrinsic(IntrinsicType.XOR)
    external public infix fun xor(other: Int): Int
    /** Inverts the bits in this value. */
    @TypedIntrinsic(IntrinsicType.INV)
    external public fun inv(): Int

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange  {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange {
        return LongRange(this.toLong(), other.toLong())
    }

    /**
     * Converts this [Int] value to [Byte].
     *
     * If this value is in [Byte.MIN_VALUE]..[Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `Int`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `Int` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toByte(): Byte
    /**
     * Converts this [Int] value to [Char].
     *
     * If this value is in the range of `Char` codes `Char.MIN_VALUE..Char.MAX_VALUE`,
     * the resulting `Char` code is equal to this value.
     *
     * The resulting `Char` code is represented by the least significant 16 bits of this `Int` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toChar(): Char
    /**
     * Converts this [Int] value to [Short].
     *
     * If this value is in [Short.MIN_VALUE]..[Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `Int`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `Int` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toShort(): Short

    /** Returns this value. */
    public inline override fun toInt(): Int =
            this
    /**
     * Converts this [Int] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `Int`.
     *
     * The least significant 32 bits of the resulting `Long` value are the same as the bits of this `Int` value,
     * whereas the most significant 32 bits are filled with the sign bit of this value.
     */
    @TypedIntrinsic(IntrinsicType.SIGN_EXTEND)
    external public override fun toLong(): Long
    /**
     * Converts this [Int] value to [Float].
     *
     * The resulting value is the closest `Float` to this `Int` value.
     * In case when this `Int` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toFloat(): Float
    /**
     * Converts this [Int] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Int`.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toDouble(): Double

    // Konan-specific.
    public fun equals(other: Int): Boolean = kotlin.native.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
         other is Int && kotlin.native.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Int_toString")
    @GCCritical
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this
    }
}

/**
 * Represents a 64-bit signed integer.
 */
public final class Long private constructor() : Number(), Comparable<Long> {
    @CanBePrecreated
    companion object {
        /**
         * A constant holding the minimum value an instance of Long can have.
         */
        public const val MIN_VALUE: Long = -9223372036854775807L - 1L

        /**
         * A constant holding the maximum value an instance of Long can have.
         */
        public const val MAX_VALUE: Long = 9223372036854775807L

        /**
         * The number of bytes used to represent an instance of Long in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = 8

        /**
         * The number of bits used to represent an instance of Long in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = 64
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Byte): Int =
            this.compareTo(other.toLong())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Short): Int =
            this.compareTo(other.toLong())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Int): Int =
            this.compareTo(other.toLong())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_COMPARE_TO)
    external public override operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Float): Int =
            this.toFloat().compareTo(other)
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Double): Int =
            this.toDouble().compareTo(other)

    /** Adds the other value to this value. */
    public inline operator fun plus(other: Byte): Long =
            this + other.toLong()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Short): Long =
            this + other.toLong()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Int): Long =
            this + other.toLong()
    /** Adds the other value to this value. */
    @TypedIntrinsic(IntrinsicType.PLUS)
    external public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Float): Float =
            this.toFloat() + other
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Double): Double =
            this.toDouble() + other

    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Byte): Long =
            this - other.toLong()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Short): Long =
            this - other.toLong()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Int): Long =
            this - other.toLong()
    /** Subtracts the other value from this value. */
    @TypedIntrinsic(IntrinsicType.MINUS)
    external public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Float): Float =
            this.toFloat() - other
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Double): Double =
            this.toDouble() - other

    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Byte): Long =
            this * other.toLong()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Short): Long =
            this * other.toLong()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Int): Long =
            this * other.toLong()
    /** Multiplies this value by the other value. */
    @TypedIntrinsic(IntrinsicType.TIMES)
    external public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Float): Float =
            this.toFloat() * other
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Double): Double =
            this.toDouble() * other

    /** Divides this value by the other value. */
    public inline operator fun div(other: Byte): Long =
            this / other.toLong()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Short): Long =
            this / other.toLong()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Int): Long =
            this / other.toLong()
    /** Divides this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_DIV)
    external public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    public inline operator fun div(other: Float): Float =
            this.toFloat() / other
    /** Divides this value by the other value. */
    public inline operator fun div(other: Double): Double =
            this.toDouble() / other

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Byte): Long =
            this % other.toLong()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Short): Long =
            this % other.toLong()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Int): Long =
            this % other.toLong()
    /** Calculates the remainder of dividing this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_REM)
    external public operator fun rem(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Float): Float =
            this.toFloat() % other
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
            this.toDouble() % other

    /** Increments this value. */
    @TypedIntrinsic(IntrinsicType.INC)
    external public operator fun inc(): Long
    /** Decrements this value. */
    @TypedIntrinsic(IntrinsicType.DEC)
    external public operator fun dec(): Long
    /** Returns this value. */
    public inline operator fun unaryPlus(): Long =
            this
    /** Returns the negative of this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_MINUS)
    external public operator fun unaryMinus(): Long

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): LongRange {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): LongRange  {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): LongRange {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange  {
        return LongRange(this, other.toLong())
    }

    /** Shifts this value left by the [bitCount] number of bits. */
    @TypedIntrinsic(IntrinsicType.SHL)
    external public infix fun shl(bitCount: Int): Long
    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with copies of the sign bit. */
    @TypedIntrinsic(IntrinsicType.SHR)
    external public infix fun shr(bitCount: Int): Long
    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros. */
    @TypedIntrinsic(IntrinsicType.USHR)
    external public infix fun ushr(bitCount: Int): Long
    /** Performs a bitwise AND operation between the two values. */
    @TypedIntrinsic(IntrinsicType.AND)
    external public infix fun and(other: Long): Long
    /** Performs a bitwise OR operation between the two values. */
    @TypedIntrinsic(IntrinsicType.OR)
    external public infix fun or(other: Long): Long
    /** Performs a bitwise XOR operation between the two values. */
    @TypedIntrinsic(IntrinsicType.XOR)
    external public infix fun xor(other: Long): Long
    /** Inverts the bits in this value. */
    @TypedIntrinsic(IntrinsicType.INV)
    external public fun inv(): Long

    /**
     * Converts this [Long] value to [Byte].
     *
     * If this value is in [Byte.MIN_VALUE]..[Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `Long`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `Long` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toByte(): Byte
    /**
     * Converts this [Long] value to [Char].
     *
     * If this value is in the range of `Char` codes `Char.MIN_VALUE..Char.MAX_VALUE`,
     * the resulting `Char` code is equal to this value.
     *
     * The resulting `Char` code is represented by the least significant 16 bits of this `Long` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toChar(): Char
    /**
     * Converts this [Long] value to [Short].
     *
     * If this value is in [Short.MIN_VALUE]..[Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `Long`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `Long` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toShort(): Short
    /**
     * Converts this [Long] value to [Int].
     *
     * If this value is in [Int.MIN_VALUE]..[Int.MAX_VALUE], the resulting `Int` value represents
     * the same numerical value as this `Long`.
     *
     * The resulting `Int` value is represented by the least significant 32 bits of this `Long` value.
     */
    @TypedIntrinsic(IntrinsicType.INT_TRUNCATE)
    external public override fun toInt(): Int

    /** Returns this value. */
    public inline override fun toLong(): Long =
            this
    /**
     * Converts this [Long] value to [Float].
     *
     * The resulting value is the closest `Float` to this `Long` value.
     * In case when this `Long` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toFloat(): Float
    /**
     * Converts this [Long] value to [Double].
     *
     * The resulting value is the closest `Double` to this `Long` value.
     * In case when this `Long` value is exactly between two `Double`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @TypedIntrinsic(IntrinsicType.SIGNED_TO_FLOAT)
    external public override fun toDouble(): Double

    // Konan-specific.
    public fun equals(other: Long): Boolean = kotlin.native.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Long && kotlin.native.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Long_toString")
    @GCCritical
    external public override fun toString(): String

    public override fun hashCode(): Int {
       return ((this ushr 32) xor this).toInt()
    }
}

/**
 * Represents a single-precision 32-bit IEEE 754 floating point number.
 */
public final class Float private constructor() : Number(), Comparable<Float> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Float.
         */
        public const val MIN_VALUE: Float = 1.40129846432481707e-45f

        /**
         * A constant holding the largest positive finite value of Float.
         */
        public const val MAX_VALUE: Float = 3.40282346638528860e+38f

        /**
         * A constant holding the positive infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public const val POSITIVE_INFINITY: Float = 1.0f / 0.0f

        /**
         * A constant holding the negative infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public const val NEGATIVE_INFINITY: Float = -1.0f / 0.0f

        /**
         * A constant holding the "not a number" value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public const val NaN: Float = -(0.0f / 0.0f)

        /**
         * The number of bytes used to represent an instance of Float in a binary form.
         */
        @SinceKotlin("1.4")
        public const val SIZE_BYTES: Int = 4

        /**
         * The number of bits used to represent an instance of Float in a binary form.
         */
        @SinceKotlin("1.4")
        public const val SIZE_BITS: Int = 32
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Byte): Int = compareTo(other.toFloat())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Short): Int = compareTo(other.toFloat())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Int): Int = compareTo(other.toFloat())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Long): Int = compareTo(other.toFloat())
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: Float): Int {
        // if any of values in NaN both comparisons return false
        if (this > other) return 1
        if (this < other) return -1

        val thisBits = this.toBits()
        val otherBits = other.toBits()

        // Canonical NaN bits representation higher than any other bit representvalue
        return thisBits.compareTo(otherBits)
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Double): Int = - other.compareTo(this)

    /** Adds the other value to this value. */
    public inline operator fun plus(other: Byte): Float =
            this + other.toFloat()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Short): Float =
            this + other.toFloat()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Int): Float =
            this + other.toFloat()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Long): Float =
            this + other.toFloat()
    /** Adds the other value to this value. */
    @TypedIntrinsic(IntrinsicType.PLUS)
    external public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Double): Double =
            this.toDouble() + other

    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Byte): Float =
            this - other.toFloat()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Short): Float =
            this - other.toFloat()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Int): Float =
            this - other.toFloat()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Long): Float =
            this - other.toFloat()
    /** Subtracts the other value from this value. */
    @TypedIntrinsic(IntrinsicType.MINUS)
    external public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Double): Double =
            this.toDouble() - other

    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Byte): Float =
            this * other.toFloat()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Short): Float =
            this * other.toFloat()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Int): Float =
            this * other.toFloat()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Long): Float =
            this * other.toFloat()
    /** Multiplies this value by the other value. */
    @TypedIntrinsic(IntrinsicType.TIMES)
    external public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Double): Double =
            this.toDouble() * other

    /** Divides this value by the other value. */
    public inline operator fun div(other: Byte): Float =
            this / other.toFloat()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Short): Float =
            this / other.toFloat()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Int): Float =
            this / other.toFloat()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Long): Float =
            this / other.toFloat()
    /** Divides this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_DIV)
    external public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    public inline operator fun div(other: Double): Double =
            this.toDouble() / other

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Byte): Float =
            this % other.toFloat()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Short): Float =
            this % other.toFloat()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Int): Float =
            this % other.toFloat()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Long): Float =
            this % other.toFloat()
    /** Calculates the remainder of dividing this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_REM)
    external public operator fun rem(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
            this.toDouble() % other

    /** Increments this value. */
    @TypedIntrinsic(IntrinsicType.INC)
    external public operator fun inc(): Float
    /** Decrements this value. */
    @TypedIntrinsic(IntrinsicType.DEC)
    external public operator fun dec(): Float
    /** Returns this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_PLUS)
    external public operator fun unaryPlus(): Float
    /** Returns the negative of this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_MINUS)
    external public operator fun unaryMinus(): Float

    /**
     * Converts this [Float] value to [Byte].
     *
     * The resulting `Byte` value is equal to `this.toInt().toByte()`.
     */
    @Deprecated("Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.", ReplaceWith("toInt().toByte()"))
    public override fun toByte(): Byte = this.toInt().toByte()

    /**
     * Converts this [Float] value to [Char].
     *
     * The resulting `Char` value is equal to `this.toInt().toChar()`.
     */
    public override fun toChar(): Char = this.toInt().toChar()

    /**
     * Converts this [Float] value to [Short].
     *
     * The resulting `Short` value is equal to `this.toInt().toShort()`.
     */
    @Deprecated("Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.", ReplaceWith("toInt().toShort()"))
    public override fun toShort(): Short = this.toInt().toShort()

    /**
     * Converts this [Float] value to [Int].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Float` value is `NaN`, [Int.MIN_VALUE] if it's less than `Int.MIN_VALUE`,
     * [Int.MAX_VALUE] if it's bigger than `Int.MAX_VALUE`.
     */
    @SymbolName("Kotlin_Float_toInt")
    @GCCritical
    external public override fun toInt(): Int
    /**
     * Converts this [Float] value to [Long].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Float` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
     * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
     */
    @SymbolName("Kotlin_Float_toLong")
    @GCCritical
    external public override fun toLong(): Long

    /** Returns this value. */
    public inline override fun toFloat(): Float =
            this
    /**
     * Converts this [Float] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Float`.
     */
    @TypedIntrinsic(IntrinsicType.FLOAT_EXTEND)
    external public override fun toDouble(): Double

    public fun equals(other: Float): Boolean = toBits() == other.toBits()

    public override fun equals(other: Any?): Boolean = other is Float && this.equals(other)

    public override fun toString() = NumberConverter.convert(this)

    public override fun hashCode(): Int {
        return bits()
    }

    @TypedIntrinsic(IntrinsicType.REINTERPRET)
    @PublishedApi
    external internal fun bits(): Int
}

/**
 * Represents a double-precision 64-bit IEEE 754 floating point number.
 */
public final class Double private constructor() : Number(), Comparable<Double> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Double.
         */
        public const val MIN_VALUE: Double = 4.9e-324

        /**
         * A constant holding the largest positive finite value of Double.
         */
        public const val MAX_VALUE: Double = 1.7976931348623157e+308

        /**
         * A constant holding the positive infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public const val POSITIVE_INFINITY: Double = 1.0 / 0.0

        /**
         * A constant holding the negative infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public const val NEGATIVE_INFINITY: Double = -1.0 / 0.0

        /**
         * A constant holding the "not a number" value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public const val NaN: Double = -(0.0 / 0.0)

        /**
         * The number of bytes used to represent an instance of Double in a binary form.
         */
        @SinceKotlin("1.4")
        public const val SIZE_BYTES: Int = 8

        /**
         * The number of bits used to represent an instance of Double in a binary form.
         */
        @SinceKotlin("1.4")
        public const val SIZE_BITS: Int = 64
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Byte): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Short): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Int): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Long): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public operator fun compareTo(other: Float): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: Double): Int {
        // if any of values in NaN both comparisons return false
        if (this > other) return 1
        if (this < other) return -1

        val thisBits = this.toBits()
        val otherBits = other.toBits()

        // Canonical NaN bits representation higher than any other bit representvalue
        return thisBits.compareTo(otherBits)
    }

    /** Adds the other value to this value. */
    public inline operator fun plus(other: Byte): Double =
            this + other.toDouble()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Short): Double =
            this + other.toDouble()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Int): Double =
            this + other.toDouble()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Long): Double =
            this + other.toDouble()
    /** Adds the other value to this value. */
    public inline operator fun plus(other: Float): Double =
            this + other.toDouble()
    /** Adds the other value to this value. */
    @TypedIntrinsic(IntrinsicType.PLUS)
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Byte): Double =
            this - other.toDouble()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Short): Double =
            this - other.toDouble()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Int): Double =
            this - other.toDouble()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Long): Double =
            this - other.toDouble()
    /** Subtracts the other value from this value. */
    public inline operator fun minus(other: Float): Double =
            this - other.toDouble()
    /** Subtracts the other value from this value. */
    @TypedIntrinsic(IntrinsicType.MINUS)
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Byte): Double =
            this * other.toDouble()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Short): Double =
            this * other.toDouble()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Int): Double =
            this * other.toDouble()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Long): Double =
            this * other.toDouble()
    /** Multiplies this value by the other value. */
    public inline operator fun times(other: Float): Double =
            this * other.toDouble()
    /** Multiplies this value by the other value. */
    @TypedIntrinsic(IntrinsicType.TIMES)
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public inline operator fun div(other: Byte): Double =
            this / other.toDouble()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Short): Double =
            this / other.toDouble()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Int): Double =
            this / other.toDouble()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Long): Double =
            this / other.toDouble()
    /** Divides this value by the other value. */
    public inline operator fun div(other: Float): Double =
            this / other.toDouble()
    /** Divides this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_DIV)
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Byte): Double =
            this % other.toDouble()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Short): Double =
            this % other.toDouble()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Int): Double =
            this % other.toDouble()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Long): Double =
            this % other.toDouble()
    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Float): Double =
            this % other.toDouble()
    /** Calculates the remainder of dividing this value by the other value. */
    @TypedIntrinsic(IntrinsicType.SIGNED_REM)
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @TypedIntrinsic(IntrinsicType.INC)
    external public operator fun inc(): Double
    /** Decrements this value. */
    @TypedIntrinsic(IntrinsicType.DEC)
    external public operator fun dec(): Double
    /** Returns this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_PLUS)
    external public operator fun unaryPlus(): Double
    /** Returns the negative of this value. */
    @TypedIntrinsic(IntrinsicType.UNARY_MINUS)
    external public operator fun unaryMinus(): Double

    /**
     * Converts this [Double] value to [Byte].
     *
     * The resulting `Byte` value is equal to `this.toInt().toByte()`.
     */
    @Deprecated("Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.", ReplaceWith("toInt().toByte()"))
    public override fun toByte(): Byte = this.toInt().toByte()

    /**
     * Converts this [Double] value to [Char].
     *
     * The resulting `Char` value is equal to `this.toInt().toChar()`.
     */
    public override fun toChar(): Char = this.toInt().toChar()

    /**
     * Converts this [Double] value to [Short].
     *
     * The resulting `Short` value is equal to `this.toInt().toShort()`.
     */
    @Deprecated("Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.", ReplaceWith("toInt().toShort()"))
    public override fun toShort(): Short = this.toInt().toShort()

    /**
     * Converts this [Double] value to [Int].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Double` value is `NaN`, [Int.MIN_VALUE] if it's less than `Int.MIN_VALUE`,
     * [Int.MAX_VALUE] if it's bigger than `Int.MAX_VALUE`.
     */
    @SymbolName("Kotlin_Double_toInt")
    @GCCritical
    external public override fun toInt(): Int
    /**
     * Converts this [Double] value to [Long].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Double` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
     * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
     */
    @SymbolName("Kotlin_Double_toLong")
    @GCCritical
    external public override fun toLong(): Long
    /**
     * Converts this [Double] value to [Float].
     *
     * The resulting value is the closest `Float` to this `Double` value.
     * In case when this `Double` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @TypedIntrinsic(IntrinsicType.FLOAT_TRUNCATE)
    external public override fun toFloat(): Float

    /** Returns this value. */
    public inline override fun toDouble(): Double =
            this

    public fun equals(other: Double): Boolean = toBits() == other.toBits()

    public override fun equals(other: Any?): Boolean = other is Double && this.equals(other)

    public override fun toString(): String = NumberConverter.convert(this)

    public override fun hashCode(): Int = bits().hashCode()

    @TypedIntrinsic(IntrinsicType.REINTERPRET)
    @PublishedApi
    external internal fun bits(): Long
}
