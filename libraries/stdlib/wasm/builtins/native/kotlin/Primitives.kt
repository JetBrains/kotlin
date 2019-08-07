/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")

package kotlin

import kotlin.wasm.internal.*

/**
 * Represents a 8-bit signed integer.
 */
public class Byte private constructor() : Number(), Comparable<Byte> {
    @ExcludedFromCodegen
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
    public override inline operator fun compareTo(other: Byte): Int =
        wasm_i32_compareTo(this.toInt(), other.toInt())

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
    public inline operator fun inc(): Byte =
        (this + 1).toByte()

    /** Decrements this value. */
    public inline operator fun dec(): Byte =
        (this - 1).toByte()

    /** Returns this value. */
    public inline operator fun unaryPlus(): Int =
        this.toInt()

    /** Returns the negative of this value. */
    public inline operator fun unaryMinus(): Int =
        -this.toInt()

    /** Returns this value. */
    public override inline fun toByte(): Byte =
        this

    /**
     * Converts this [Byte] value to [Char].
     *
     * If this value is non-negative, the resulting `Char` code is equal to this value.
     *
     * The least significant 8 bits of the resulting `Char` code are the same as the bits of this `Byte` value,
     * whereas the most significant 8 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.NOP)
    public override fun toChar(): Char = implementedAsIntrinsic

    /**
     * Converts this [Byte] value to [Short].
     *
     * The resulting `Short` value represents the same numerical value as this `Byte`.
     *
     * The least significant 8 bits of the resulting `Short` value are the same as the bits of this `Byte` value,
     * whereas the most significant 8 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.NOP)
    public override fun toShort(): Short = implementedAsIntrinsic

    /**
     * Converts this [Byte] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `Byte`.
     *
     * The least significant 8 bits of the resulting `Int` value are the same as the bits of this `Byte` value,
     * whereas the most significant 24 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.NOP)
    public override fun toInt(): Int = implementedAsIntrinsic

    /**
     * Converts this [Byte] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `Byte`.
     *
     * The least significant 8 bits of the resulting `Long` value are the same as the bits of this `Byte` value,
     * whereas the most significant 56 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.I64_EXTEND_I32_S)
    public override fun toLong(): Long = implementedAsIntrinsic

    /**
     * Converts this [Byte] value to [Float].
     *
     * The resulting `Float` value represents the same numerical value as this `Byte`.
     */
    @WasmInstruction(WasmInstruction.F32_CONVERT_I32_S)
    public override fun toFloat(): Float = implementedAsIntrinsic

    /**
     * Converts this [Byte] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Byte`.
     */
    @WasmInstruction(WasmInstruction.F64_CONVERT_I32_S)
    public override fun toDouble(): Double = implementedAsIntrinsic

//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Byte): IntRange {
//        return IntRange(this.toInt(), other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Short): IntRange {
//        return IntRange(this.toInt(), other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Int): IntRange {
//        return IntRange(this.toInt(), other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Long): LongRange {
//        return LongRange(this.toLong(), other.toLong())
//    }

    // TODO: Support Any? and type operators
//    public override fun equals(other: Any?): Boolean =
//        other is Byte && wasm_i32_eq(this.toInt(), other.toInt()).reinterpretAsBoolean()

    public inline fun equals(other: Byte): Boolean =
        wasm_i32_eq(this.toInt(), other.toInt()).reinterpretAsBoolean()

    // TODO: Implement Byte.toString()
    // public override fun toString(): String

    public override inline fun hashCode(): Int =
        this.toInt()
}

/**
 * Represents a 16-bit signed integer.
 */
public class Short private constructor() : Number(), Comparable<Short> {
    @ExcludedFromCodegen
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
    public override inline operator fun compareTo(other: Short): Int =
        this.toInt().compareTo(other.toInt())

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
    public inline operator fun inc(): Short =
        (this + 1).toShort()

    /** Decrements this value. */
    public inline operator fun dec(): Short =
        (this - 1).toShort()

    /** Returns this value. */
    public inline operator fun unaryPlus(): Int =
        this.toInt()

    /** Returns the negative of this value. */
    public inline operator fun unaryMinus(): Int =
        -this.toInt()

//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Byte): IntRange {
//        return IntRange(this.toInt(), other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Short): IntRange {
//        return IntRange(this.toInt(), other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Int): IntRange {
//        return IntRange(this.toInt(), other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Long): LongRange  {
//        return LongRange(this.toLong(), other.toLong())
//    }

    /**
     * Converts this [Short] value to [Byte].
     *
     * If this value is in [Byte.MIN_VALUE]..[Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `Short`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `Short` value.
     */
    public override inline fun toByte(): Byte =
        this.toInt().toByte()

    /**
     * Converts this [Short] value to [Char].
     *
     * The resulting `Char` code is equal to this value reinterpreted as an unsigned number,
     * i.e. it has the same binary representation as this `Short`.
     */
    @WasmInstruction(WasmInstruction.NOP)
    public override fun toChar(): Char =
        implementedAsIntrinsic

    /** Returns this value. */
    public override inline fun toShort(): Short =
        this

    /**
     * Converts this [Short] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `Short`.
     *
     * The least significant 16 bits of the resulting `Int` value are the same as the bits of this `Short` value,
     * whereas the most significant 16 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.NOP)
    public override fun toInt(): Int =
        implementedAsIntrinsic

    /**
     * Converts this [Short] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `Short`.
     *
     * The least significant 16 bits of the resulting `Long` value are the same as the bits of this `Short` value,
     * whereas the most significant 48 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.I64_EXTEND_I32_S)
    public override fun toLong(): Long =
        implementedAsIntrinsic

    /**
     * Converts this [Short] value to [Float].
     *
     * The resulting `Float` value represents the same numerical value as this `Short`.
     */
    @WasmInstruction(WasmInstruction.F32_CONVERT_I32_S)
    public override fun toFloat(): Float =
        implementedAsIntrinsic

    /**
     * Converts this [Short] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Short`.
     */
    @WasmInstruction(WasmInstruction.F64_CONVERT_I32_S)
    public override fun toDouble(): Double =
        implementedAsIntrinsic

    public inline fun equals(other: Short): Boolean =
        wasm_i32_eq(this.toInt(), other.toInt()).reinterpretAsBoolean()

    // TODO: Support Any? and type operators
//    public override fun equals(other: Any?): Boolean =
//        other is Short && wasm_i32_eq(this.toInt(), other.toInt()).reinterpretAsBoolean()

    // TODO: Implement Short.toString()
    // public override fun toString(): String

    public override inline fun hashCode(): Int =
        this.toInt()
}

/**
 * Represents a 32-bit signed integer.
 */
public class Int private constructor() : Number(), Comparable<Int> {

    @ExcludedFromCodegen
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
    public override inline operator fun compareTo(other: Int): Int =
        wasm_i32_compareTo(this, other)

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
    @WasmInstruction(WasmInstruction.I32_ADD)
    public operator fun plus(other: Int): Int =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I32_SUB)
    public operator fun minus(other: Int): Int =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I32_MUL)
    public operator fun times(other: Int): Int =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I32_DIV_S)
    public operator fun div(other: Int): Int =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I32_REM_S)
    public operator fun rem(other: Int): Int =
        implementedAsIntrinsic

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
    public inline operator fun inc(): Int =
        this + 1

    /** Decrements this value. */
    public inline operator fun dec(): Int =
        this - 1

    /** Returns this value. */
    public inline operator fun unaryPlus(): Int = this

    /** Returns the negative of this value. */
    public inline operator fun unaryMinus(): Int = 0 - this

    /** Shifts this value left by the [bitCount] number of bits. */
    @WasmInstruction(WasmInstruction.I32_SHL)
    public infix fun shl(bitCount: Int): Int =
        implementedAsIntrinsic

    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with copies of the sign bit. */
    @WasmInstruction(WasmInstruction.I32_SHR_S)
    public infix fun shr(bitCount: Int): Int =
        implementedAsIntrinsic

    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros. */
    @WasmInstruction(WasmInstruction.I32_SHR_U)
    public infix fun ushr(bitCount: Int): Int =
        implementedAsIntrinsic

    /** Performs a bitwise AND operation between the two values. */
    @WasmInstruction(WasmInstruction.I32_AND)
    public infix fun and(other: Int): Int =
        implementedAsIntrinsic

    /** Performs a bitwise OR operation between the two values. */
    @WasmInstruction(WasmInstruction.I32_OR)
    public infix fun or(other: Int): Int =
        implementedAsIntrinsic

    /** Performs a bitwise XOR operation between the two values. */
    @WasmInstruction(WasmInstruction.I32_XOR)
    public infix fun xor(other: Int): Int =
        implementedAsIntrinsic

    /** Inverts the bits in this value. */
    public inline fun inv(): Int =
        this.xor(-1)

//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Byte): IntRange {
//        return IntRange(this, other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Short): IntRange {
//        return IntRange(this, other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Int): IntRange  {
//        return IntRange(this, other.toInt())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Long): LongRange {
//        return LongRange(this.toLong(), other.toLong())
//    }

    /**
     * Converts this [Int] value to [Byte].
     *
     * If this value is in [Byte.MIN_VALUE]..[Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `Int`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `Int` value.
     */
    public override fun toByte(): Byte =
        ((this shl 24) shr 24).reinterpretAsByte()

    /**
     * Converts this [Int] value to [Char].
     *
     * If this value is in the range of `Char` codes `Char.MIN_VALUE..Char.MAX_VALUE`,
     * the resulting `Char` code is equal to this value.
     *
     * The resulting `Char` code is represented by the least significant 16 bits of this `Int` value.
     */
    public override fun toChar(): Char =
        (this and 0xFFFF).reinterpretAsChar()

    /**
     * Converts this [Int] value to [Short].
     *
     * If this value is in [Short.MIN_VALUE]..[Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `Int`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `Int` value.
     */
    public override fun toShort(): Short =
        ((this shl 16) shr 16).reinterpretAsShort()

    /** Returns this value. */
    public override inline fun toInt(): Int =
        this

    /**
     * Converts this [Int] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `Int`.
     *
     * The least significant 32 bits of the resulting `Long` value are the same as the bits of this `Int` value,
     * whereas the most significant 32 bits are filled with the sign bit of this value.
     */
    @WasmInstruction(WasmInstruction.I64_EXTEND_I32_S)
    public override fun toLong(): Long =
        implementedAsIntrinsic

    /**
     * Converts this [Int] value to [Float].
     *
     * The resulting value is the closest `Float` to this `Int` value.
     * In case when this `Int` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @WasmInstruction(WasmInstruction.F32_CONVERT_I32_S)
    public override fun toFloat(): Float =
        implementedAsIntrinsic

    /**
     * Converts this [Int] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Int`.
     */
    @WasmInstruction(WasmInstruction.F64_CONVERT_I32_S)
    public override fun toDouble(): Double =
        implementedAsIntrinsic

    public inline fun equals(other: Int): Boolean =
          wasm_i32_eq(this, other).reinterpretAsBoolean()

    // TODO: Support Any? and type operators
//    public override fun equals(other: Any?): Boolean =
//        other is Int && wasm_i32_eq(this, other).reinterpretAsBoolean()

    // TODO: Implement Int.toString()
    // public override fun toString(): String

    public override inline fun hashCode(): Int =
        this

    @WasmInstruction(WasmInstruction.NOP)
    @PublishedApi
    internal fun reinterpretAsBoolean(): Boolean =
        implementedAsIntrinsic

    @PublishedApi
    @WasmInstruction(WasmInstruction.NOP)
    internal fun reinterpretAsByte(): Byte =
        implementedAsIntrinsic

    @PublishedApi
    @WasmInstruction(WasmInstruction.NOP)
    internal fun reinterpretAsShort(): Short =
        implementedAsIntrinsic

    @PublishedApi
    @WasmInstruction(WasmInstruction.NOP)
    internal fun reinterpretAsChar(): Char =
        implementedAsIntrinsic
}

/**
 * Represents a 64-bit signed integer.
 */
public class Long private constructor() : Number(), Comparable<Long> {

    @ExcludedFromCodegen
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
    public override inline operator fun compareTo(other: Long): Int =
        wasm_i64_compareTo(this, other)

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
    @WasmInstruction(WasmInstruction.I64_ADD)
    public operator fun plus(other: Long): Long =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I64_SUB)
    public operator fun minus(other: Long): Long =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I64_MUL)
    public operator fun times(other: Long): Long =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I64_DIV_S)
    public operator fun div(other: Long): Long =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.I64_REM_S)
    public operator fun rem(other: Long): Long =
        implementedAsIntrinsic

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Float): Float =
        this.toFloat() % other

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
        this.toDouble() % other

    /** Increments this value. */
    public inline operator fun inc(): Long =
        this + 1L

    /** Decrements this value. */
    public inline operator fun dec(): Long =
        this - 1L

    /** Returns this value. */
    public inline operator fun unaryPlus(): Long =
        this

    /** Returns the negative of this value. */
    public inline operator fun unaryMinus(): Long = 0L - this

//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Byte): LongRange {
//        return LongRange(this, other.toLong())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Short): LongRange  {
//        return LongRange(this, other.toLong())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Int): LongRange {
//        return LongRange(this, other.toLong())
//    }
//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Long): LongRange  {
//        return LongRange(this, other.toLong())
//    }

    /** Shifts this value left by the [bitCount] number of bits. */
    public inline infix fun shl(bitCount: Int): Long =
        wasm_i64_shl(this, bitCount.toLong())

    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with copies of the sign bit. */
    public inline infix fun shr(bitCount: Int): Long =
        wasm_i64_shr_s(this, bitCount.toLong())

    /** Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros. */
    public inline infix fun ushr(bitCount: Int): Long =
        wasm_i64_shr_u(this, bitCount.toLong())

    /** Performs a bitwise AND operation between the two values. */
    @WasmInstruction(WasmInstruction.I64_AND)
    public infix fun and(other: Long): Long =
        implementedAsIntrinsic

    /** Performs a bitwise OR operation between the two values. */
    @WasmInstruction(WasmInstruction.I64_OR)
    public infix fun or(other: Long): Long =
        implementedAsIntrinsic

    /** Performs a bitwise XOR operation between the two values. */
    @WasmInstruction(WasmInstruction.I64_XOR)
    public infix fun xor(other: Long): Long =
        implementedAsIntrinsic

    /** Inverts the bits in this value. */
    public inline fun inv(): Long =
        this.xor(-1L)

    /**
     * Converts this [Long] value to [Byte].
     *
     * If this value is in [Byte.MIN_VALUE]..[Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `Long`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `Long` value.
     */
    public override inline fun toByte(): Byte =
        this.toInt().toByte()

    /**
     * Converts this [Long] value to [Char].
     *
     * If this value is in the range of `Char` codes `Char.MIN_VALUE..Char.MAX_VALUE`,
     * the resulting `Char` code is equal to this value.
     *
     * The resulting `Char` code is represented by the least significant 16 bits of this `Long` value.
     */
    public override inline fun toChar(): Char =
        this.toInt().toChar()

    /**
     * Converts this [Long] value to [Short].
     *
     * If this value is in [Short.MIN_VALUE]..[Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `Long`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `Long` value.
     */
    public override inline fun toShort(): Short =
        this.toInt().toShort()

    /**
     * Converts this [Long] value to [Int].
     *
     * If this value is in [Int.MIN_VALUE]..[Int.MAX_VALUE], the resulting `Int` value represents
     * the same numerical value as this `Long`.
     *
     * The resulting `Int` value is represented by the least significant 32 bits of this `Long` value.
     */
    @WasmInstruction(WasmInstruction.I32_WRAP_I64)
    public override fun toInt(): Int =
        implementedAsIntrinsic

    /** Returns this value. */
    public override inline fun toLong(): Long =
        this

    /**
     * Converts this [Long] value to [Float].
     *
     * The resulting value is the closest `Float` to this `Long` value.
     * In case when this `Long` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @WasmInstruction(WasmInstruction.F32_CONVERT_I64_S)
    public override fun toFloat(): Float =
        implementedAsIntrinsic

    /**
     * Converts this [Long] value to [Double].
     *
     * The resulting value is the closest `Double` to this `Long` value.
     * In case when this `Long` value is exactly between two `Double`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @WasmInstruction(WasmInstruction.F64_CONVERT_I64_S)
    public override fun toDouble(): Double =
        implementedAsIntrinsic

    public inline fun equals(other: Long): Boolean =
        wasm_i64_eq(this, other).reinterpretAsBoolean()

    // TODO: Support Any? and type operators
//    public override fun equals(other: Any?): Boolean =
//        other is Long && wasm_i64_eq(this, other).reinterpretAsBoolean()

    // TODO: Implement Long.toString()
    // public override fun toString(): String

    public override fun hashCode(): Int =
        ((this ushr 32) xor this).toInt()
}

/**
 * Represents a single-precision 32-bit IEEE 754 floating point number.
 */
public class Float private constructor() : Number(), Comparable<Float> {

    @ExcludedFromCodegen
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
        public val POSITIVE_INFINITY: Float = 1.0f / 0.0f

        /**
         * A constant holding the negative infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NEGATIVE_INFINITY: Float = -1.0f / 0.0f

        /**
         * A constant holding the "not a number" value of Float.
         */
        public val NaN: Float = wasm_f32_const_nan()
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Byte): Int = compareTo(other.toFloat())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Short): Int = compareTo(other.toFloat())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Int): Int = compareTo(other.toFloat())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Long): Int = compareTo(other.toFloat())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: Float): Int {
        // if any of values in NaN both comparisons return false
        if (this > other) return 1
        if (this < other) return -1

        val thisBits = this.bits()
        val otherBits = other.bits()

        // Canonical NaN bits representation higher than any other value
        return thisBits.compareTo(otherBits)
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Double): Int = -other.compareTo(this)

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
    @WasmInstruction(WasmInstruction.F32_ADD)
    public operator fun plus(other: Float): Float =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.F32_SUB)
    public operator fun minus(other: Float): Float =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.F32_MUL)
    public operator fun times(other: Float): Float =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.F32_DIV)
    public operator fun div(other: Float): Float =
        implementedAsIntrinsic

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
    public operator fun rem(other: Float): Float =
        this - (wasm_f32_nearest(this / other) * other)

    /** Calculates the remainder of dividing this value by the other value. */
    public inline operator fun rem(other: Double): Double =
        this.toDouble() % other

    /** Increments this value. */
    public inline operator fun inc(): Float =
        this + 1.0f

    /** Decrements this value. */
    public inline operator fun dec(): Float =
        this - 1.0f

    /** Returns this value. */
    public inline operator fun unaryPlus(): Float = this

    /** Returns the negative of this value. */
    @WasmInstruction(WasmInstruction.F32_NEG)
    public operator fun unaryMinus(): Float =
        implementedAsIntrinsic

    /**
     * Converts this [Float] value to [Byte].
     *
     * The resulting `Byte` value is equal to `this.toInt().toByte()`.
     */
    public override inline fun toByte(): Byte = this.toInt().toByte()

    /**
     * Converts this [Float] value to [Char].
     *
     * The resulting `Char` value is equal to `this.toInt().toChar()`.
     */
    public override inline fun toChar(): Char = this.toInt().toChar()

    /**
     * Converts this [Float] value to [Short].
     *
     * The resulting `Short` value is equal to `this.toInt().toShort()`.
     */
    public override inline fun toShort(): Short = this.toInt().toShort()

    /**
     * Converts this [Float] value to [Int].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Float` value is `NaN`, [Int.MIN_VALUE] if it's less than `Int.MIN_VALUE`,
     * [Int.MAX_VALUE] if it's bigger than `Int.MAX_VALUE`.
     */
    // TODO: Implement Float.toInt()
    public override fun toInt(): Int {
        wasm_unreachable()
        return 0
    }

    /**
     * Converts this [Float] value to [Long].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Float` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
     * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
     */
    // TODO: Implement Float.toLong()
    public override fun toLong(): Long {
        wasm_unreachable()
        return 0
    }

    /** Returns this value. */
    public override inline fun toFloat(): Float =
        this

    /**
     * Converts this [Float] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `Float`.
     */
    @WasmInstruction(WasmInstruction.F64_PROMOTE_F32)
    public override fun toDouble(): Double =
        implementedAsIntrinsic

    public inline fun equals(other: Float): Boolean =
        bits() == other.bits()

    // TODO: Support Any? and type operators
//    public override fun equals(other: Any?): Boolean =
//        other is Float && this.equals(other)

    // TODO: Implement Float.toString()
    // public override fun toString(): String

    public override inline fun hashCode(): Int =
        bits()

    @PublishedApi
    @WasmInstruction(WasmInstruction.I32_REINTERPRET_F32)
    internal fun bits(): Int = implementedAsIntrinsic
}

/**
 * Represents a double-precision 64-bit IEEE 754 floating point number.
 */
public class Double private constructor() : Number(), Comparable<Double> {

    @ExcludedFromCodegen
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
        public val POSITIVE_INFINITY: Double = 1.0 / 0.0

        /**
         * A constant holding the negative infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NEGATIVE_INFINITY: Double = -1.0 / 0.0

        /**
         * A constant holding the "not a number" value of Double.
         */
        public val NaN: Double = wasm_f64_const_nan()
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Byte): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Short): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Int): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Long): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: Float): Int = compareTo(other.toDouble())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override operator fun compareTo(other: Double): Int {
        // if any of values in NaN both comparisons return false
        if (this > other) return 1
        if (this < other) return -1

        val thisBits = this.bits()
        val otherBits = other.bits()

        // Canonical NaN bits representation higher than any other value
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
    @WasmInstruction(WasmInstruction.F64_ADD)
    public operator fun plus(other: Double): Double =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.F64_SUB)
    public operator fun minus(other: Double): Double =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.F64_MUL)
    public operator fun times(other: Double): Double =
        implementedAsIntrinsic

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
    @WasmInstruction(WasmInstruction.F64_DIV)
    public operator fun div(other: Double): Double =
        implementedAsIntrinsic

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
    public operator fun rem(other: Double): Double =
        this - (wasm_f64_nearest(this / other) * other)

    /** Increments this value. */
    public inline operator fun inc(): Double =
        this + 1.0

    /** Decrements this value. */
    public inline operator fun dec(): Double =
        this - 1.0

    /** Returns this value. */
    public inline operator fun unaryPlus(): Double =
        this

    /** Returns the negative of this value. */
    @WasmInstruction(WasmInstruction.F64_NEG)
    public operator fun unaryMinus(): Double =
        implementedAsIntrinsic

    /**
     * Converts this [Double] value to [Byte].
     *
     * The resulting `Byte` value is equal to `this.toInt().toByte()`.
     */
    public override inline fun toByte(): Byte = this.toInt().toByte()

    /**
     * Converts this [Double] value to [Char].
     *
     * The resulting `Char` value is equal to `this.toInt().toChar()`.
     */
    public override inline fun toChar(): Char = this.toInt().toChar()

    /**
     * Converts this [Double] value to [Short].
     *
     * The resulting `Short` value is equal to `this.toInt().toShort()`.
     */
    public override inline fun toShort(): Short = this.toInt().toShort()

    /**
     * Converts this [Double] value to [Int].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Double` value is `NaN`, [Int.MIN_VALUE] if it's less than `Int.MIN_VALUE`,
     * [Int.MAX_VALUE] if it's bigger than `Int.MAX_VALUE`.
     */
    // TODO: Implement Double.toInt()
    public override fun toInt(): Int {
        wasm_unreachable()
        return 0
    }

    /**
     * Converts this [Double] value to [Long].
     *
     * The fractional part, if any, is rounded down.
     * Returns zero if this `Double` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
     * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
     */
    // TODO: Implement Double.toLong()
    public override fun toLong(): Long {
        wasm_unreachable()
        return 0
    }

    /**
     * Converts this [Double] value to [Float].
     *
     * The resulting value is the closest `Float` to this `Double` value.
     * In case when this `Double` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @WasmInstruction(WasmInstruction.F32_DEMOTE_F64)
    public override fun toFloat(): Float =
        implementedAsIntrinsic

    /** Returns this value. */
    public override inline fun toDouble(): Double =
        this

    public inline fun equals(other: Double): Boolean =
        this.bits() == other.bits()

    // TODO: Support Any? and type operators
//    public override fun equals(other: Any?): Boolean =
//        other is Double && this.bits() == other.bits()

    // TODO: Implement Double.toString()
    // public override fun toString(): String

    public override inline fun hashCode(): Int = bits().hashCode()

    @PublishedApi
    @WasmInstruction(WasmInstruction.I64_REINTERPRET_F64)
    internal fun bits(): Long =
        implementedAsIntrinsic
}
