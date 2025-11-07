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
