/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal const val MAX_CODE_POINT_VALUE = 0x10FFFF
private const val MIN_SUPPLEMENTARY_CODE_POINT_VALUE: Int = 0x10000

/**
 * Represents a Unicode code point, which is a numerical value that uniquely identifies a character in the Unicode standard.
 *
 * A code point [code] is an integer value in the range `0x0000` to `0x10FFFF` (inclusive).
 *
 * Code points are divided into two categories:
 * - **Basic (BMP) code points**: values from `0x0` to `0xFFFF`, which can be represented by a single [Char] (16-bit value).
 * - **Supplementary code points**: values from `0x10000` to `0x10FFFF`, which require a surrogate pair of two [Char] values.
 *
 * This value class provides a type-safe representation of Unicode code points and operations for working with them,
 * including conversion to/from characters and surrogate pairs, arithmetic operations, and range creation.
 *
 * ### Creating CodePoint instances
 *
 * Code points can be created in several ways:
 * ```kotlin
 * val fromCode = CodePoint(0x1F600)                  // from integer code
 * val fromInt = 0x1F600.toCodePoint()                // from Int using extension
 * val fromChar = 'a'.toCodePoint()                   // from Char
 * val emoji = CodePoint.fromSurrogatePair(high, low) // from surrogate pair
 * ```
 *
 * ### Converting to characters
 *
 * - Use [toSingleChar] for basic code points to get a single [Char].
 * - Use [toSurrogatePair] for supplementary code points to get the surrogate pair representation.
 * - Use [toCharArray] for both basic and supplementary code point to get an array of either one or two [Char] values.
 * - Use [toString] to get a string representation of the code point consisting of either one or two [Char] values.
 *
 * ### Arithmetic and ranges
 *
 * CodePoint supports arithmetic operations and range creation:
 * ```kotlin
 * val next = codePoint + 1
 * val distance = codePoint2 - codePoint1
 * val range = 'a'.toCodePoint()..'z'.toCodePoint()
 * ```
 *
 * @property code The integer value of the Unicode code point, in the range `0x0..0x10FFFF`.
 * @throws IllegalArgumentException if the provided code value is outside the valid range.
 */
@ExperimentalCodePointApi
@kotlin.jvm.JvmInline
public value class CodePoint(public val code: Int) : Comparable<CodePoint> {

    init {
        // TODO: Investigate how to remove this check in a non-validating constructor
        require(code in 0..MAX_CODE_POINT_VALUE) { "CodePoint code value must be in range [0..0x10FFFF], but was ${code.toString(16)}" }
    }


    /**
     * The number of [Char] values needed to represent this code point.
     *
     * Returns `1` for basic (BMP) code points in the range `0x0000` to `0xFFFF`, which can be represented by a single [Char].
     * Returns `2` for supplementary code points in the range `0x10000` to `0x10FFFF`, which can be represented by a surrogate pair of two [Char] values.
     */
    public val size: Int
        get() = if (code < MIN_SUPPLEMENTARY_CODE_POINT_VALUE) 1 else 2

    /**
     * Returns `true` if this is a basic (BMP) code point that can be represented by a single [Char].
     *
     * Basic code points have code values in the range `0x0000` to `0xFFFF` (inclusive).
     */
    public val isBasic: Boolean
        get() = code < MIN_SUPPLEMENTARY_CODE_POINT_VALUE

    /**
     * Returns `true` if this is a supplementary code point that can be represented by a surrogate pair of two [Char] values.
     *
     * Supplementary code points have code values in the range `0x10000` to `0x10FFFF` (inclusive).
     */
    public val isSupplementary: Boolean
        get() = code >= MIN_SUPPLEMENTARY_CODE_POINT_VALUE

    @PublishedApi
    internal fun requireIsSupplementary() {
        if (!isSupplementary) throw IllegalArgumentException("Required a supplementary code point to perform the operation, but was ${code.toString(16)}")
    }

    @PublishedApi
    internal fun highSurrogate(): Char = Char.MIN_HIGH_SURROGATE + ((code - MIN_SUPPLEMENTARY_CODE_POINT_VALUE) ushr 10)
    @PublishedApi
    internal fun lowSurrogate(): Char = Char.MIN_LOW_SURROGATE + (code and 0x3ff)

    /**
     * Converts this basic (BMP) code point to a single [Char].
     *
     * This method is only applicable to basic code points in the range `0x0000` to `0xFFFF`.
     * For supplementary code points, use [toSurrogatePair] or [toCharArray] instead.
     *
     * @return The [Char] representation of this basic code point.
     * @throws IllegalArgumentException if this is a supplementary code point.
     * @see isBasic
     * @see toCharArray
     * @see toSurrogatePair
     */
    public fun toSingleChar(): Char =
        if (isBasic)
            code.toChar()
        else
            throw IllegalArgumentException("Required a basic code point to perform the operation, but was ${code.toString(16)}")

    /**
     * Converts this code point to a [CharArray].
     *
     * - For basic (BMP) code points, returns an array with a single [Char] with the same `code` value.
     * - For supplementary code points, returns an array with two [Char] values representing the surrogate pair encoding this codepoint..
     *
     * @return A [CharArray] containing one or two [Char] values depending on whether this is a basic or supplementary code point.
     * @see toSingleChar
     * @see toSurrogatePair
     * @see size
     */
    public fun toCharArray(): CharArray =
        if (isBasic) charArrayOf(code.toChar()) else charArrayOf(highSurrogate(), lowSurrogate())

    /**
     * Converts this supplementary code point to a surrogate pair.
     *
     * This method is only applicable to supplementary code points in the range `0x10000` to `0x10FFFF`.
     * The returned pair contains the high surrogate as the first component and the low surrogate as the second component.
     *
     * @return A [Pair] of [Char] values representing the high and low surrogates.
     * @throws IllegalArgumentException if this is a basic (BMP) code point.
     * @see isSupplementary
     * @see toCharArray
     */
    public fun toSurrogatePair(): Pair<Char, Char> =
        toSurrogatePair(::Pair)

    /**
     * Converts this supplementary code point to a surrogate pair and applies the given [action] to its components.
     *
     * This method is only applicable to supplementary code points in the range `0x10000` to `0x10FFFF`.
     * The [action] function receives the high surrogate as the first parameter and the low surrogate as the second parameter.
     *
     * @param action A function that takes the high and low surrogate [Char] values and returns a result of type [T].
     * @return The result of applying [action] to the high and low surrogates.
     * @throws IllegalArgumentException if this is a basic (BMP) code point.
     * @see isSupplementary
     * @see toSurrogatePair
     */
    public inline fun <T> toSurrogatePair(action: (high: Char, low: Char) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
            // returnsResultOf(action)
        }
        requireIsSupplementary()
        return action(highSurrogate(), lowSurrogate())
    }

    internal inline fun <T> toSurrogatePairUnchecked(action: (high: Char, low: Char) -> T): T {
        return action(highSurrogate(), lowSurrogate())
    }

    /**
     * Compares this code point with the specified [other] code point for order.
     *
     * Returns zero if this code point is equal to the [other] code point, a negative number if it's less than [other],
     * or a positive number if it's greater than [other].
     *
     * Code points are compared by their integer [code] values, so the natural ordering corresponds to the Unicode code point order.
     *
     * @param other The code point to compare with this code point.
     * @return A negative integer, zero, or a positive integer as this code point is less than, equal to, or greater than the specified [other] code point.
     */
    override fun compareTo(other: CodePoint): Int = this.code compareTo other.code

    /**
     * Adds the specified integer [other] value to this code point.
     *
     * Returns a new [CodePoint] with the code value equal to the sum of this code point's value and [other].
     * If the result is outside the valid code point range `0..0x10FFFF`, it wraps around using modulo arithmetic.
     *
     * @param other The integer value to add to this code point.
     * @return A [CodePoint] representing the sum of this code point and [other], wrapped within the valid range.
     * @see minus
     * @see inc
     */
    public operator fun plus(other: Int): CodePoint =
        (this.code + other).toCodePoint()

    /**
     * Subtracts the [other] code point from this code point and returns the difference as an [Int].
     *
     * This operation calculates the distance between two code points in the Unicode code space.
     *
     * @param other The code point to subtract from this code point.
     * @return The integer difference between this code point's [code] and the [other] code point's [code].
     * @see plus
     * @see minus
     */
    public operator fun minus(other: CodePoint): Int =
        this.code - other.code

    /**
     * Subtracts the specified integer [other] value from this code point.
     *
     * Returns a new [CodePoint] with the code value equal to the difference of this code point's [code] and [other].
     * If the result is outside the valid code point range `0..0x10FFFF`, it wraps around using modulo arithmetic.
     *
     * @param other The integer value to subtract from this code point.
     * @return A [CodePoint] representing the difference of this code point and [other], wrapped within the valid range.
     * @see plus
     * @see dec
     */
    public operator fun minus(other: Int): CodePoint =
        (this.code - other).toCodePoint()

    /**
     * Returns the next code point by incrementing this code point's value by 1.
     *
     * This is equivalent to `this + 1`. If this is the maximum code point [CodePoint.MAX_VALUE],
     * the result wraps around to [CodePoint.MIN_VALUE].
     *
     * @return A [CodePoint] with the value incremented by 1, wrapped within the valid range.
     * @see plus
     * @see dec
     */
    @kotlin.internal.InlineOnly
    public inline operator fun inc(): CodePoint = this + 1

    /**
     * Returns the previous code point by decrementing this code point's value by 1.
     *
     * This is equivalent to `this - 1`. If this is the minimum code point [CodePoint.MIN_VALUE],
     * the result wraps around to [CodePoint.MAX_VALUE].
     *
     * @return A [CodePoint] with the value decremented by 1, wrapped within the valid range.
     * @see minus
     * @see inc
     */
    @kotlin.internal.InlineOnly
    public inline operator fun dec(): CodePoint = this - 1

    /** Creates a range from this value to the specified [other] value. */
    @kotlin.internal.InlineOnly
    public inline operator fun rangeTo(other: CodePoint): CodePointRange = CodePointRange(this, other)

    /**
     * Creates a range from this value up to but excluding the specified [other] value.
     *
     * If the [other] value is less than or equal to `this` value, then the returned range is empty.
     */
    public operator fun rangeUntil(other: CodePoint): CodePointRange {
        if (other <= CodePoint.MIN_VALUE) return CodePointRange.EMPTY
        return this..(other - 1)
    }

    /**
     * Returns a string representation of this code point.
     *
     * - For basic (BMP) code points, returns a string with a single [Char] with the same `code`.
     * - For supplementary code points, returns a string with two [Char] values representing the surrogate pair encoding this code point.
     *
     * @return A [String] containing one or two [Char] values depending on whether this is a basic or supplementary code point.
     * @see toCharArray
     */
    public override fun toString(): String = toCharArray().concatToString()


    public companion object { // TODO: companion block
        /**
         * Creates a [CodePoint] from a single [Char].
         *
         * The resulting code point has the same [code] as [char] that is always in the range `0x0000` to `0xFFFF`.
         *
         * @return A [CodePoint] with the same numeric value as [char].
         * @see Char.toCodePoint
         */
        public fun fromChar(char: Char): CodePoint =
            char.code.toCodePoint()

        /**
         * Creates a [CodePoint] from a UTF-16 surrogate pair.
         *
         * The [high] character must be a high surrogate and the [low] character must be a low surrogate.
         * The returned code point is always in the supplementary range `0x10000` to `0x10FFFF`.
         *
         * @param high The high surrogate component.
         * @param low The low surrogate component.
         * @return A [CodePoint] decoded from the surrogate pair.
         * @throws IllegalArgumentException if [high] is not a high surrogate or [low] is not a low surrogate.
         * @see isSurrogatePair
         * @see toSurrogatePair
         */
        public fun fromSurrogatePair(high: Char, low: Char): CodePoint {
            require(high.isHighSurrogate()) { "high value must be in high surrogates range, but was ${high.code.toString(16)}" }
            require(low.isLowSurrogate()) { "low value must be in low surrogates range, but was ${low.code.toString(16)}" }
            return fromSurrogatePairUnchecked(high, low)
        }

        internal fun fromSurrogatePairUnchecked(high: Char, low: Char): CodePoint =
            // TODO: Use non-validating constructor
            CodePoint((((high - Char.MIN_HIGH_SURROGATE) shl 10) or (low - Char.MIN_LOW_SURROGATE)) + 0x10000)

        /**
         * Returns `true` if the supplied characters form a valid UTF-16 surrogate pair.
         *
         * @param high The potential high surrogate.
         * @param low The potential low surrogate.
         * @return `true` if [high] is a high surrogate and [low] is a low surrogate.
         */
        public fun isSurrogatePair(high: Char, low: Char): Boolean =
            high.isHighSurrogate() && low.isLowSurrogate()

        /**
         * The smallest valid Unicode code point, `U+0000`.
         */
        public val MIN_VALUE: CodePoint = CodePoint(0)

        /**
         * The largest valid Unicode code point, `U+10FFFF`.
         */
        public val MAX_VALUE: CodePoint = CodePoint(MAX_CODE_POINT_VALUE)
    }
}


/**
 * Converts this [Int] value to a [CodePoint].
 *
 * If the value is outside the valid Unicode code point range `0..0x10FFFF`,
 * it wraps around using modulo arithmetic to ensure the result is within the valid range.
 *
 * @return A [CodePoint] with the code equal to this integer value modulo `0x10FFFF + 1`.
 * @see CodePoint
 * @see Char.toCodePoint
 */
@ExperimentalCodePointApi
public fun Int.toCodePoint(): CodePoint =
    CodePoint(this.mod(MAX_CODE_POINT_VALUE + 1))

/**
 * Converts this [Char] to a [CodePoint].
 *
 * The resulting code point has the same [code] as `this` [Char] that is always in the range `0x0000` to `0xFFFF`.
 *
 * @return A [CodePoint] with the same numeric value as this character.
 * @see CodePoint.fromChar
 * @see CodePoint.toSingleChar
 */
@ExperimentalCodePointApi
public fun Char.toCodePoint(): CodePoint =
    code.toCodePoint()


/**
 * Appends the specified code point [value] to this [Appendable] and returns this instance.
 *
 * For basic (BMP) code points, appends a single [Char] value.
 * For supplementary code points, appends two [Char] values representing the surrogate pair.
 *
 * @param value The code point to append.
 * @see Appendable.append
 * @see CodePoint.toString
 * @see CodePoint.toCharArray
 */
@ExperimentalCodePointApi
@IgnorableReturnValue
public fun <T : Appendable> T.appendCodePoint(value: CodePoint): T {
    if (value.isBasic) {
        append(value.code.toChar())
    } else {
        append(value.highSurrogate())
        append(value.lowSurrogate())
    }
    return this
}

/**
 * Inserts the specified code point [value] into this [StringBuilder] at the specified [index] and returns this instance.
 *
 * For basic (BMP) code points, inserts a single [Char] value.
 * For supplementary code points, inserts two [Char] values representing the surrogate pair.
 *
 * @param index The position at which to insert the code point.
 * @param value The code point to insert.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 * @see StringBuilder.insert
 * @see appendCodePoint
 */
@ExperimentalCodePointApi
@IgnorableReturnValue
public fun StringBuilder.insertCodePointAt(index: Int, value: CodePoint): StringBuilder {
    return if (value.isBasic) {
        insert(index, value.code.toChar())
    } else {
        insert(index, value.toCharArray())
    }
}

/**
 * Replaces the code point at the specified [index] in this [StringBuilder] with the specified [value] and returns this instance.
 *
 * @param index The position of the code point to replace.
 * @param value The new code point to set at the specified position.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to the length of this string builder.
 * @see StringBuilder.set
 * @see codePointAt
 */
@ExperimentalCodePointApi
@IgnorableReturnValue
public fun StringBuilder.setCodePointAt(index: Int, value: CodePoint): StringBuilder {
    val current = codePointAt(index)
    if (value.isBasic) {
        set(index, value.code.toChar())
        if (current.isSupplementary) {
            deleteAt(index + 1)
        }
    } else {
        set(index, value.highSurrogate())
        if (current.isSupplementary) {
            set(index + 1, value.lowSurrogate())
        } else {
            insert(index + 1, value.lowSurrogate())
        }
    }
    return this
}

/**
 * Removes the [CodePoint] at the specified [index] from this [StringBuilder] and returns this instance.
 *
 * @param index The position of the code point to remove.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to the length of this string builder.
 * @see StringBuilder.deleteAt
 * @see codePointAt
 */
@ExperimentalCodePointApi
@IgnorableReturnValue
public fun StringBuilder.deleteCodePointAt(index: Int): StringBuilder {
    val current = codePointAt(index)
    return deleteRange(index, index + current.size)
}
