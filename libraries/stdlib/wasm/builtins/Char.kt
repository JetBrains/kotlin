/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)

package kotlin

/**
 * Represents a 16-bit Unicode character.
 *
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
public class Char private constructor() : Comparable<Char> {
    /**
     * Compares this value with the specified value for order.
     *
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override fun compareTo(other: Char): Int

    /** Adds the other Int value to this value resulting a Char. */
    public operator fun plus(other: Int): Char

    /** Subtracts the other Char value from this value resulting an Int. */
    public operator fun minus(other: Char): Int
    /** Subtracts the other Int value from this value resulting a Char. */
    public operator fun minus(other: Int): Char

    /** Increments this value. */
    public operator fun inc(): Char
    /** Decrements this value. */
    public operator fun dec(): Char

//    /** Creates a range from this value to the specified [other] value. */
//    public operator fun rangeTo(other: Char): CharRange

    /** Returns the value of this character as a `Byte`. */
    public fun toByte(): Byte
    /** Returns the value of this character as a `Char`. */
    public fun toChar(): Char
    /** Returns the value of this character as a `Short`. */
    public fun toShort(): Short
    /** Returns the value of this character as a `Int`. */
    public fun toInt(): Int
    /** Returns the value of this character as a `Long`. */
    public fun toLong(): Long
    /** Returns the value of this character as a `Float`. */
    public fun toFloat(): Float
    /** Returns the value of this character as a `Double`. */
    public fun toDouble(): Double

    companion object {
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

