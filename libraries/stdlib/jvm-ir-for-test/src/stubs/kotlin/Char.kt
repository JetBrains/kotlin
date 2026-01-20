@file:Suppress("NOTHING_TO_INLINE")
package kotlin

public expect class Char : Comparable<Char> {
    @Suppress("EXPECTED_PROPERTY_INITIALIZER")
    public companion object {
        public const val MIN_VALUE: Char = '\u0000'
        public const val MAX_VALUE: Char = '\uFFFF'
        public const val MIN_HIGH_SURROGATE: Char = '\uD800'
        public const val MAX_HIGH_SURROGATE: Char = '\uDBFF'
        public const val MIN_LOW_SURROGATE: Char = '\uDC00'
        public const val MAX_LOW_SURROGATE: Char = '\uDFFF'
        public const val MIN_SURROGATE: Char = '\uD800'
        public const val MAX_SURROGATE: Char = '\uDFFF'
        public const val SIZE_BYTES: Int = 2
        public const val SIZE_BITS: Int = 16
    }
    public override operator fun compareTo(other: Char): Int
    public override fun equals(other: Any?): Boolean
    public override fun hashCode(): Int
    public override fun toString(): String
    
    public fun toInt(): Int
    public fun toByte(): Byte
    public fun toShort(): Short
    public fun toLong(): Long
    public fun toFloat(): Float
    public fun toDouble(): Double
    
    public operator fun plus(other: Int): Char
    public operator fun minus(other: Char): Int
    public operator fun minus(other: Int): Char
    public operator fun inc(): Char
    public operator fun dec(): Char
}
