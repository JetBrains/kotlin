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

package kotlin

/**
 * Represents a 8-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `byte`.
 */
public final class Byte : Number(), Comparable<Byte> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Byte can have.
         */
        public const val MIN_VALUE: Byte = -128

        /**
         * A constant holding the maximum value an instance of Byte can have.
         */
        public const val MAX_VALUE: Byte = 127
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Byte_compareTo_Byte")
    external public override operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Byte_compareTo_Short")
    external public operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Byte_compareTo_Int")
    external public operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Byte_compareTo_Long")
    external public operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Byte_compareTo_Float")
    external public operator fun compareTo(other: Float): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Byte_compareTo_Double")
    external public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Byte_plus_Byte")
    external public operator fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Byte_plus_Short")
    external public operator fun plus(other: Short): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Byte_plus_Int")
    external public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Byte_plus_Long")
    external public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Byte_plus_Float")
    external public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Byte_plus_Double")
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Byte_minus_Byte")
    external public operator fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Byte_minus_Short")
    external public operator fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Byte_minus_Int")
    external public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Byte_minus_Long")
    external public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Byte_minus_Float")
    external public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Byte_minus_Double")
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Byte_times_Byte")
    external public operator fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Byte_times_Short")
    external public operator fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Byte_times_Int")
    external public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Byte_times_Long")
    external public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Byte_times_Float")
    external public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Byte_times_Double")
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Byte_div_Byte")
    external public operator fun div(other: Byte): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Byte_div_Short")
    external public operator fun div(other: Short): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Byte_div_Int")
    external public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Byte_div_Long")
    external public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Byte_div_Float")
    external public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Byte_div_Double")
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Byte_mod_Byte")
    external public operator fun rem(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Byte_mod_Short")
    external public operator fun rem(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Byte_mod_Int")
    external public operator fun rem(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Byte_mod_Long")
    external public operator fun rem(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Byte_mod_Float")
    external public operator fun rem(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Byte_mod_Double")
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @SymbolName("Kotlin_Byte_inc")
    external public operator fun inc(): Byte
    /** Decrements this value. */
    @SymbolName("Kotlin_Byte_dec")
    external public operator fun dec(): Byte
    /** Returns this value. */
    @SymbolName("Kotlin_Byte_unaryPlus")
    external public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    @SymbolName("Kotlin_Byte_unaryMinus")
    external public operator fun unaryMinus(): Int

    /** Performs a bitwise AND operation between the two values. */
    @SymbolName("Kotlin_Byte_and_Byte")
    external public infix fun and(other: Byte): Byte
    /** Performs a bitwise OR operation between the two values. */
    @SymbolName("Kotlin_Byte_or_Byte")
    external public infix fun or(other: Byte): Byte
    /** Performs a bitwise XOR operation between the two values. */
    @SymbolName("Kotlin_Byte_xor_Byte")
    external public infix fun xor(other: Byte): Byte
    /** Inverts the bits in this value/ */
    @SymbolName("Kotlin_Byte_inv")
    external public fun inv(): Byte

    @SymbolName("Kotlin_Byte_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Byte_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Byte_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Byte_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Byte_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Byte_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Byte_toDouble")
    external public override fun toDouble(): Double

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange {
        return LongRange(this.toLong(), other.toLong())
    }

    // Konan-specific.
    public fun equals(other: Byte): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Byte && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Byte_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt()
    }
}

/**
 * Represents a 16-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `short`.
 */
public final class Short : Number(), Comparable<Short> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Short can have.
         */
        public const val MIN_VALUE: Short = -32768

        /**
         * A constant holding the maximum value an instance of Short can have.
         */
        public const val MAX_VALUE: Short = 32767
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Short_compareTo_Byte")
    external public operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Short_compareTo_Short")
    external public override operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Short_compareTo_Int")
    external public operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Short_compareTo_Long")
    external public operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Short_compareTo_Float")
    external public operator fun compareTo(other: Float): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Short_compareTo_Double")
    external public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Short_plus_Byte")
    external public operator fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Short_plus_Short")
    external public operator fun plus(other: Short): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Short_plus_Int")
    external public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Short_plus_Long")
    external public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Short_plus_Float")
    external public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Short_plus_Double")
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Short_minus_Byte")
    external public operator fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Short_minus_Short")
    external public operator fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Short_minus_Int")
    external public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Short_minus_Long")
    external public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Short_minus_Float")
    external public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Short_minus_Double")
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Short_times_Byte")
    external public operator fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Short_times_Short")
    external public operator fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Short_times_Int")
    external public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Short_times_Long")
    external public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Short_times_Float")
    external public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Short_times_Double")
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Short_div_Byte")
    external public operator fun div(other: Byte): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Short_div_Short")
    external public operator fun div(other: Short): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Short_div_Int")
    external public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Short_div_Long")
    external public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Short_div_Float")
    external public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Short_div_Double")
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Short_mod_Byte")
    external public operator fun rem(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Short_mod_Short")
    external public operator fun rem(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Short_mod_Int")
    external public operator fun rem(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Short_mod_Long")
    external public operator fun rem(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Short_mod_Float")
    external public operator fun rem(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Short_mod_Double")
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @SymbolName("Kotlin_Short_inc")
    external public operator fun inc(): Short
    /** Decrements this value. */
    @SymbolName("Kotlin_Short_dec")
    external public operator fun dec(): Short
    /** Returns this value. */
    @SymbolName("Kotlin_Short_unaryPlus")
    external public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    @SymbolName("Kotlin_Short_unaryMinus")
    external public operator fun unaryMinus(): Int

    /** Performs a bitwise AND operation between the two values. */
    @SymbolName("Kotlin_Short_and_Short")
    external public infix fun and(other: Short): Short
    /** Performs a bitwise OR operation between the two values. */
    @SymbolName("Kotlin_Short_or_Short")
    external public infix fun or(other: Short): Short
    /** Performs a bitwise XOR operation between the two values. */
    @SymbolName("Kotlin_Short_xor_Short")
    external public infix fun xor(other: Short): Short
    /** Inverts the bits in this value/ */
    @SymbolName("Kotlin_Short_inv")
    external public fun inv(): Short

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange  {
        return LongRange(this.toLong(), other.toLong())
    }

    @SymbolName("Kotlin_Short_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Short_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Short_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Short_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Short_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Short_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Short_toDouble")
    external public override fun toDouble(): Double

    // Konan-specific.
    public fun equals(other: Short): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
        other is Short && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Short_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt()
    }
}

/**
 * Represents a 32-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `int`.
 */
public final class Int : Number(), Comparable<Int> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Int can have.
         */
        public const val MIN_VALUE: Int = -2147483648

        /**
         * A constant holding the maximum value an instance of Int can have.
         */
        public const val MAX_VALUE: Int = 2147483647
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Int_compareTo_Byte")
    external public operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Int_compareTo_Short")
    external public operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Int_compareTo_Int")
    external public override operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Int_compareTo_Long")
    external public operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Int_compareTo_Float")
    external public operator fun compareTo(other: Float): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Int_compareTo_Double")
    external public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Int_plus_Byte")
    external public operator fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Int_plus_Short")
    external public operator fun plus(other: Short): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Int_plus_Int")
    external public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Int_plus_Long")
    external public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Int_plus_Float")
    external public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Int_plus_Double")
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Int_minus_Byte")
    external public operator fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Int_minus_Short")
    external public operator fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Int_minus_Int")
    external public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Int_minus_Long")
    external public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Int_minus_Float")
    external public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Int_minus_Double")
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Int_times_Byte")
    external public operator fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Int_times_Short")
    external public operator fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Int_times_Int")
    external public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Int_times_Long")
    external public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Int_times_Float")
    external public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Int_times_Double")
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Int_div_Byte")
    external public operator fun div(other: Byte): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Int_div_Short")
    external public operator fun div(other: Short): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Int_div_Int")
    external public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Int_div_Long")
    external public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Int_div_Float")
    external public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Int_div_Double")
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Int_mod_Byte")
    external public operator fun rem(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Int_mod_Short")
    external public operator fun rem(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Int_mod_Int")
    external public operator fun rem(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Int_mod_Long")
    external public operator fun rem(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Int_mod_Float")
    external public operator fun rem(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Int_mod_Double")
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @SymbolName("Kotlin_Int_inc")
    external public operator fun inc(): Int
    /** Decrements this value. */
    @SymbolName("Kotlin_Int_dec")
    external public operator fun dec(): Int
    /** Returns this value. */
    @SymbolName("Kotlin_Int_unaryPlus")
    external public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    @SymbolName("Kotlin_Int_unaryMinus")
    external public operator fun unaryMinus(): Int

    /** Shifts this value left by [bits]. */
    @SymbolName("Kotlin_Int_shl_Int")
    external public infix fun shl(bitCount: Int): Int
    /** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
    @SymbolName("Kotlin_Int_shr_Int")
    external public infix fun shr(bitCount: Int): Int
    /** Shifts this value right by [bits], filling the leftmost bits with zeros. */
    @SymbolName("Kotlin_Int_ushr_Int")
    external public infix fun ushr(bitCount: Int): Int
    /** Performs a bitwise AND operation between the two values. */
    @SymbolName("Kotlin_Int_and_Int")
    external public infix fun and(other: Int): Int
    /** Performs a bitwise OR operation between the two values. */
    @SymbolName("Kotlin_Int_or_Int")
    external public infix fun or(other: Int): Int
    /** Performs a bitwise XOR operation between the two values. */
    @SymbolName("Kotlin_Int_xor_Int")
    external public infix fun xor(other: Int): Int
    /** Inverts the bits in this value/ */
    @SymbolName("Kotlin_Int_inv")
    external public fun inv(): Int

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange  {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange {
        return LongRange(this.toLong(), other.toLong())
    }

    @SymbolName("Kotlin_Int_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Int_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Int_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Int_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Int_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Int_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Int_toDouble")
    external public override fun toDouble(): Double

    // Konan-specific.
    public fun equals(other: Int): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
         other is Int && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Int_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this
    }
}

/**
 * Represents a 64-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `long`.
 */
public final class Long : Number(), Comparable<Long> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Long can have.
         */
        public const val MIN_VALUE: Long = -9223372036854775807L - 1L

        /**
         * A constant holding the maximum value an instance of Long can have.
         */
        public const val MAX_VALUE: Long = 9223372036854775807L
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Long_compareTo_Byte")
    external public operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Long_compareTo_Short")
    external public operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Long_compareTo_Int")
    external public operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Long_compareTo_Long")
    external public override operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Long_compareTo_Float")
    external public operator fun compareTo(other: Float): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Long_compareTo_Double")
    external public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Long_plus_Byte")
    external public operator fun plus(other: Byte): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Long_plus_Short")
    external public operator fun plus(other: Short): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Long_plus_Int")
    external public operator fun plus(other: Int): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Long_plus_Long")
    external public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Long_plus_Float")
    external public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Long_plus_Double")
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Long_minus_Byte")
    external public operator fun minus(other: Byte): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Long_minus_Short")
    external public operator fun minus(other: Short): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Long_minus_Int")
    external public operator fun minus(other: Int): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Long_minus_Long")
    external public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Long_minus_Float")
    external public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Long_minus_Double")
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Long_times_Byte")
    external public operator fun times(other: Byte): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Long_times_Short")
    external public operator fun times(other: Short): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Long_times_Int")
    external public operator fun times(other: Int): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Long_times_Long")
    external public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Long_times_Float")
    external public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Long_times_Double")
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Long_div_Byte")
    external public operator fun div(other: Byte): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Long_div_Short")
    external public operator fun div(other: Short): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Long_div_Int")
    external public operator fun div(other: Int): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Long_div_Long")
    external public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Long_div_Float")
    external public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Long_div_Double")
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Long_mod_Byte")
    external public operator fun rem(other: Byte): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Long_mod_Short")
    external public operator fun rem(other: Short): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Long_mod_Int")
    external public operator fun rem(other: Int): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Long_mod_Long")
    external public operator fun rem(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Long_mod_Float")
    external public operator fun rem(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Long_mod_Double")
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @SymbolName("Kotlin_Long_inc")
    external public operator fun inc(): Long
    /** Decrements this value. */
    @SymbolName("Kotlin_Long_dec")
    external public operator fun dec(): Long
    /** Returns this value. */
    @SymbolName("Kotlin_Long_unaryPlus")
    external public operator fun unaryPlus(): Long
    /** Returns the negative of this value. */
    @SymbolName("Kotlin_Long_unaryMinus")
    external public operator fun unaryMinus(): Long

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): LongRange {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): LongRange  {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): LongRange {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange  {
        return LongRange(this, other.toLong())
    }

    /** Shifts this value left by [bits]. */
    @SymbolName("Kotlin_Long_shl_Int")
    external public infix fun shl(bitCount: Int): Long
    /** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
    @SymbolName("Kotlin_Long_shr_Int")
    external public infix fun shr(bitCount: Int): Long
    /** Shifts this value right by [bits], filling the leftmost bits with zeros. */
    @SymbolName("Kotlin_Long_ushr_Int")
    external public infix fun ushr(bitCount: Int): Long
    /** Performs a bitwise AND operation between the two values. */
    @SymbolName("Kotlin_Long_and_Long")
    external public infix fun and(other: Long): Long
    /** Performs a bitwise OR operation between the two values. */
    @SymbolName("Kotlin_Long_or_Long")
    external public infix fun or(other: Long): Long
    /** Performs a bitwise XOR operation between the two values. */
    @SymbolName("Kotlin_Long_xor_Long")
    external public infix fun xor(other: Long): Long
    /** Inverts the bits in this value/ */
    @SymbolName("Kotlin_Long_inv")
    external public fun inv(): Long

    @SymbolName("Kotlin_Long_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Long_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Long_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Long_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Long_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Long_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Long_toDouble")
    external public override fun toDouble(): Double

    // Konan-specific.
    public fun equals(other: Long): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Long && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Long_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
       return ((this ushr 32) xor this).toInt()
    }
}

/**
 * Represents a single-precision 32-bit IEEE 754 floating point number.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `float`.
 */
public final class Float : Number(), Comparable<Float> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Float.
         */
        public const val MIN_VALUE: Float = 1.40129846432481707e-45f

        /**
         * A constant holding the largest positive finite value of Float.
         */
        public const val MAX_VALUE: Float = 3.40282346638528860e+38f

        /**
         * A constant holding the positive infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val POSITIVE_INFINITY: Float = 1.0f / 0.0f

        /**
         * A constant holding the negative infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NEGATIVE_INFINITY: Float = -1.0f / 0.0f

        /**
         * A constant holding the "not a number" value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NaN: Float = 0.0f / 0.0f
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Float_compareTo_Byte")
    external public operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Float_compareTo_Short")
    external public operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Float_compareTo_Int")
    external public operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Float_compareTo_Long")
    external public operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Float_compareTo_Float")
    external public override operator fun compareTo(other: Float): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Float_compareTo_Double")
    external public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Float_plus_Byte")
    external public operator fun plus(other: Byte): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Float_plus_Short")
    external public operator fun plus(other: Short): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Float_plus_Int")
    external public operator fun plus(other: Int): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Float_plus_Long")
    external public operator fun plus(other: Long): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Float_plus_Float")
    external public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Float_plus_Double")
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Float_minus_Byte")
    external public operator fun minus(other: Byte): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Float_minus_Short")
    external public operator fun minus(other: Short): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Float_minus_Int")
    external public operator fun minus(other: Int): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Float_minus_Long")
    external public operator fun minus(other: Long): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Float_minus_Float")
    external public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Float_minus_Double")
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Float_times_Byte")
    external public operator fun times(other: Byte): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Float_times_Short")
    external public operator fun times(other: Short): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Float_times_Int")
    external public operator fun times(other: Int): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Float_times_Long")
    external public operator fun times(other: Long): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Float_times_Float")
    external public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Float_times_Double")
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Float_div_Byte")
    external public operator fun div(other: Byte): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Float_div_Short")
    external public operator fun div(other: Short): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Float_div_Int")
    external public operator fun div(other: Int): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Float_div_Long")
    external public operator fun div(other: Long): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Float_div_Float")
    external public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Float_div_Double")
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Float_mod_Byte")
    external public operator fun rem(other: Byte): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Float_mod_Short")
    external public operator fun rem(other: Short): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Float_mod_Int")
    external public operator fun rem(other: Int): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Float_mod_Long")
    external public operator fun rem(other: Long): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Float_mod_Float")
    external public operator fun rem(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Float_mod_Double")
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @SymbolName("Kotlin_Float_inc")
    external public operator fun inc(): Float
    /** Decrements this value. */
    @SymbolName("Kotlin_Float_dec")
    external public operator fun dec(): Float
    /** Returns this value. */
    @SymbolName("Kotlin_Float_unaryPlus")
    external public operator fun unaryPlus(): Float
    /** Returns the negative of this value. */
    @SymbolName("Kotlin_Float_unaryMinus")
    external public operator fun unaryMinus(): Float

    @SymbolName("Kotlin_Float_toByte")
    external public override fun toByte(): Byte
    public override fun toChar(): Char = this.toInt().toChar()
    @SymbolName("Kotlin_Float_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Float_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Float_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Float_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Float_toDouble")
    external public override fun toDouble(): Double

    // Konan-specific.
    // We intentionally provide this overload to equals() to avoid artifical boxing.
    // Note that here we intentionally deviate from JVM Kotlin, where this method would be
    // this.bits() == other.bits().
    public fun equals(other: Float): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Float && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Float_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return bits()
    }

    @SymbolName("Kotlin_Float_bits")
    external public fun bits(): Int
}

/**
 * Represents a double-precision 64-bit IEEE 754 floating point number.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `double`.
 */
public final class Double : Number(), Comparable<Double> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Double.
         */
        public const val MIN_VALUE: Double = 4.9e-324

        /**
         * A constant holding the largest positive finite value of Double.
         */
        public const val MAX_VALUE: Double = 1.7976931348623157e+308

        /**
         * A constant holding the positive infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val POSITIVE_INFINITY: Double = 1.0 / 0.0

        /**
         * A constant holding the negative infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NEGATIVE_INFINITY: Double = -1.0 / 0.0

        /**
         * A constant holding the "not a number" value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NaN: Double = 0.0 / 0.0
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Double_compareTo_Byte")
    external public operator fun compareTo(other: Byte): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Double_compareTo_Short")
    external public operator fun compareTo(other: Short): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Double_compareTo_Int")
    external public operator fun compareTo(other: Int): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Double_compareTo_Long")
    external public operator fun compareTo(other: Long): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Double_compareTo_Float")
    external public operator fun compareTo(other: Float): Int
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    @SymbolName("Kotlin_Double_compareTo_Double")
    external public override operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Double_plus_Byte")
    external public operator fun plus(other: Byte): Double
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Double_plus_Short")
    external public operator fun plus(other: Short): Double
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Double_plus_Int")
    external public operator fun plus(other: Int): Double
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Double_plus_Long")
    external public operator fun plus(other: Long): Double
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Double_plus_Float")
    external public operator fun plus(other: Float): Double
    /** Adds the other value to this value. */
    @SymbolName("Kotlin_Double_plus_Double")
    external public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Double_minus_Byte")
    external public operator fun minus(other: Byte): Double
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Double_minus_Short")
    external public operator fun minus(other: Short): Double
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Double_minus_Int")
    external public operator fun minus(other: Int): Double
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Double_minus_Long")
    external public operator fun minus(other: Long): Double
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Double_minus_Float")
    external public operator fun minus(other: Float): Double
    /** Subtracts the other value from this value. */
    @SymbolName("Kotlin_Double_minus_Double")
    external public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Double_times_Byte")
    external public operator fun times(other: Byte): Double
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Double_times_Short")
    external public operator fun times(other: Short): Double
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Double_times_Int")
    external public operator fun times(other: Int): Double
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Double_times_Long")
    external public operator fun times(other: Long): Double
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Double_times_Float")
    external public operator fun times(other: Float): Double
    /** Multiplies this value by the other value. */
    @SymbolName("Kotlin_Double_times_Double")
    external public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Double_div_Byte")
    external public operator fun div(other: Byte): Double
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Double_div_Short")
    external public operator fun div(other: Short): Double
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Double_div_Int")
    external public operator fun div(other: Int): Double
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Double_div_Long")
    external public operator fun div(other: Long): Double
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Double_div_Float")
    external public operator fun div(other: Float): Double
    /** Divides this value by the other value. */
    @SymbolName("Kotlin_Double_div_Double")
    external public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Double_mod_Byte")
    external public operator fun rem(other: Byte): Double
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Double_mod_Short")
    external public operator fun rem(other: Short): Double
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Double_mod_Int")
    external public operator fun rem(other: Int): Double
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Double_mod_Long")
    external public operator fun rem(other: Long): Double
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Double_mod_Float")
    external public operator fun rem(other: Float): Double
    /** Calculates the remainder of dividing this value by the other value. */
    @SymbolName("Kotlin_Double_mod_Double")
    external public operator fun rem(other: Double): Double

    /** Increments this value. */
    @SymbolName("Kotlin_Double_inc")
    external public operator fun inc(): Double
    /** Decrements this value. */
    @SymbolName("Kotlin_Double_dec")
    external public operator fun dec(): Double
    /** Returns this value. */
    @SymbolName("Kotlin_Double_unaryPlus")
    external public operator fun unaryPlus(): Double
    /** Returns the negative of this value. */
    @SymbolName("Kotlin_Double_unaryMinus")
    external public operator fun unaryMinus(): Double


    @SymbolName("Kotlin_Double_toByte")
    external public override fun toByte(): Byte
    public override fun toChar(): Char = this.toInt().toChar()
    @SymbolName("Kotlin_Double_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Double_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Double_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Double_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Double_toDouble")
    external public override fun toDouble(): Double

    // Konan-specific.
    // Note that here we intentionally deviate from JVM Kotlin, where this method would be
    // this.bits() == other.bits().
    public fun equals(other: Double): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Double && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Double_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return bits().hashCode()
    }

    @SymbolName("Kotlin_Double_bits")
    external public fun bits(): Long
}
