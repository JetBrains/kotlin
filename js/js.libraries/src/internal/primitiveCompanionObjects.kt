/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.js.internal

private object DoubleCompanionObject {
    val MIN_VALUE: Double = js("Number.MIN_VALUE")
    val MAX_VALUE: Double = js("Number.MAX_VALUE")
    val POSITIVE_INFINITY: Double = js("Number.POSITIVE_INFINITY")
    val NEGATIVE_INFINITY: Double = js("Number.NEGATIVE_INFINITY")
    val NaN: Double = js("Number.NaN")
}

private object FloatCompanionObject {
    val MIN_VALUE: Float = js("Number.MIN_VALUE")
    val MAX_VALUE: Float = js("Number.MAX_VALUE")
    val POSITIVE_INFINITY: Float = js("Number.POSITIVE_INFINITY")
    val NEGATIVE_INFINITY: Float = js("Number.NEGATIVE_INFINITY")
    val NaN: Float = js("Number.NaN")
}

private object IntCompanionObject {
    val MIN_VALUE: Int = -2147483647 - 1
    val MAX_VALUE: Int = 2147483647
}

private object LongCompanionObject {
    val MIN_VALUE: Long = js("Kotlin.Long.MIN_VALUE")
    val MAX_VALUE: Long = js("Kotlin.Long.MAX_VALUE")
}

private object ShortCompanionObject {
    val MIN_VALUE: Short = -32768
    val MAX_VALUE: Short = 32767
}

private object ByteCompanionObject {
    val MIN_VALUE: Byte = -128
    val MAX_VALUE: Byte = 127
}

private object CharCompanionObject {
    public const val MIN_HIGH_SURROGATE: Char = '\uD800'
    public const val MAX_HIGH_SURROGATE: Char = '\uDBFF'
    public const val MIN_LOW_SURROGATE: Char = '\uDC00'
    public const val MAX_LOW_SURROGATE: Char = '\uDFFF'
    public const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE
    public const val MAX_SURROGATE: Char = MAX_LOW_SURROGATE
}

private object StringCompanionObject {}

private object EnumCompanionObject {}
