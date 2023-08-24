/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

// Char is a magic class.
// Char is defined as a regular class, but we lower it as a value class.
// See [org.jetbrains.kotlin.ir.backend.js.utils.JsInlineClassesUtils.isClassInlineLike] for explanation.

/** Represents a 16-bit Unicode character. */
public class Char 
@kotlin.internal.LowPriorityInOverloadResolution
internal constructor(private val value: Int) : Comparable<Char> {
    @SinceKotlin("1.5")
    @WasExperimental(ExperimentalStdlibApi::class)
    public constructor(code: UShort) : this(code.toInt())

    /**
     * Compares this value with the specified value for order.
     *
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public override fun compareTo(other: Char): Int = value - other.value

    /** Adds the other Int value to this value resulting a Char. */
    @kotlin.internal.IntrinsicConstEvaluation
    public operator fun plus(other: Int): Char = (value + other).toChar()

    /** Subtracts the other Char value from this value resulting an Int. */
    @kotlin.internal.IntrinsicConstEvaluation
    public operator fun minus(other: Char): Int = value - other.value

    /** Subtracts the other Int value from this value resulting a Char. */
    @kotlin.internal.IntrinsicConstEvaluation
    public operator fun minus(other: Int): Char = (value - other).toChar()

    /**
     * Returns this value incremented by one.
     *
     * @sample samples.misc.Builtins.inc
     */
    public operator fun inc(): Char = (value + 1).toChar()

    /**
     * Returns this value decremented by one.
     *
     * @sample samples.misc.Builtins.dec
     */
    public operator fun dec(): Char = (value - 1).toChar()

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Char): CharRange = CharRange(this, other)

    /**
     * Creates a range from this value up to but excluding the specified [other] value.
     *
     * If the [other] value is less than or equal to `this` value, then the returned range is empty.
     */
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    public operator fun rangeUntil(other: Char): CharRange = this until other

    /** Returns the value of this character as a `Byte`. */
    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toByte()"))
    @DeprecatedSinceKotlin(warningSince = "1.5")
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toByte(): Byte = value.toByte()

    /** Returns the value of this character as a `Char`. */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toChar(): Char = this

    /** Returns the value of this character as a `Short`. */
    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toShort()"))
    @DeprecatedSinceKotlin(warningSince = "1.5")
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toShort(): Short = value.toShort()

    /** Returns the value of this character as a `Int`. */
    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code"))
    @DeprecatedSinceKotlin(warningSince = "1.5")
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toInt(): Int = value

    /** Returns the value of this character as a `Long`. */
    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toLong()"))
    @DeprecatedSinceKotlin(warningSince = "1.5")
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toLong(): Long = value.toLong()

    /** Returns the value of this character as a `Float`. */
    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toFloat()"))
    @DeprecatedSinceKotlin(warningSince = "1.5")
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toFloat(): Float = value.toFloat()

    /** Returns the value of this character as a `Double`. */
    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toDouble()"))
    @DeprecatedSinceKotlin(warningSince = "1.5")
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toDouble(): Double = value.toDouble()

    @kotlin.internal.IntrinsicConstEvaluation
    @Suppress("JS_NAME_PROHIBITED_FOR_OVERRIDE")
    @JsName("toString")
    // TODO implicit usages of toString and valueOf must be covered in DCE
    public override fun toString(): String {
        return js("String").fromCharCode(value).unsafeCast<String>()
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean {
        if (other !is Char) return false
        return this.value == other.value
    }

    public override fun hashCode(): Int = value

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
