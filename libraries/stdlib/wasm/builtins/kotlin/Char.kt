/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin

import kotlin.wasm.internal.*

/**
 * Represents a 16-bit Unicode character.
 *
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
@WasmAutoboxed
@Suppress("NOTHING_TO_INLINE")
public class Char private constructor(public val value: Char) : Comparable<Char> {
    /**
     * Compares this value with the specified value for order.
     *
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override fun compareTo(other: Char): Int =
        wasm_i32_compareTo(this.toInt(), other.toInt())

    public override fun equals(other: Any?): Boolean {
        if (other is Char)
            return this === (other as Char)
        return false
    }

    /** Adds the other Int value to this value resulting a Char. */
    public inline operator fun plus(other: Int): Char =
        (this.toInt() + other).toChar()

    /** Subtracts the other Char value from this value resulting an Int. */
    public inline operator fun minus(other: Char): Int =
        (this.toInt() - other.toInt())

    /** Subtracts the other Int value from this value resulting a Char. */
    public inline operator fun minus(other: Int): Char =
        (this.toInt() - other).toChar()

    /**
     * Returns this value incremented by one.
     *
     * @sample samples.misc.Builtins.inc
     */
    public inline operator fun inc(): Char =
        (this.toInt() + 1).toChar()

    /**
     * Returns this value decremented by one.
     *
     * @sample samples.misc.Builtins.dec
     */
    public inline operator fun dec(): Char =
        (this.toInt() - 1).toChar()

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Char): CharRange =
        CharRange(this, other)

    /** Returns the value of this character as a `Byte`. */
    public inline fun toByte(): Byte =
        this.toInt().toByte()

    /** Returns the value of this character as a `Char`. */
    public inline fun toChar(): Char =
        this

    /** Returns the value of this character as a `Short`. */
    public inline fun toShort(): Short =
        this.toInt().toShort()

    /** Returns the value of this character as a `Int`. */
    @WasmNoOpCast
    public fun toInt(): Int =
        implementedAsIntrinsic

    /** Returns the value of this character as a `Long`. */
    public inline fun toLong(): Long =
        this.toInt().toLong()

    /** Returns the value of this character as a `Float`. */
    public inline fun toFloat(): Float =
        this.toInt().toFloat()

    /** Returns the value of this character as a `Double`. */
    public inline fun toDouble(): Double =
        this.toInt().toDouble()

    override fun toString(): String =
        String.unsafeFromCharArray(WasmCharArray(1).also { it.set(0, this) })

    override fun hashCode(): Int =
        this.toInt().hashCode()

    public companion object {
        /**
         * The minimum value of a character code unit.
         */
        @SinceKotlin("1.3")
        public const val MIN_VALUE: Char = '\u0000'

        /**
         * The maximum value of a character code unit.
         */
        @SinceKotlin("1.3")
        public const val MAX_VALUE: Char = '\uFFFF'

        /**
         * The minimum value of a Unicode high-surrogate code unit.
         */
        public const val MIN_HIGH_SURROGATE: Char = '\uD800'

        /**
         * The maximum value of a Unicode high-surrogate code unit.
         */
        public const val MAX_HIGH_SURROGATE: Char = '\uDBFF'

        /**
         * The minimum value of a Unicode low-surrogate code unit.
         */
        public const val MIN_LOW_SURROGATE: Char = '\uDC00'

        /**
         * The maximum value of a Unicode low-surrogate code unit.
         */
        public const val MAX_LOW_SURROGATE: Char = '\uDFFF'

        /**
         * The minimum value of a Unicode surrogate code unit.
         */
        public const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE

        /**
         * The maximum value of a Unicode surrogate code unit.
         */
        public const val MAX_SURROGATE: Char = MAX_LOW_SURROGATE

        /**
         * The minimum value of a supplementary code point, `\u0x10000`.
         */
        internal const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

        /**
         * The minimum value of a Unicode code point.
         */
        internal const val MIN_CODE_POINT = 0x000000

        /**
         * The maximum value of a Unicode code point.
         */
        internal const val MAX_CODE_POINT = 0X10FFFF

        /**
         * The minimum radix available for conversion to and from strings.
         */
        internal const val MIN_RADIX: Int = 2

        /**
         * The maximum radix available for conversion to and from strings.
         */
        internal const val MAX_RADIX: Int = 36

        /**
         * The number of bytes used to represent a Char in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = 2

        /**
         * The number of bits used to represent a Char in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = 16
    }
}
