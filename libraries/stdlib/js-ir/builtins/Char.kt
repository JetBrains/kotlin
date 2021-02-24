/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Represents a 16-bit Unicode character.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
// TODO: KT-35100
//public inline class Char internal constructor (val value: Int) : Comparable<Char> {
public class Char
@OptIn(ExperimentalUnsignedTypes::class)
@ExperimentalStdlibApi
@SinceKotlin("1.4")
constructor(code: UShort) : Comparable<Char> {
    private val value: Int = code.toInt()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override fun compareTo(other: Char): Int = value - other.value

    /** Adds the other Int value to this value resulting a Char. */
    public operator fun plus(other: Int): Char = (value + other).toChar()

    /** Subtracts the other Char value from this value resulting an Int. */
    public operator fun minus(other: Char): Int = value - other.value
    /** Subtracts the other Int value from this value resulting a Char. */
    public operator fun minus(other: Int): Char = (value - other).toChar()

    /** Increments this value. */
    public operator fun inc(): Char = (value + 1).toChar()
    /** Decrements this value. */
    public operator fun dec(): Char = (value - 1).toChar()

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Char): CharRange = CharRange(this, other)

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

    override fun equals(other: Any?): Boolean {
        @Suppress("IMPLICIT_BOXING_IN_IDENTITY_EQUALS")
        if (other === this) return true
        if (other !is Char) return false

        return this.value == other.value
    }

    override fun hashCode(): Int = value

    // TODO implicit usages of toString and valueOf must be covered in DCE
    @Suppress("JS_NAME_PROHIBITED_FOR_OVERRIDE")
    @JsName("toString")
    public override fun toString(): String {
        return js("String").fromCharCode(value).unsafeCast<String>()
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