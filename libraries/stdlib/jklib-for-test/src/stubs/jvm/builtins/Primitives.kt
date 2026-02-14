@file:Suppress("NOTHING_TO_INLINE")
package kotlin

// Boolean is in Boolean.kt

public actual class Byte : Number(), Comparable<Byte> {
    public actual companion object {
        public actual const val MIN_VALUE: Byte = -128
        public actual const val MAX_VALUE: Byte = 127
    }
    public actual override fun toByte(): Byte = this
    public actual override fun toShort(): Short = this.toShort()
    public actual override fun toInt(): Int = this.toInt()
    public actual override fun toLong(): Long = this.toLong()
    public actual override fun toFloat(): Float = this.toFloat()
    public actual override fun toDouble(): Double = this.toDouble()
    public actual override fun toChar(): Char = this.toChar()
    public actual override operator fun compareTo(other: Byte): Int = 0

    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual operator fun unaryMinus(): Int = 0
    public actual operator fun unaryPlus(): Int = 0
    public actual operator fun plus(other: Byte): Int = 0
    public actual operator fun minus(other: Byte): Int = 0
    public actual operator fun times(other: Byte): Int = 0
    public actual operator fun div(other: Byte): Int = 0
    public actual operator fun rem(other: Byte): Int = 0
}

public actual class Short : Number(), Comparable<Short> {
    public actual companion object {
        public actual const val MIN_VALUE: Short = -32768
        public actual const val MAX_VALUE: Short = 32767
    }
    public actual override fun toByte(): Byte = this.toByte()
    public actual override fun toShort(): Short = this
    public actual override fun toInt(): Int = this.toInt()
    public actual override fun toLong(): Long = this.toLong()
    public actual override fun toFloat(): Float = this.toFloat()
    public actual override fun toDouble(): Double = this.toDouble()
    public actual override fun toChar(): Char = this.toChar()
    public actual override operator fun compareTo(other: Short): Int = 0

    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual operator fun unaryMinus(): Int = 0
    public actual operator fun unaryPlus(): Int = 0
    public actual operator fun plus(other: Short): Int = 0
    public actual operator fun minus(other: Short): Int = 0
    public actual operator fun times(other: Short): Int = 0
    public actual operator fun div(other: Short): Int = 0
    public actual operator fun rem(other: Short): Int = 0
}

public actual class Int : Number(), Comparable<Int> {
    public actual companion object {
        public actual const val MIN_VALUE: Int = -2147483648
        public actual const val MAX_VALUE: Int = 2147483647
    }
    public actual override fun toByte(): Byte = this.toByte()
    public actual override fun toShort(): Short = this.toShort()
    public actual override fun toInt(): Int = this
    public actual override fun toLong(): Long = this.toLong()
    public actual override fun toFloat(): Float = this.toFloat()
    public actual override fun toDouble(): Double = this.toDouble()
    public actual override fun toChar(): Char = this.toChar()
    public actual override operator fun compareTo(other: Int): Int = 0

    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual operator fun unaryMinus(): Int = 0
    public actual operator fun unaryPlus(): Int = 0
    public actual operator fun plus(other: Int): Int = 0
    public actual operator fun plus(other: Long): Long = 0
    public actual operator fun minus(other: Int): Int = 0
    public actual operator fun minus(other: Long): Long = 0
    public actual operator fun times(other: Int): Int = 0
    public actual operator fun times(other: Long): Long = 0
    public actual operator fun div(other: Int): Int = 0
    public actual operator fun div(other: Long): Long = 0
    public actual operator fun rem(other: Int): Int = 0
    public actual operator fun rem(other: Long): Long = 0
    
    public actual infix fun shl(bitCount: Int): Int = 0
    public actual infix fun shr(bitCount: Int): Int = 0
    public actual infix fun ushr(bitCount: Int): Int = 0
    public actual infix fun and(other: Int): Int = 0
    public actual infix fun or(other: Int): Int = 0
    public actual infix fun xor(other: Int): Int = 0
    public actual fun inv(): Int = 0
}

public actual class Long : Number(), Comparable<Long> {
    public actual companion object {
        public actual const val MIN_VALUE: Long = 1L // Dummy value
        public actual const val MAX_VALUE: Long = 9223372036854775807L
    }
    public actual override fun toByte(): Byte = this.toByte()
    public actual override fun toShort(): Short = this.toShort()
    public actual override fun toInt(): Int = this.toInt()
    public actual override fun toLong(): Long = this
    public actual override fun toFloat(): Float = this.toFloat()
    public actual override fun toDouble(): Double = this.toDouble()
    public actual override fun toChar(): Char = this.toChar()
    public actual override operator fun compareTo(other: Long): Int = 0

    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual operator fun unaryMinus(): Long = 0
    public actual operator fun unaryPlus(): Long = 0
    public actual operator fun plus(other: Long): Long = 0
    public actual operator fun minus(other: Long): Long = 0
    public actual operator fun times(other: Long): Long = 0
    public actual operator fun div(other: Long): Long = 0
    public actual operator fun rem(other: Long): Long = 0

    public actual infix fun shl(bitCount: Int): Long = 0
    public actual infix fun shr(bitCount: Int): Long = 0
    public actual infix fun ushr(bitCount: Int): Long = 0
    public actual infix fun and(other: Long): Long = 0
    public actual infix fun or(other: Long): Long = 0
    public actual infix fun xor(other: Long): Long = 0
    public actual fun inv(): Long = 0
}

public actual class Float : Number(), Comparable<Float> {
    public actual companion object {
        public actual const val MIN_VALUE: Float = 1.4E-45F
        public actual const val MAX_VALUE: Float = 3.4028235E38F
        public actual const val POSITIVE_INFINITY: Float = 0.0F // Dummy value
        public actual const val NEGATIVE_INFINITY: Float = 0.0F // Dummy value
        public actual const val NaN: Float = 0.0F // Dummy value
    }
    public actual override fun toByte(): Byte = this.toByte()
    public actual override fun toShort(): Short = this.toShort()
    public actual override fun toInt(): Int = this.toInt()
    public actual override fun toLong(): Long = this.toLong()
    public actual override fun toFloat(): Float = this
    public actual override fun toDouble(): Double = this.toDouble()
    public actual override fun toChar(): Char = this.toChar()
    public actual override operator fun compareTo(other: Float): Int = 0

    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual operator fun unaryMinus(): Float = 0.0F
    public actual operator fun unaryPlus(): Float = 0.0F
    public actual operator fun plus(other: Float): Float = 0.0F
    public actual operator fun minus(other: Float): Float = 0.0F
    public actual operator fun times(other: Float): Float = 0.0F
    public actual operator fun div(other: Float): Float = 0.0F
    public actual operator fun rem(other: Float): Float = 0.0F
}

public actual class Double : Number(), Comparable<Double> {
    public actual companion object {
        public actual const val MIN_VALUE: Double = 4.9E-324
        public actual const val MAX_VALUE: Double = 1.7976931348623157E308
        public actual const val POSITIVE_INFINITY: Double = 0.0 // Dummy value
        public actual const val NEGATIVE_INFINITY: Double = 0.0 // Dummy value
        public actual const val NaN: Double = 0.0 // Dummy value
    }
    public actual override fun toByte(): Byte = this.toByte()
    public actual override fun toShort(): Short = this.toShort()
    public actual override fun toInt(): Int = this.toInt()
    public actual override fun toLong(): Long = this.toLong()
    public actual override fun toFloat(): Float = this.toFloat()
    public actual override fun toDouble(): Double = this
    public actual override fun toChar(): Char = this.toChar()
    public actual override operator fun compareTo(other: Double): Int = 0

    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual operator fun unaryMinus(): Double = 0.0
    public actual operator fun unaryPlus(): Double = 0.0
    public actual operator fun plus(other: Double): Double = 0.0
    public actual operator fun minus(other: Double): Double = 0.0
    public actual operator fun times(other: Double): Double = 0.0
    public actual operator fun div(other: Double): Double = 0.0
    public actual operator fun rem(other: Double): Double = 0.0
}
