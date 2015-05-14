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

private object DoubleCompanionObject : FloatingPointConstants<Double> {
    override val POSITIVE_INFINITY: Double = js("Number.POSITIVE_INFINITY")
    override val NEGATIVE_INFINITY: Double = js("Number.NEGATIVE_INFINITY")
    override val NaN: Double = js("Number.NaN")
}

private object FloatCompanionObject : FloatingPointConstants<Float> {
    override val POSITIVE_INFINITY : Float = js("Number.POSITIVE_INFINITY")
    override val NEGATIVE_INFINITY : Float = js("Number.NEGATIVE_INFINITY")
    override val NaN : Float = js("Number.NaN")
}

private object IntCompanionObject : IntegerConstants<Int> {
    override val MIN_VALUE: Int = -0x80000000
    override val MAX_VALUE: Int =  0x7FFFFFFF
}

private object LongCompanionObject : IntegerConstants<Long> {
    override val MIN_VALUE: Long = js("Kotlin.Long.MIN_VALUE")
    override val MAX_VALUE: Long = js("Kotlin.Long.MAX_VALUE")
}

private object ShortCompanionObject : IntegerConstants<Short> {
    override val MIN_VALUE: Short = -0x8000
    override val MAX_VALUE: Short = 0x7FFF
}

private object ByteCompanionObject : IntegerConstants<Byte> {
    override val MIN_VALUE: Byte = -0x80
    override val MAX_VALUE: Byte = 0x7F
}

private object CharCompanionObject {}

private object StringCompanionObject {}
private object EnumCompanionObject {}
