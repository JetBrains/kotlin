package kotlin

/**
 * Represents a 16-bit Unicode character.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
public class Char : Comparable<Char> {

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    external public override fun compareTo(other: Char): Int

    /** Adds the other Int value to this value resulting a Char. */
    external public operator fun plus(other: Int): Char

    /** Subtracts the other Char value from this value resulting an Int. */
    external public operator fun minus(other: Char): Int
    /** Subtracts the other Int value from this value resulting a Char. */
    external public operator fun minus(other: Int): Char

    /** Increments this value. */
    external public operator fun inc(): Char
    /** Decrements this value. */
    external public operator fun dec(): Char

    /** Creates a range from this value to the specified [other] value. */
    // external public operator fun rangeTo(other: Char): CharRange

    /** Returns the value of this character as a `Byte`. */
    external public fun toByte(): Byte
    /** Returns the value of this character as a `Char`. */
    external public fun toChar(): Char
    /** Returns the value of this character as a `Short`. */
    external public fun toShort(): Short
    /** Returns the value of this character as a `Int`. */
    external public fun toInt(): Int
    /** Returns the value of this character as a `Long`. */
    external public fun toLong(): Long
    /** Returns the value of this character as a `Float`. */
    external public fun toFloat(): Float
    /** Returns the value of this character as a `Double`. */
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
    @SymbolName("Kotlin_Char_toString")
    external public override fun toString(): String
}

