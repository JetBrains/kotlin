@file:Suppress("NOTHING_TO_INLINE")
package kotlin

// Boolean is defined in Boolean.kt

public expect class Byte : Number, Comparable<Byte> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Byte = -128
        public const val MAX_VALUE: Byte = 127
    }
    public override fun toByte(): Byte
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
    public override fun toChar(): Char
    public override operator fun compareTo(other: Byte): Int

    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String

    public operator fun unaryMinus(): Int
    public operator fun unaryPlus(): Int
    public operator fun plus(other: Byte): Int
    public operator fun minus(other: Byte): Int
    public operator fun times(other: Byte): Int
    public operator fun div(other: Byte): Int
    public operator fun rem(other: Byte): Int
}

public expect class Short : Number, Comparable<Short> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Short = -32768
        public const val MAX_VALUE: Short = 32767
    }
    public override fun toByte(): Byte
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
    public override fun toChar(): Char
    public override operator fun compareTo(other: Short): Int

    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String

    public operator fun unaryMinus(): Int
    public operator fun unaryPlus(): Int
    public operator fun plus(other: Short): Int
    public operator fun minus(other: Short): Int
    public operator fun times(other: Short): Int
    public operator fun div(other: Short): Int
    public operator fun rem(other: Short): Int
}

public expect class Int : Number, Comparable<Int> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Int = -2147483648
        public const val MAX_VALUE: Int = 2147483647
    }
    public override fun toByte(): Byte
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
    public override fun toChar(): Char
    public override operator fun compareTo(other: Int): Int

    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String

    public operator fun unaryMinus(): Int
    public operator fun unaryPlus(): Int
    public operator fun plus(other: Int): Int
    public operator fun plus(other: Long): Long
    public operator fun minus(other: Int): Int
    public operator fun minus(other: Long): Long
    public operator fun times(other: Int): Int
    public operator fun times(other: Long): Long
    public operator fun div(other: Int): Int
    public operator fun div(other: Long): Long
    public operator fun rem(other: Int): Int
    public operator fun rem(other: Long): Long
    
    public infix fun shl(bitCount: Int): Int
    public infix fun shr(bitCount: Int): Int
    public infix fun ushr(bitCount: Int): Int
    public infix fun and(other: Int): Int
    public infix fun or(other: Int): Int
    public infix fun xor(other: Int): Int
    public fun inv(): Int
}

public expect class Long : Number, Comparable<Long> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Long = 1L // Dummy value
        public const val MAX_VALUE: Long = 9223372036854775807L
    }
    public override fun toByte(): Byte
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
    public override fun toChar(): Char
    public override operator fun compareTo(other: Long): Int

    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String

    public operator fun unaryMinus(): Long
    public operator fun unaryPlus(): Long
    public operator fun plus(other: Long): Long
    public operator fun minus(other: Long): Long
    public operator fun times(other: Long): Long
    public operator fun div(other: Long): Long
    public operator fun rem(other: Long): Long

    public infix fun shl(bitCount: Int): Long
    public infix fun shr(bitCount: Int): Long
    public infix fun ushr(bitCount: Int): Long
    public infix fun and(other: Long): Long
    public infix fun or(other: Long): Long
    public infix fun xor(other: Long): Long
    public fun inv(): Long
}

public expect class Float : Number, Comparable<Float> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Float = 1.4E-45F
        public const val MAX_VALUE: Float = 3.4028235E38F
        public const val POSITIVE_INFINITY: Float = 0.0F // Dummy value
        public const val NEGATIVE_INFINITY: Float = 0.0F // Dummy value
        public const val NaN: Float = 0.0F // Dummy value
    }
    public override fun toByte(): Byte
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
    public override fun toChar(): Char
    public override operator fun compareTo(other: Float): Int

    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String

    public operator fun unaryMinus(): Float
    public operator fun unaryPlus(): Float
    public operator fun plus(other: Float): Float
    public operator fun minus(other: Float): Float
    public operator fun times(other: Float): Float
    public operator fun div(other: Float): Float
    public operator fun rem(other: Float): Float
}

public expect class Double : Number, Comparable<Double> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Double = 4.9E-324
        public const val MAX_VALUE: Double = 1.7976931348623157E308
        public const val POSITIVE_INFINITY: Double = 0.0 // Dummy value
        public const val NEGATIVE_INFINITY: Double = 0.0 // Dummy value
        public const val NaN: Double = 0.0 // Dummy value
    }
    public override fun toByte(): Byte
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
    public override fun toChar(): Char
    public override operator fun compareTo(other: Double): Int

    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String

    public operator fun unaryMinus(): Double
    public operator fun unaryPlus(): Double
    public operator fun plus(other: Double): Double
    public operator fun minus(other: Double): Double
    public operator fun times(other: Double): Double
    public operator fun div(other: Double): Double
    public operator fun rem(other: Double): Double
}
