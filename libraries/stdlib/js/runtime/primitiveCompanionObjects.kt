/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js.internal

@JsName("DoubleCompanionObject")
private object DoubleCompanionObject {
    @JsName("MIN_VALUE")
    val MIN_VALUE: Double = js("Number.MIN_VALUE")

    @JsName("MAX_VALUE")
    val MAX_VALUE: Double = js("Number.MAX_VALUE")

    @JsName("POSITIVE_INFINITY")
    val POSITIVE_INFINITY: Double = js("Number.POSITIVE_INFINITY")

    @JsName("NEGATIVE_INFINITY")
    val NEGATIVE_INFINITY: Double = js("Number.NEGATIVE_INFINITY")

    @JsName("NaN")
    val NaN: Double = js("Number.NaN")
}

@JsName("FloatCompanionObject")
private object FloatCompanionObject {
    @JsName("MIN_VALUE")
    val MIN_VALUE: Float = js("Number.MIN_VALUE")

    @JsName("MAX_VALUE")
    val MAX_VALUE: Float = js("Number.MAX_VALUE")

    @JsName("POSITIVE_INFINITY")
    val POSITIVE_INFINITY: Float = js("Number.POSITIVE_INFINITY")

    @JsName("NEGATIVE_INFINITY")
    val NEGATIVE_INFINITY: Float = js("Number.NEGATIVE_INFINITY")

    @JsName("NaN")
    val NaN: Float = js("Number.NaN")
}

@JsName("IntCompanionObject")
private object IntCompanionObject {
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
private object LongCompanionObject {
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
private object ShortCompanionObject {
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
private object ByteCompanionObject {
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
private object CharCompanionObject {
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

private object StringCompanionObject {}

private object BooleanCompanionObject {}

