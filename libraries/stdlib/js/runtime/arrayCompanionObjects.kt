/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INAPPLICABLE_OPERATOR_MODIFIER", "NOTHING_TO_INLINE")

package kotlin.js.internal

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object ArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun <T> of(vararg elements: T): Array<T> = arrayOf(*elements)
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object IntArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Int): IntArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object LongArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Long): LongArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object ShortArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Short): ShortArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object ByteArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Byte): ByteArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object CharArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Char): CharArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object BooleanArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Boolean): BooleanArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object FloatArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Float): FloatArray = elements
}

@ExperimentalStdlibApi
@kotlin.internal.UsedFromCompilerGeneratedCode
internal object DoubleArrayCompanionObject {
    @ExperimentalStdlibApi
    @kotlin.internal.InlineOnly
    inline operator fun of(vararg elements: Double): DoubleArray = elements
}
