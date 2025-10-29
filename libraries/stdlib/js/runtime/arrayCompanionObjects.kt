/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js.internal

@JsName("ArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object ArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun <T> of(vararg elements: T): Array<T> = arrayOf(*elements)
}

@JsName("IntArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object IntArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Int): IntArray = elements
}

@JsName("LongArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object LongArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Long): LongArray = elements
}

@JsName("ShortArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object ShortArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Short): ShortArray = elements
}

@JsName("ByteArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object ByteArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Byte): ByteArray = elements
}

@JsName("CharArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object CharArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Char): CharArray = elements
}

@JsName("BooleanArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object BooleanArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Boolean): BooleanArray = elements
}

@JsName("FloatArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object FloatArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Float): FloatArray = elements
}

@JsName("DoubleArrayCompanionObject")
@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object DoubleArrayCompanionObject {
    @ExperimentalStdlibApi
    @JsName("of")
    operator fun of(vararg elements: Double): DoubleArray = elements
}
