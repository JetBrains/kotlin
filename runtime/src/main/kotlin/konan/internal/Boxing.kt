package konan.internal

// TODO: cache some boxes.

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

fun boxChar(value: Char) = CharBox(value)

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

fun boxByte(value: Byte) = ByteBox(value)

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

fun boxShort(value: Short) = ShortBox(value)

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

fun boxInt(value: Int) = IntBox(value)

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

fun boxLong(value: Long) = LongBox(value)

class FloatBox(val value: Float) : Number(), Comparable<Float> {
    override fun equals(other: Any?): Boolean {
        if (other !is FloatBox) {
            return false
        }

        return this.value == other.value
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

fun boxFloat(value: Float) = FloatBox(value)

class DoubleBox(val value: Double) : Number(), Comparable<Double> {
    override fun equals(other: Any?): Boolean {
        if (other !is DoubleBox) {
            return false
        }

        return this.value == other.value
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

fun boxDouble(value: Double) = DoubleBox(value)
