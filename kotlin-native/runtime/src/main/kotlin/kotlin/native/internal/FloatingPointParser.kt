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
@SymbolName("Kotlin_native_FloatingPointParser_parseDoubleImpl")
@GCCritical
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
@SymbolName("Kotlin_native_FloatingPointParser_parseFloatImpl")
@GCCritical
private external fun parseFloatImpl(s: String, e: Int): Float

/**
 * Used to parse a string and return either a single or double precision
 * floating point number.
 */
object FloatingPointParser {
    /*
     * All number with exponent larger than MAX_EXP can be treated as infinity.
     * All number with exponent smaller than MIN_EXP can be treated as zero.
     * Exponent is 10 based.
     * Eg. double's min value is 5e-324, so double "1e-325" should be parsed as 0.0
     */
    private val FLOAT_MIN_EXP = -46
    private val FLOAT_MAX_EXP = 38
    private val DOUBLE_MIN_EXP = -324
    private val DOUBLE_MAX_EXP = 308

    private class StringExponentPair(var s: String, var e: Int, var negative: Boolean)

    /**
     * Takes a String and does some initial parsing. Should return a
     * StringExponentPair containing a String with no leading or trailing white
     * space and trailing zeroes eliminated. The exponent of the
     * StringExponentPair will be used to calculate the floating point number by
     * taking the positive integer the String represents and multiplying by 10
     * raised to the power of the exponent.

     * @param string the String that will be parsed to a floating point
     * @return a StringExponentPair with necessary values
     * @exception NumberFormatException if the String doesn't pass basic tests
     */
    private fun initialParse(string: String): StringExponentPair {
        var s = string
        var length = s.length
        var negative = false
        var c: Char
        var start: Int
        var end: Int
        val decimal: Int
        var shift: Int
        var e = 0

        start = 0
        if (length == 0)
            throw NumberFormatException(s)

        c = s[length - 1]
        if (c == 'D' || c == 'd' || c == 'F' || c == 'f') {
            length--
            if (length == 0)
                throw NumberFormatException(s)
        }

        end = maxOf(s.indexOf('E'), s.indexOf('e'))
        if (end > -1) {
            if (end + 1 == length)
                throw NumberFormatException(s)

            var exponent_offset = end + 1
            if (s[exponent_offset] == '+') {
                if (s[exponent_offset + 1] == '-') {
                    throw NumberFormatException(s)
                }
                exponent_offset++ // skip the plus sign
                if (exponent_offset == length)
                    throw NumberFormatException(s)
            }
            val strExp = s.substring(exponent_offset, length)
            try {
                e = strExp.toInt()
            } catch (ex: NumberFormatException) {
                // strExp is not empty, so there are 2 situations the exception be thrown
                // if the string is invalid we should throw exception, if the actual number
                // is out of the range of Integer, we can still parse the original number to
                // double or float.
                var ch: Char
                for (i in 0..strExp.length - 1) {
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

        decimal = s.indexOf('.')
        if (decimal > -1) {
            shift = end - decimal - 1
            // Prevent e overflow, shift >= 0.
            if (e >= 0 || e - Int.MIN_VALUE > shift) {
                e -= shift
            }
            s = s.substring(start, decimal) + s.substring(decimal + 1, end)
        } else {
            s = s.substring(start, end)
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
     * Assumes the string is trimmed.
     */
    private fun parseDoubleName(namedDouble: String, length: Int): Double {
        // Valid strings are only +Nan, NaN, -Nan, +Infinity, Infinity, -Infinity.
        if (length != 3 && length != 4 && length != 8 && length != 9) {
            throw NumberFormatException()
        }

        var negative = false
        var cmpstart = 0
        when (namedDouble[0]) {
            '-' -> {
                negative = true
                cmpstart = 1
            }
            '+' -> cmpstart = 1
        }

        if (namedDouble.regionMatches(cmpstart, "Infinity", 0, 8, ignoreCase = false)) {
            return if (negative)
                Double.NEGATIVE_INFINITY
            else
                Double.POSITIVE_INFINITY
        }

        if (namedDouble.regionMatches(cmpstart, "NaN", 0, 3, ignoreCase = false)) {
            return Double.NaN
        }

        throw NumberFormatException()
    }

    /*
     * Assumes the string is trimmed.
     */
    private fun parseFloatName(namedFloat: String, length: Int): Float {
        // Valid strings are only +Nan, NaN, -Nan, +Infinity, Infinity, -Infinity.
        if (length != 3 && length != 4 && length != 8 && length != 9) {
            throw NumberFormatException()
        }

        var negative = false
        var cmpstart = 0
        when (namedFloat[0]) {
            '-' -> {
                negative = true
                cmpstart = 1
            }
            '+' -> cmpstart = 1
        }

        if (namedFloat.regionMatches(cmpstart, "Infinity", 0, 8, ignoreCase = false)) {
            return if (negative) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
        }

        if (namedFloat.regionMatches(cmpstart, "NaN", 0, 3, ignoreCase = false)) {
            return Float.NaN
        }

        throw NumberFormatException()
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

    /**
     * Returns the closest double value to the real number in the string.
     *
     * @param string the String that will be parsed to a floating point
     * @return the double closest to the real number
     * @exception NumberFormatException if the String doesn't represent a double
     */
    fun parseDouble(string: String): Double {
        var s = string
        s = s.trim { it <= ' ' }
        val length = s.length

        if (length == 0) {
            throw NumberFormatException(s)
        }

        // See if this could be a named double.
        val last = s[length - 1]
        if (last == 'y' || last == 'N') {
            return parseDoubleName(s, length)
        }

        // See if it could be a hexadecimal representation.
        if (parseAsHex(s)) {
            return HexStringParser.parseDouble(s)
        }

        val info = initialParse(s)

        // Two kinds of situation will directly return 0.0:
        // 1. info.s is 0;
        // 2. actual exponent is less than Double.MIN_EXPONENT.
        if ("0" == info.s || info.e + info.s.length - 1 < DOUBLE_MIN_EXP) {
            return if (info.negative) -0.0 else 0.0
        }
        // If actual exponent is larger than Double.MAX_EXPONENT, return infinity.
        // Prevent overflow, check twice.
        if (info.e > DOUBLE_MAX_EXP || info.e + info.s.length - 1 > DOUBLE_MAX_EXP) {
            return if (info.negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        }
        var result = parseDoubleImpl(info.s, info.e)
        if (info.negative)
            result = -result

        return result
    }

    /**
     * Returns the closest float value to the real number in the string.
     *
     * @param s the String that will be parsed to a floating point
     * @return the float closest to the real number
     * @exception NumberFormatException if the String doesn't represent a float
     */
    fun parseFloat(string: String): Float {
        var s = string
        s = s.trim { it <= ' ' }
        val length = s.length

        if (length == 0) {
            throw NumberFormatException(s)
        }

        // See if this could be a named float.
        val last = s[length - 1]
        if (last == 'y' || last == 'N') {
            return parseFloatName(s, length)
        }

        // See if it could be a hexadecimal representation.
        if (parseAsHex(s)) {
            return HexStringParser.parseFloat(s)
        }

        val info = initialParse(s)

        // Two kinds of situation will directly return 0.0f.
        // 1. info.s is 0;
        // 2. actual exponent is less than Float.MIN_EXPONENT.
        if ("0" == info.s || info.e + info.s.length - 1 < FLOAT_MIN_EXP) {
            return if (info.negative) -0.0f else 0.0f
        }
        // If actual exponent is larger than Float.MAX_EXPONENT, return infinity.
        // Prevent overflow, check twice.
        if (info.e > FLOAT_MAX_EXP || info.e + info.s.length - 1 > FLOAT_MAX_EXP) {
            return if (info.negative) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
        }
        var result = parseFloatImpl(info.s, info.e)
        if (info.negative)
            result = -result

        return result
    }
}