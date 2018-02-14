/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package konan.internal

class BooleanBox(val value: Boolean) : Comparable<Boolean> {
    override fun equals(other: Any?): Boolean {
        if (other !is BooleanBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Boolean): Int = value.compareTo(other)
}

@ExportForCppRuntime("Kotlin_boxBoolean")
fun boxBoolean(value: Boolean) = BooleanBox(value)

class CharBox(val value: Char) : Comparable<Char> {
    override fun equals(other: Any?): Boolean {
        if (other !is CharBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Char): Int = value.compareTo(other)
}

@ExportForCppRuntime("Kotlin_boxChar")
fun boxChar(value: Char) = if (inCharBoxCache(value)) {
    getCachedCharBox(value)
} else {
    CharBox(value)
}

class ByteBox(val value: Byte) : Number(), Comparable<Byte> {
    override fun equals(other: Any?): Boolean {
        if (other !is ByteBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Byte): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

@ExportForCppRuntime("Kotlin_boxByte")
fun boxByte(value: Byte) = if (inByteBoxCache(value)) {
    getCachedByteBox(value)
} else {
    ByteBox(value)
}

class ShortBox(val value: Short) : Number(), Comparable<Short> {
    override fun equals(other: Any?): Boolean {
        if (other !is ShortBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Short): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

@ExportForCppRuntime("Kotlin_boxShort")
fun boxShort(value: Short) = if (inShortBoxCache(value)) {
    getCachedShortBox(value)
} else {
    ShortBox(value)
}

class IntBox(val value: Int) : Number(), Comparable<Int> {
    override fun equals(other: Any?): Boolean {
        if (other !is IntBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Int): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

@ExportForCppRuntime("Kotlin_boxInt")
fun boxInt(value: Int) = if (inIntBoxCache(value)) {
    getCachedIntBox(value)
} else {
    IntBox(value)
}

class LongBox(val value: Long) : Number(), Comparable<Long> {
    override fun equals(other: Any?): Boolean {
        if (other !is LongBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Long): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

@ExportForCppRuntime("Kotlin_boxLong")
fun boxLong(value: Long) = if (inLongBoxCache(value)) {
    getCachedLongBox(value)
} else {
    LongBox(value)
}

class FloatBox(val value: Float) : Number(), Comparable<Float> {
    override fun equals(other: Any?): Boolean {
        if (other !is FloatBox) {
            return false
        }

        return this.value.equals(other.value)
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Float): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

@ExportForCppRuntime("Kotlin_boxFloat")
fun boxFloat(value: Float) = FloatBox(value)

class DoubleBox(val value: Double) : Number(), Comparable<Double> {
    override fun equals(other: Any?): Boolean {
        if (other !is DoubleBox) {
            return false
        }

        return this.value.equals(other.value)
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Double): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

@ExportForCppRuntime("Kotlin_boxDouble")
fun boxDouble(value: Double) = DoubleBox(value)
