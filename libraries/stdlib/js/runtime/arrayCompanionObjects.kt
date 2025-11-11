/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INAPPLICABLE_OPERATOR_MODIFIER", "NOTHING_TO_INLINE")

package kotlin.js.internal

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object ArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun <T> of(vararg elements: T): Array<T> = arrayOf(*elements)
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object IntArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Int): IntArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object LongArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Long): LongArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object ShortArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Short): ShortArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object ByteArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Byte): ByteArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object CharArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Char): CharArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object BooleanArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Boolean): BooleanArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object FloatArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Float): FloatArray = elements
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object DoubleArrayCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Double): DoubleArray = elements
}
