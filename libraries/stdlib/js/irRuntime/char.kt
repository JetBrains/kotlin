/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Represents a 16-bit Unicode character.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
public class Char(value: Int) : Comparable<Char> {

    private val value = value and 0xFFFF

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override fun compareTo(other: Char): Int = value - other.value

    /** Adds the other Int value to this value resulting a Char. */
    public operator fun plus(other: Int): Char = Char(value + other)

    /** Subtracts the other Char value from this value resulting an Int. */
    public operator fun minus(other: Char): Int = value - other.value
    /** Subtracts the other Int value from this value resulting a Char. */
    public operator fun minus(other: Int): Char = Char(value - other)

    /** Increments this value. */
    public operator fun inc(): Char = Char(value + 1)
    /** Decrements this value. */
    public operator fun dec(): Char = Char(value - 1)

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Char): CharRange = null!! // TODO

    /** Returns the value of this character as a `Byte`. */
    public fun toByte(): Byte = value.toByte()
    /** Returns the value of this character as a `Char`. */
    public fun toChar(): Char = this
    /** Returns the value of this character as a `Short`. */
    public fun toShort(): Short = value.toShort()
    /** Returns the value of this character as a `Int`. */
    public fun toInt(): Int = value
    /** Returns the value of this character as a `Long`. */
    public fun toLong(): Long = value.toLong()
    /** Returns the value of this character as a `Float`. */
    public fun toFloat(): Float = value.toFloat()
    /** Returns the value of this character as a `Double`. */
    public fun toDouble(): Double = value.toDouble()

    override fun equals(other: Any?): Boolean = other is Char && value == other.value

    override fun hashCode(): Int = value

    override fun toString(): String {
        val value = value
        return js("String.fromCharCode(value)").unsafeCast<String>()
    }

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
    }

}