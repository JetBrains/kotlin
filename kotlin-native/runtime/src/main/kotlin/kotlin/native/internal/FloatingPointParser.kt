/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlin.native.internal

import kotlin.comparisons.*

/**
 * Takes a String and an integer exponent. The String should hold a positive
 * integer value (or zero). The exponent will be used to calculate the
 * floating point number by taking the positive integer the String
 * represents and multiplying by 10 raised to the power of the
 * exponent. Returns the closest double value to the real number.

 * @param s the String that will be parsed to a floating point
 * @param e an int represent the 10 to part
 * @return the double closest to the real number
 * @exception NumberFormatException if the String doesn't represent a positive integer value
 */
@GCUnsafeCall("Kotlin_native_FloatingPointParser_parseDoubleImpl")
private external fun parseDoubleImpl(s: String, e: Int): Double

/**
 * Takes a String and an integer exponent. The String should hold a positive
 * integer value (or zero). The exponent will be used to calculate the
 * floating point number by taking the positive integer the String
 * represents and multiplying by 10 raised to the power of the
 * exponent. Returns the closest float value to the real number.

 * @param s the String that will be parsed to a floating point
 * @param e an int represent the 10 to part
 * @return the float closest to the real number
 * @exception NumberFormatException if the String doesn't represent a positive integer value
 */
@GCUnsafeCall("Kotlin_native_FloatingPointParser_parseFloatImpl")
private external fun parseFloatImpl(s: String, e: Int): Float

/**
 * Used to parse a string and return either a single or double precision
 * floating point number.
 */
internal object FloatingPointParser {
    /*
     * All number with exponent larger than MAX_EXP can be treated as infinity.
     * All number with exponent smaller than MIN_EXP can be treated as zero.
     * Exponent is 10 based.
     * Eg. double's min value is 5e-324, so double "1e-325" should be parsed as 0.0
     */
    private const val FLOAT_MIN_EXP = -46
    private const val FLOAT_MAX_EXP = 38
    private const val DOUBLE_MIN_EXP = -324
    private const val DOUBLE_MAX_EXP = 308

    private data class StringExponentPair(val s: String, val e: Int, val negative: Boolean)


    /**
     * Adaptor for parsing string and returning the closest Double value to the real number in the string.
     *
     * @param string the String that will be parsed to Double
     * @return the Double number closest to the real number
     * @exception NumberFormatException if the String doesn't represent a number of type Double
     */
    fun parseDouble(string: String): Double =
            parse(string, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
                    0.0, DOUBLE_MAX_EXP, DOUBLE_MIN_EXP, ::parseDoubleImpl, HexStringParser::parseDouble)

    /**
     * Adaptor for parsing string and returning the closest Float value to the real number in the string.
     *
     * @param string the String that will be parsed to a Float
     * @return the Float number closest to the real number
     * @exception NumberFormatException if the String doesn't represent a number of type Float
     */
    fun parseFloat(string: String): Float =
            parse(string, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
                    0.0f, FLOAT_MAX_EXP, FLOAT_MIN_EXP, ::parseFloatImpl, HexStringParser::parseFloat)

    /**
     * Common method for parsing floating point number, unified for Double and Float.
     * Returns the closest Float or Double value to the real number in the string.
     *
     * @param string that will be parsed to a Float or a Double
     * @param negativeInf will be returned if the value is infinitesimal
     * @param positiveInf will be returned if the value is infinitely large
     * @param nan will be returned if the value is NaN
     * @param zero will be returned if the input value is 0.0
     * @param maxExp maximum exponent size that can be processed, in case the input string has bigger number
     *  will return Infinity
     * @param minExp minimum exponent size that can be processed, in case the input string has bigger number
     *  will return Minus Infinity
     * @param parserImpl - native method that implements parsing and processing of a float/double value
     * @param hexParserImpl - a method for parsing of the number in hex format
     * @return the Float or Double number closest to the real number
     * @exception NumberFormatException if the String doesn't represent a number of type T (Double or Float)
     */
    private inline fun <reified T : Number> parse(string: String, negativeInf: T, positiveInf: T, nan: T, zero: T,
                                                  maxExp: Int, minExp: Int, parserImpl: (String, Int) -> T,
                                                  hexParserImpl: (String) -> T): T {
        // Trim useless whitespaces.
        val s = string.trim { it <= ' ' }
        val length = s.length

        // We should not process empty string values. Such check should not be duplicated in other methods below.
        if (length == 0) {
            throw NumberFormatException(s)
        }

        // Processing for a named number ("Infinity" or "NaN").
        val last = s[length - 1]
        if (last == 'y' || last == 'N') {
            return parseNamed(s, length, negativeInf, positiveInf, nan)
        }

        // Check if the value could be a hexadecimal representation and parse it properly.
        if (parseAsHex(s)) {
            return hexParserImpl(s)
        }

        val info = initialParse(s)

        // Two kinds of situation will directly return 0.0/0.0f:
        // 1. info.s is 0;
        // 2. actual exponent is less than double or float minimum exponent .
        if ("0" == info.s || info.e + info.s.length - 1 < minExp) {
            return if (info.negative) unaryMinus(zero) else zero
        }
        // If actual exponent is larger than maximum exponent then will return infinity.
        // To prevent overflow checking twice.
        if (info.e > maxExp || info.e + info.s.length - 1 > maxExp) {
            return if (info.negative) negativeInf else positiveInf
        }

        val result = parserImpl(info.s, info.e)
        return if (info.negative) unaryMinus(result) else result
    }

    /**
     * To unify the logic of Double and Float parsing we need a common method to calculate a negative value.
     * Unfortunately in this case we need to make uncheck cast to generic type T to have a proper return type.
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> unaryMinus(value: T): T {
        return when (value) {
            is Float -> -value as T
            is Double -> -value as T
            else -> throw NumberFormatException()
        }
    }

    /**
     * Takes a String and does some initial parsing. Should return a
     * StringExponentPair containing a String with no leading or trailing white
     * space and trailing zeroes eliminated. The exponent of the
     * StringExponentPair will be used to calculate the floating point number by
     * taking the positive integer the String represents and multiplying by 10
     * raised to the power of the exponent.
     *
     * This method is not optimal, can cause performance issues. This logic can be done in one single run.
     * It also violates Kotlin coding practices and should be refactored in the future after stabilizing the logic.
     *
     * @param string the String that will be parsed to a floating point
     * @return a StringExponentPair with necessary values
     * @exception NumberFormatException if the String doesn't pass basic tests
     */
    private fun initialParse(string: String): StringExponentPair {
        var s = string
        var length = s.length
        var negative = false
        val decimal: Int
        var shift: Int
        var e = 0
        var start = 0
        var c: Char = s[length - 1]

        // Checking that the initial string ends with one of valid prefixes(D/d/F/f) and skipping it.
        if (c == 'D' || c == 'd' || c == 'F' || c == 'f') {
            length--
            if (length == 0)
                throw NumberFormatException(s)
        }

        // Getting exponent separator from the string (E/e)
        var end = maxOf(s.indexOf('E'), s.indexOf('e'))
        if (end > -1) {
            if (end + 1 == length)
                throw NumberFormatException(s)

            var exponentOffset = end + 1
            if (s[exponentOffset] == '+') {
                if (s[exponentOffset + 1] == '-') {
                    throw NumberFormatException(s)
                }
                exponentOffset++ // skip the plus sign
                if (exponentOffset == length)
                    throw NumberFormatException(s)
            }
            val strExp = s.substring(exponentOffset, length)
            try {
                e = strExp.toInt()
            } catch (ex: NumberFormatException) {
                // strExp is not empty, so there are 2 situations the exception be thrown
                // if the string is invalid we should throw exception, if the actual number
                // is out of the range of Integer, we can still parse the original number to
                // double or float.
                var ch: Char
                for (i in strExp.indices) {
                    ch = strExp[i]
                    if (ch < '0' || ch > '9') {
                        if (i == 0 && ch == '-')
                            continue
                        // ex contains the exponent substring only so throw
                        // a new exception with the correct string.
                        throw NumberFormatException(s)
                    }
                }
                e = if (strExp[0] == '-') Int.MIN_VALUE else Int.MAX_VALUE
            }
        } else {
            end = length
        }
        if (length == 0)
            throw NumberFormatException(s)

        // skipping a sign at the beginning of the string
        c = s[start]
        if (c == '-') {
            ++start
            --length
            negative = true
        } else if (c == '+') {
            ++start
            --length
        }
        if (length == 0)
            throw NumberFormatException(s)

        // Getting dot separator from the string (E/e)
        decimal = s.indexOf('.')
        s = if (decimal > -1) {
            shift = end - decimal - 1
            // Prevent e overflow, shift >= 0.
            if (e >= 0 || e - Int.MIN_VALUE > shift) {
                e -= shift
            }
            s.substring(start, decimal) + s.substring(decimal + 1, end)
        } else {
            s.substring(start, end)
        }

        // Optimal validation of characters in the string to prevent incorrect parsing.
        // Number after an exponent were validated already.
        s.forEach {
            if (it < '0' || it > '9') {
                throw NumberFormatException()
            }
        }

        length = s.length
        if (length == 0)
            throw NumberFormatException()

        end = length
        while (end > 1 && s[end - 1] == '0')
            --end

        start = 0
        while (start < end - 1 && s[start] == '0')
            start++

        if (end != length || start != 0) {
            shift = length - end
            if (e <= 0 || Int.MAX_VALUE - e > shift) {
                e += shift
            }
            s = s.substring(start, end)
        }

        // Trim the length of very small numbers, natives can only handle down to E-309.
        val APPROX_MIN_MAGNITUDE = -359
        val MAX_DIGITS = 52
        length = s.length
        if (length > MAX_DIGITS && e < APPROX_MIN_MAGNITUDE) {
            val d = minOf(APPROX_MIN_MAGNITUDE - e, length - 1)
            s = s.substring(0, length - d)
            e += d
        }

        return StringExponentPair(s, e, negative)
    }

    /*
     * Assumes the string is trimmed. This method is used for both Double and Float "named" numbers.
     * This method was needed to unify the common logic for Double and Float processing.
     * "Inifinity" and "NaN" string values will be covered by this method.
     */
    private fun <T> parseNamed(namedFloat: String, length: Int,
                               negativeInf: T, positiveInf: T, nan: T): T {
        // Valid strings are only +Nan, NaN, -Nan, +Infinity, Infinity, -Infinity.
        if (length != 3 && length != 4 && length != 8 && length != 9) {
            throw NumberFormatException()
        }

        var negative = false
        val cmpstart = when (namedFloat[0]) {
            '-' -> {
                negative = true
                1
            }
            '+' -> 1
            else -> 0
        }

        return when (namedFloat.subSequence(cmpstart, length)) {
            "Infinity" -> if (negative) negativeInf else positiveInf
            "NaN" -> nan
            else -> throw NumberFormatException()
        }
    }

    /*
     * Answers true if the string should be parsed as a hex encoding.
     * Assumes the string is trimmed.
     */
    private fun parseAsHex(s: String): Boolean {
        val length = s.length
        if (length < 2) {
            return false
        }
        var first = s[0]
        var second = s[1]
        if (first == '+' || first == '-') {
            // Move along.
            if (length < 3) {
                return false
            }
            first = second
            second = s[2]
        }
        return first == '0' && (second == 'x' || second == 'X')
    }
}