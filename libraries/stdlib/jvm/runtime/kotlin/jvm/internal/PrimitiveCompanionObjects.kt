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

package kotlin.jvm.internal

internal object DoubleCompanionObject {
    val MIN_VALUE: Double = java.lang.Double.MIN_VALUE
    val MAX_VALUE: Double = java.lang.Double.MAX_VALUE
    val POSITIVE_INFINITY : Double = java.lang.Double.POSITIVE_INFINITY
    val NEGATIVE_INFINITY : Double = java.lang.Double.NEGATIVE_INFINITY
    val NaN : Double = java.lang.Double.NaN
}

internal object FloatCompanionObject {
    val MIN_VALUE: Float = java.lang.Float.MIN_VALUE
    val MAX_VALUE: Float = java.lang.Float.MAX_VALUE
    val POSITIVE_INFINITY : Float = java.lang.Float.POSITIVE_INFINITY
    val NEGATIVE_INFINITY : Float = java.lang.Float.NEGATIVE_INFINITY
    val NaN : Float = java.lang.Float.NaN
}

internal object IntCompanionObject {
    const val MIN_VALUE: Int = java.lang.Integer.MIN_VALUE
    const val MAX_VALUE: Int = java.lang.Integer.MAX_VALUE
}

internal object LongCompanionObject {
    const val MIN_VALUE: Long = java.lang.Long.MIN_VALUE
    const val MAX_VALUE: Long = java.lang.Long.MAX_VALUE
}

internal object ShortCompanionObject {
    const val MIN_VALUE: Short = java.lang.Short.MIN_VALUE
    const val MAX_VALUE: Short = java.lang.Short.MAX_VALUE
}

internal object ByteCompanionObject {
    const val MIN_VALUE: Byte = java.lang.Byte.MIN_VALUE
    const val MAX_VALUE: Byte = java.lang.Byte.MAX_VALUE
}


internal object CharCompanionObject {
    const val MIN_HIGH_SURROGATE: Char = '\uD800'
    const val MAX_HIGH_SURROGATE: Char = '\uDBFF'
    const val MIN_LOW_SURROGATE: Char = '\uDC00'
    const val MAX_LOW_SURROGATE: Char = '\uDFFF'
    const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE
    const val MAX_SURROGATE: Char = MAX_LOW_SURROGATE
}

internal object StringCompanionObject {}
internal object EnumCompanionObject {}