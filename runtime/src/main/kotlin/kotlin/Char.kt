package kotlin

/**
 * Represents a 16-bit Unicode character.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
public final class Char : Comparable<Char> {

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Char_compareTo_Char")
    external public override fun compareTo(other: Char): Int

    /** Adds the other Int value to this value resulting a Char. */
    @SymbolName("Kotlin_Char_plus_Int")
    external public operator fun plus(other: Int): Char

    /** Subtracts the other Char value from this value resulting an Int. */
    @SymbolName("Kotlin_Char_minus_Char")
    external public operator fun minus(other: Char): Int
    /** Subtracts the other Int value from this value resulting a Char. */
    @SymbolName("Kotlin_Char_minus_Int")
    external public operator fun minus(other: Int): Char

    /** Increments this value. */
    @SymbolName("Kotlin_Char_inc")
    external public operator fun inc(): Char
    /** Decrements this value. */
    @SymbolName("Kotlin_Char_dec")
    external public operator fun dec(): Char

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Char): CharRange {
        return CharRange(this, other)
    }

    /** Returns the value of this character as a `Byte`. */
    @SymbolName("Kotlin_Char_toByte")
    external public fun toByte(): Byte
    /** Returns the value of this character as a `Char`. */
    @SymbolName("Kotlin_Char_toChar")
    external public fun toChar(): Char
    /** Returns the value of this character as a `Short`. */
    @SymbolName("Kotlin_Char_toShort")
    external public fun toShort(): Short
    /** Returns the value of this character as a `Int`. */
    @SymbolName("Kotlin_Char_toInt")
    external public fun toInt(): Int
    /** Returns the value of this character as a `Long`. */
    @SymbolName("Kotlin_Char_toLong")
    external public fun toLong(): Long
    /** Returns the value of this character as a `Float`. */
    @SymbolName("Kotlin_Char_toFloat")
    external public fun toFloat(): Float
    /** Returns the value of this character as a `Double`. */
    @SymbolName("Kotlin_Char_toDouble")
    external public fun toDouble(): Double

    companion object {
        /**
         * The minimum value of a Unicode high-surrogate code unit.
         */
        public const val MIN_HIGH_SURROGATE: Char = '\uD800'

        /**
         * The maximum value of a Unicode high-surrogate code unit.
         */
        public const val MAX_HIGH_SURROGATE: Char = '\uDBFF'

        /**
         * The minimum value of a Unicode low-surrogate code unit.
         */
        public const val MIN_LOW_SURROGATE: Char = '\uDC00'

        /**
         * The maximum value of a Unicode low-surrogate code unit.
         */
        public const val MAX_LOW_SURROGATE: Char = '\uDFFF'

        /**
         * The minimum value of a Unicode surrogate code unit.
         */
        public const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE

        /**
         * The maximum value of a Unicode surrogate code unit.
         */
        public const val MAX_SURROGATE: Char = MAX_LOW_SURROGATE
    }

    // Konan-specific.
    public fun equals(other: Char): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Char && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Char_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt();
    }
}

