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

// TODO: support other boxes.