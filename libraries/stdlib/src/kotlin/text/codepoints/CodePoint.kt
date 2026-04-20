/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal const val MAX_CODE_POINT_VALUE = 0x10FFFF
private const val MIN_SUPPLEMENTARY_CODE_POINT_VALUE: Int = 0x10000

@ExperimentalCodePointApi
@kotlin.jvm.JvmInline
public value class CodePoint(public val code: Int) : Comparable<CodePoint> {

    init {
        // TODO: Investigate how to remove this check
        require(code in 0..MAX_CODE_POINT_VALUE) { "CodePoint code value must be in range [0..0x10FFFF], but was ${code.toString(16)}" }
    }

    public val size: Int
        get() = if (code < MIN_SUPPLEMENTARY_CODE_POINT_VALUE) 1 else 2

    public val isBasic: Boolean
        get() = code < MIN_SUPPLEMENTARY_CODE_POINT_VALUE

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

    public fun toSingleChar(): Char =
        if (isBasic)
            code.toChar()
        else
            throw IllegalArgumentException("Required a basic code point to perform the operation, but was ${code.toString(16)}")

    public fun toCharArray(): CharArray =
        if (isBasic) charArrayOf(code.toChar()) else charArrayOf(highSurrogate(), lowSurrogate())

    public fun toSurrogatePair(): Pair<Char, Char> =
        toSurrogatePair(::Pair)

    public inline fun <T> toSurrogatePair(action: (high: Char, low: Char) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
            // returnsResultOf(action)
        }
        requireIsSupplementary()
        return action(highSurrogate(), lowSurrogate())
    }

    override fun compareTo(other: CodePoint): Int = this.code compareTo other.code

    /** Adds the other Int value to this value resulting a CodePoint. */
    public operator fun plus(other: Int): CodePoint =
        (this.code + other).toCodePoint()

    /** Subtracts the other CodePoint value from this value resulting an Int. */
    public operator fun minus(other: CodePoint): Int =
        this.code - other.code

    /** Subtracts the other Int value from this value resulting a CodePoint. */
    public operator fun minus(other: Int): CodePoint =
        (this.code - other).toCodePoint()

    @kotlin.internal.InlineOnly
    public inline operator fun inc(): CodePoint = this + 1

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

    public override fun toString(): String = toCharArray().concatToString()

    public companion object {
        public fun fromChar(char: Char): CodePoint =
            char.code.toCodePoint()

        // TODO: contract for not a surrogate pair
        public fun fromSurrogatePair(high: Char, low: Char): CodePoint =
            CodePoint((((high - Char.MIN_HIGH_SURROGATE) shl 10) or (low - Char.MIN_LOW_SURROGATE)) + 0x10000)

        public fun isSurrogatePair(high: Char, low: Char): Boolean =
            high.isHighSurrogate() && low.isLowSurrogate()


        public val MIN_VALUE: CodePoint = CodePoint(0)
        public val MAX_VALUE: CodePoint = CodePoint(MAX_CODE_POINT_VALUE)
    }
}


@ExperimentalCodePointApi
public fun Int.toCodePoint(): CodePoint =
    CodePoint(this.mod(MAX_CODE_POINT_VALUE + 1))

@ExperimentalCodePointApi
public fun Char.toCodePoint(): CodePoint =
    code.toCodePoint()


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

@ExperimentalCodePointApi
@IgnorableReturnValue
public fun StringBuilder.insertCodePointAt(index: Int, value: CodePoint): StringBuilder {
    return if (value.isBasic) {
        insert(index, value.code.toChar())
    } else {
        insert(index, value.toCharArray())
    }
}

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

@ExperimentalCodePointApi
@IgnorableReturnValue
public fun StringBuilder.deleteCodePointAt(index: Int): StringBuilder {
    val current = codePointAt(index)
    return deleteRange(index, index + current.size)
}
