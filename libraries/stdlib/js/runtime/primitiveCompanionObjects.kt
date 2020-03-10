/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js.internal

@JsName("DoubleCompanionObject")
internal object DoubleCompanionObject {
    @JsName("MIN_VALUE")
    const val MIN_VALUE: Double = 4.9E-324

    @JsName("MAX_VALUE")
    const val MAX_VALUE: Double = 1.7976931348623157E308

    @JsName("POSITIVE_INFINITY")
    @Suppress("DIVISION_BY_ZERO")
    const val POSITIVE_INFINITY: Double = 1.0 / 0.0

    @JsName("NEGATIVE_INFINITY")
    @Suppress("DIVISION_BY_ZERO")
    const val NEGATIVE_INFINITY: Double = -1.0 / 0.0

    @JsName("NaN")
    @Suppress("DIVISION_BY_ZERO")
    const val NaN: Double = -(0.0 / 0.0)

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 8

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 64
}

@JsName("FloatCompanionObject")
internal  object FloatCompanionObject {
    @JsName("MIN_VALUE")
    const val MIN_VALUE: Float = 1.4E-45F

    @JsName("MAX_VALUE")
    const val MAX_VALUE: Float = 3.4028235E38F

    @JsName("POSITIVE_INFINITY")
    @Suppress("DIVISION_BY_ZERO")
    const val POSITIVE_INFINITY: Float = 1.0F / 0.0F

    @JsName("NEGATIVE_INFINITY")
    @Suppress("DIVISION_BY_ZERO")
    const val NEGATIVE_INFINITY: Float = -1.0F / 0.0F

    @JsName("NaN")
    @Suppress("DIVISION_BY_ZERO")
    const val NaN: Float = -(0.0F / 0.0F)

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 4

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 32
}

@JsName("IntCompanionObject")
internal  object IntCompanionObject {
    @JsName("MIN_VALUE")
    val MIN_VALUE: Int = -2147483647 - 1

    @JsName("MAX_VALUE")
    val MAX_VALUE: Int = 2147483647

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 4

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 32
}

@JsName("LongCompanionObject")
internal  object LongCompanionObject {
    @JsName("MIN_VALUE")
    val MIN_VALUE: Long = js("Kotlin.Long.MIN_VALUE")

    @JsName("MAX_VALUE")
    val MAX_VALUE: Long = js("Kotlin.Long.MAX_VALUE")

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 8

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 64
}

@JsName("ShortCompanionObject")
internal  object ShortCompanionObject {
    @JsName("MIN_VALUE")
    val MIN_VALUE: Short = -32768

    @JsName("MAX_VALUE")
    val MAX_VALUE: Short = 32767

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 2

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 16
}

@JsName("ByteCompanionObject")
internal  object ByteCompanionObject {
    @JsName("MIN_VALUE")
    val MIN_VALUE: Byte = -128

    @JsName("MAX_VALUE")
    val MAX_VALUE: Byte = 127

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 1

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 8
}

@JsName("CharCompanionObject")
internal  object CharCompanionObject {
    @JsName("MIN_VALUE")
    public const val MIN_VALUE: Char = '\u0000'

    @JsName("MAX_VALUE")
    public const val MAX_VALUE: Char = '\uFFFF'

    @JsName("MIN_HIGH_SURROGATE")
    public const val MIN_HIGH_SURROGATE: Char = '\uD800'

    @JsName("MAX_HIGH_SURROGATE")
    public const val MAX_HIGH_SURROGATE: Char = '\uDBFF'

    @JsName("MIN_LOW_SURROGATE")
    public const val MIN_LOW_SURROGATE: Char = '\uDC00'

    @JsName("MAX_LOW_SURROGATE")
    public const val MAX_LOW_SURROGATE: Char = '\uDFFF'

    @JsName("MIN_SURROGATE")
    public const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE

    @JsName("MAX_SURROGATE")
    public const val MAX_SURROGATE: Char = MAX_LOW_SURROGATE

    @JsName("SIZE_BYTES")
    const val SIZE_BYTES = 2

    @JsName("SIZE_BITS")
    const val SIZE_BITS = 16
}

internal  object StringCompanionObject {}

internal  object BooleanCompanionObject {}

