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

@SymbolName("Kotlin_native_NumberConverter_bigIntDigitGeneratorInstImpl")
// The method has several nested loops, so we do not annotate it as GCCritical.
private external fun bigIntDigitGeneratorInstImpl(results: IntArray, uArray: IntArray, f: Long, e: Int,
                                                  isDenormalized: Boolean, mantissaIsZero: Boolean, p: Int)

@SymbolName("Kotlin_native_NumberConverter_ceil")
@GCCritical
private external fun ceil(x: Double): Double

/**
 * Converts [Float] or [Double] numbers to the [String] representation
 */
class NumberConverter {

    private var setCount: Int = 0 // Number of times u and k have been gotten.

    private var getCount: Int = 0 // Number of times u and k have been set.

    private val uArray = IntArray(64)

    private var firstK: Int = 0

    private fun convertDouble(inputNumber: Double): String {
        val p = 1023 + 52 // The power offset (precision).
        @Suppress("INTEGER_OVERFLOW")
        val signMask = 0x7FFFFFFFFFFFFFFFL + 1 // The mask to get the sign of.
        // The number.
        val eMask = 0x7FF0000000000000L // The mask to get the power bits.
        val fMask = 0x000FFFFFFFFFFFFFL // The mask to get the significand.

        // Bits.
        val inputNumberBits = inputNumber.bits()
        // The value of the sign... 0 is positive, ~0 is negative.
        val signString = if (inputNumberBits and signMask == 0L) "" else "-"
        // The value of the 'power bits' of the inputNumber.
        val e = (inputNumberBits and eMask shr 52).toInt()
        // The value of the 'significand bits' of the inputNumber.
        var f = inputNumberBits and fMask
        val mantissaIsZero = f == 0L
        var pow: Int
        var numBits = 52

        if (e == 2047)
            return if (mantissaIsZero) signString + "Infinity" else "NaN"
        if (e == 0) {
            if (mantissaIsZero)
                return signString + "0.0"
            if (f == 1L)
            // Special case to increase precision even though 2 * Double.MIN_VALUE is 1.0e-323.
                return signString + "4.9E-324"
            pow = 1 - p // A denormalized number.
            var ff = f
            while (ff and 0x0010000000000000L == 0L) {
                ff = ff shl 1
                numBits--
            }
        } else {
            // 0 < e < 2047.
            // A "normalized" number.
            f = f or 0x0010000000000000L
            pow = e - p
        }

        if (-59 < pow && pow < 6 || pow == -59 && !mantissaIsZero)
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits)
        else
            bigIntDigitGeneratorInstImpl(f, pow, e == 0, mantissaIsZero, numBits)

        if (inputNumber >= 1e7 || inputNumber <= -1e7
                || inputNumber > -1e-3 && inputNumber < 1e-3)
            return signString + freeFormatExponential()

        return signString + freeFormat()
    }

    private fun convertFloat(inputNumber: Float): String {
        val p = 127 + 23 // The power offset (precision).
        @Suppress("INTEGER_OVERFLOW")
        val signMask = 0x7FFFFFFF + 1 // The mask to get the sign of the number.
        val eMask = 0x7F800000 // The mask to get the power bits.
        val fMask = 0x007FFFFF // The mask to get the significand bits.

        val inputNumberBits = inputNumber.bits()
        // The value of the sign... 0 is positive, ~0 is negative.
        val signString = if (inputNumberBits and signMask == 0) "" else "-"
        // The value of the 'power bits' of the inputNumber.
        val e = inputNumberBits and eMask shr 23
        // The value of the 'significand bits' of the inputNumber.
        var f = inputNumberBits and fMask
        val mantissaIsZero = f == 0
        var pow: Int
        var numBits = 23

        if (e == 255)
            return if (mantissaIsZero) signString + "Infinity" else "NaN"
        if (e == 0) {
            if (mantissaIsZero)
                return signString + "0.0"
            pow = 1 - p // A denormalized number.
            if (f < 8) { // Want more precision with smallest values.
                f = f shl 2
                pow -= 2
            }
            var ff = f
            while (ff and 0x00800000 == 0) {
                ff = ff shl 1
                numBits--
            }
        } else {
            // 0 < e < 255.
            // A "normalized" number.
            f = f or 0x00800000
            pow = e - p
        }

        if (-59 < pow && pow < 35 || pow == -59 && !mantissaIsZero)
            longDigitGenerator(f.toLong(), pow, e == 0, mantissaIsZero, numBits)
        else
            bigIntDigitGeneratorInstImpl(f.toLong(), pow, e == 0, mantissaIsZero, numBits)
        if (inputNumber >= 1e7f || inputNumber <= -1e7f
                || inputNumber > -1e-3f && inputNumber < 1e-3f)
            return signString + freeFormatExponential()

        return signString + freeFormat()
    }

    private fun freeFormatExponential(): String {
        // Corresponds to process "Free-Format Exponential".
        val formattedDecimal = CharArray(25)
        formattedDecimal[0] = ('0' + uArray[getCount++])
        formattedDecimal[1] = '.'
        // The position the next character is to be inserted into formattedDecimal.
        var charPos = 2

        var k = firstK
        val expt = k
        while (true) {
            k--
            if (getCount >= setCount)
                break

            formattedDecimal[charPos++] = ('0' + uArray[getCount++])
        }

        if (k == expt - 1)
            formattedDecimal[charPos++] = '0'
        formattedDecimal[charPos++] = 'E'
        return unsafeStringFromCharArray(formattedDecimal, 0, charPos) + expt.toString()
    }

    private fun freeFormat(): String {
        // Corresponds to process "Free-Format".
        val formattedDecimal = CharArray(25)
        // The position the next character is to be inserted into formattedDecimal.
        var charPos = 0
        var k = firstK
        if (k < 0) {
            formattedDecimal[0] = '0'
            formattedDecimal[1] = '.'
            charPos += 2
            for (i in k + 1 .. -1)
                formattedDecimal[charPos++] = '0'
        }

        var u = uArray[getCount++]
        do {
            if (u != -1)
                formattedDecimal[charPos++] = ('0' + u)
            else if (k >= -1)
                formattedDecimal[charPos++] = '0'

            if (k == 0)
                formattedDecimal[charPos++] = '.'

            k--
            u = if (getCount < setCount) uArray[getCount++] else -1
        } while (u != -1 || k >= -1)
        return unsafeStringFromCharArray(formattedDecimal, 0, charPos)
    }

    private fun bigIntDigitGeneratorInstImpl(f: Long, e: Int,
                                             isDenormalized: Boolean, mantissaIsZero: Boolean, p: Int) {
        val results = IntArray(3)
        bigIntDigitGeneratorInstImpl(results, uArray, f, e, isDenormalized, mantissaIsZero, p)
        setCount = results[0]
        getCount = results[1]
        firstK   = results[2]
    }

    private fun longDigitGenerator(f: Long, e: Int, isDenormalized: Boolean,
                                   mantissaIsZero: Boolean, p: Int) {
        var r: Long
        var s: Long
        var m: Long
        if (e >= 0) {
            m = 1L shl e
            if (!mantissaIsZero) {
                r = f shl e + 1
                s = 2
            } else {
                r = f shl e + 2
                s = 4
            }
        } else {
            m = 1
            if (isDenormalized || !mantissaIsZero) {
                r = f shl 1
                s = 1L shl 1 - e
            } else {
                r = f shl 2
                s = 1L shl 2 - e
            }
        }

        val k = ceil((e + p - 1) * invLogOfTenBaseTwo - 1e-10).toInt()

        if (k > 0) {
            s *= TEN_TO_THE[k]
        } else if (k < 0) {
            val scale = TEN_TO_THE[-k]
            r *= scale
            m = if (m == 1L) scale else m * scale
        }

        if (r + m > s) { // Was M_plus.
            firstK = k
        } else {
            firstK = k - 1
            r *= 10
            m *= 10
        }

        setCount = 0
        getCount = setCount // Reset indices.
        var low: Boolean
        var high: Boolean
        var u: Int
        val si = longArrayOf(s, s shl 1, s shl 2, s shl 3)
        while (true) {
            // Set U to be floor (r / s) and r to be the remainder
            // using a kind of "binary search" to find the answer.
            // It's a lot quicker than actually dividing since we know
            // the answer will be between 0 and 10.
            u = 0
            var remainder: Long
            for (i in 3 downTo 0) {
                remainder = r - si[i]
                if (remainder >= 0) {
                    r = remainder
                    u += 1 shl i
                }
            }

            low = r < m // Was M_minus.
            high = r + m > s // Was M_plus.

            if (low || high)
                break

            r *= 10
            m *= 10
            uArray[setCount++] = u
        }
        if (low && !high)
            uArray[setCount++] = u
        else if (high && !low)
            uArray[setCount++] = u + 1
        else if (r shl 1 < s)
            uArray[setCount++] = u
        else
            uArray[setCount++] = u + 1
    }

    companion object {

        private val invLogOfTenBaseTwo = 0.30102999566398114251

        private val TEN_TO_THE = LongArray(20)

        init {
            TEN_TO_THE[0] = 1L
            for (i in 1 until TEN_TO_THE.size) {
                TEN_TO_THE[i] = TEN_TO_THE[i - 1] * 10
            }
        }

        private val converter: NumberConverter
            get() = NumberConverter()

        fun convert(input: Double): String {
            return converter.convertDouble(input)
        }

        fun convert(input: Float): String {
            return converter.convertFloat(input)
        }
    }
}