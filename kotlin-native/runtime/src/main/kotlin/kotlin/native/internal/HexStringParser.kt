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

import kotlin.text.regex.*

@SymbolName("Kotlin_native_int_bits_to_float")
@GCCritical
private external fun intBitsToFloat(x: Int): Float

@SymbolName("Kotlin_native_long_bits_to_double")
@GCCritical
private external fun longBitsToDouble(x: Long): Double

/*
 * Parses hex string to a single or double precision floating point number.
 */
internal class HexStringParser(private val EXPONENT_WIDTH: Int, private val MANTISSA_WIDTH: Int) {

    private val EXPONENT_BASE: Long

    private val MAX_EXPONENT: Long

    private val MIN_EXPONENT: Long

    private val MANTISSA_MASK: Long

    private var sign: Long = 0

    private var exponent: Long = 0

    private var mantissa: Long = 0

    private var abandonedNumber = "" //$NON-NLS-1$

    init {
        this.EXPONENT_BASE = (-1L shl (EXPONENT_WIDTH - 1)).inv()
        this.MAX_EXPONENT = (-1L shl EXPONENT_WIDTH).inv()
        this.MIN_EXPONENT = (-(MANTISSA_WIDTH + 1)).toLong()
        this.MANTISSA_MASK = (-1L shl MANTISSA_WIDTH).inv()
    }

    private fun parse(hexString: String): Long {
        val hexSegments = getSegmentsFromHexString(hexString)
        val signStr = hexSegments[0]
        val significantStr = hexSegments[1]
        val exponentStr = hexSegments[2]

        parseHexSign(signStr)
        parseExponent(exponentStr)
        parseMantissa(significantStr)

        sign = sign shl (MANTISSA_WIDTH + EXPONENT_WIDTH)
        exponent = exponent shl MANTISSA_WIDTH
        return sign or exponent or mantissa
    }

    /*
     * Parses the sign field.
     */
    private fun parseHexSign(signStr: String) {
        this.sign = (if (signStr == "-") 1 else 0).toLong() //$NON-NLS-1$
    }

    /*
     * Parses the exponent field.
     */
    private fun parseExponent(exponentString: String) {
        var exponentStr = exponentString
        val leadingChar = exponentStr[0]
        val expSign = if (leadingChar == '-') -1 else 1
        if (!leadingChar.isDigit()) {
            exponentStr = exponentStr.substring(1)
        }

        try {
            exponent = expSign * exponentStr.toLong()
            checkedAddExponent(EXPONENT_BASE)
        } catch (e: NumberFormatException) {
            exponent = expSign * Long.MAX_VALUE
        }

    }

    /*
     * Parses the mantissa field.
     */
    private fun parseMantissa(significantStr: String) {
        val strings = significantStr.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() //$NON-NLS-1$
        val strIntegerPart = strings[0]
        val strDecimalPart = if (strings.size > 1) strings[1] else "" //$NON-NLS-1$

        var significand = getNormalizedSignificand(strIntegerPart, strDecimalPart)
        if (significand == "0") { //$NON-NLS-1$
            setZero()
            return
        }

        val offset = getOffset(strIntegerPart, strDecimalPart)
        checkedAddExponent(offset.toLong())

        if (exponent >= MAX_EXPONENT) {
            setInfinite()
            return
        }

        if (exponent <= MIN_EXPONENT) {
            setZero()
            return
        }

        if (significand.length > MAX_SIGNIFICANT_LENGTH) {
            abandonedNumber = significand.substring(MAX_SIGNIFICANT_LENGTH)
            significand = significand.substring(0, MAX_SIGNIFICANT_LENGTH)
        }

        mantissa = significand.toLong(HEX_RADIX)

        if (exponent >= 1) {
            processNormalNumber()
        } else {
            processSubNormalNumber()
        }

    }

    private fun setInfinite() {
        exponent = MAX_EXPONENT
        mantissa = 0
    }

    private fun setZero() {
        exponent = 0
        mantissa = 0
    }

    private fun signum(x: Long) = when {
        x == 0L -> 0
        x > 0L -> 1
        else -> -1
    }

    /*
     * Sets the exponent variable to Long.MAX_VALUE or -Long.MAX_VALUE if
     * overflow or underflow happens.
     */
    private fun checkedAddExponent(offset: Long) {
        val result = exponent + offset
        val expSign = signum(exponent)
        if (expSign * signum(offset) > 0 && expSign * signum(result) < 0) {
            exponent = expSign * Long.MAX_VALUE
        } else {
            exponent = result
        }
    }

    private fun processNormalNumber() {
        val desiredWidth = MANTISSA_WIDTH + 2
        fitMantissaInDesiredWidth(desiredWidth)
        round()
        mantissa = mantissa and MANTISSA_MASK
    }

    private fun processSubNormalNumber() {
        var desiredWidth = MANTISSA_WIDTH + 1
        desiredWidth += exponent.toInt()//lends bit from mantissa to exponent
        exponent = 0
        fitMantissaInDesiredWidth(desiredWidth)
        round()
        mantissa = mantissa and MANTISSA_MASK
    }

    /*
     * Adjusts the mantissa to desired width for further analysis.
     */
    private fun fitMantissaInDesiredWidth(desiredWidth: Int) {
        val bitLength = countBitsLength(mantissa)
        if (bitLength > desiredWidth) {
            discardTrailingBits((bitLength - desiredWidth).toLong())
        } else {
            mantissa = mantissa shl (desiredWidth - bitLength)
        }
    }

    /*
     * Stores the discarded bits to abandonedNumber.
     */
    private fun discardTrailingBits(num: Long) {
        val mask = (-1L shl num.toInt()).inv()
        abandonedNumber += mantissa and mask
        mantissa = mantissa shr num.toInt()
    }

    /*
     * The value is rounded up or down to the nearest infinitely precise result.
     * If the value is exactly halfway between two infinitely precise results,
     * then it should be rounded up to the nearest infinitely precise even.
     */
    private fun round() {
        val result = abandonedNumber.replace("0+".toRegex(), "") //$NON-NLS-1$ //$NON-NLS-2$
        val moreThanZero = result.length > 0

        val lastDiscardedBit = (mantissa and 1L).toInt()
        mantissa = mantissa shr 1
        val tailBitInMantissa = (mantissa and 1L).toInt()

        if (lastDiscardedBit == 1 && (moreThanZero || tailBitInMantissa == 1)) {
            val oldLength = countBitsLength(mantissa)
            mantissa += 1L
            val newLength = countBitsLength(mantissa)

            //Rounds up to exponent when whole bits of mantissa are one-bits.
            if (oldLength >= MANTISSA_WIDTH && newLength > oldLength) {
                checkedAddExponent(1)
            }
        }
    }

    /*
     * Returns the normalized significand after removing the leading zeros.
     */
    private fun getNormalizedSignificand(strIntegerPart: String, strDecimalPart: String): String {
        var significand = strIntegerPart + strDecimalPart
        significand = significand.replaceFirst("^0+".toRegex(), "") //$NON-NLS-1$//$NON-NLS-2$
        if (significand.length == 0) {
            significand = "0" //$NON-NLS-1$
        }
        return significand
    }

    /*
     * Calculates the offset between the normalized number and unnormalized
     * number. In a normalized representation, significand is represented by the
     * characters "0x1." followed by a lowercase hexadecimal representation of
     * the rest of the significand as a fraction.
     */
    private fun getOffset(strIntegerPartParam: String, strDecimalPart: String): Int {
        var strIntegerPart = strIntegerPartParam
        strIntegerPart = strIntegerPart.replaceFirst("^0+".toRegex(), "") //$NON-NLS-1$ //$NON-NLS-2$

        // If the Integer part is a nonzero number.
        if (strIntegerPart.length != 0) {
            val leadingNumber = strIntegerPart.substring(0, 1)
            return (strIntegerPart.length - 1) * 4 + countBitsLength(leadingNumber.toLong(HEX_RADIX)) - 1
        }

        // If the Integer part is a zero number.
        var i = 0
        while (i < strDecimalPart.length && strDecimalPart[i] == '0') {
            i++
        }
        if (i == strDecimalPart.length) {
            return 0
        }
        val leadingNumber = strDecimalPart.substring(i, i + 1)
        return (-i - 1) * 4 + countBitsLength(leadingNumber.toLong(HEX_RADIX)) - 1
    }

    fun numberOfLeadingZeros(i: Long): Int {
        // HD, Figure 5-6
        if (i == 0L)
            return 64
        var n = 1
        var x = (i ushr 32).toInt()
        if (x == 0) {
            n += 32
            x = i.toInt()
        }
        if (x ushr 16 == 0) {
            n += 16
            x = x shl 16
        }
        if (x ushr 24 == 0) {
            n += 8
            x = x shl 8
        }
        if (x ushr 28 == 0) {
            n += 4
            x = x shl 4
        }
        if (x ushr 30 == 0) {
            n += 2
            x = x shl 2
        }
        n -= x ushr 31
        return n
    }

    private fun countBitsLength(value: Long) = 64 - numberOfLeadingZeros(value)

    companion object {

        private val DOUBLE_EXPONENT_WIDTH = 11

        private val DOUBLE_MANTISSA_WIDTH = 52

        private val FLOAT_EXPONENT_WIDTH = 8

        private val FLOAT_MANTISSA_WIDTH = 23

        private val HEX_RADIX = 16

        private val MAX_SIGNIFICANT_LENGTH = 15

        private val HEX_SIGNIFICANT = "0[xX](\\p{XDigit}+\\.?|\\p{XDigit}*\\.\\p{XDigit}+)" //$NON-NLS-1$

        private val BINARY_EXPONENT = "[pP]([+-]?\\d+)" //$NON-NLS-1$

        private val FLOAT_TYPE_SUFFIX = "[fFdD]?" //$NON-NLS-1$

        private val HEX_PATTERN = "[\\x00-\\x20]*([+-]?)$HEX_SIGNIFICANT" + //$NON-NLS-1$

                BINARY_EXPONENT + FLOAT_TYPE_SUFFIX + "[\\x00-\\x20]*" //$NON-NLS-1$

        private val PATTERN = Regex(HEX_PATTERN)

        /*
         * Parses the hex string to a double number.
         */
        fun parseDouble(hexString: String): Double {
            val parser = HexStringParser(DOUBLE_EXPONENT_WIDTH,
                    DOUBLE_MANTISSA_WIDTH)
            val result = parser.parse(hexString)
            return longBitsToDouble(result)
        }

        /*
         * Parses the hex string to a float number.
         */
        fun parseFloat(hexString: String): Float {
            val parser = HexStringParser(FLOAT_EXPONENT_WIDTH,
                    FLOAT_MANTISSA_WIDTH)
            val result = parser.parse(hexString).toInt()
            return intBitsToFloat(result)
        }

        /*
         * Analyzes the hex string and extracts the sign and digit segments.
         */
        private fun getSegmentsFromHexString(hexString: String): Array<String> {
            val matchResult = PATTERN.matchEntire(hexString)
            if (matchResult == null) {
                throw NumberFormatException()
            }

            val hexSegments = arrayOf(
                    matchResult.groupValues[1],
                    matchResult.groupValues[2],
                    matchResult.groupValues[3]
            )

            return hexSegments
        }
    }
}