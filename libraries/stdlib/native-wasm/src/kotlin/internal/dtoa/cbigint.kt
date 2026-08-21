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

@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("RETURN_VALUE_NOT_USED", "NOTHING_TO_INLINE")

package kotlin.internal.dtoa

private const val MANTISSA_MASK = 0x000FFFFFFFFFFFFFUL
private const val EXPONENT_MASK = 0x7FF0000000000000UL
internal const val NORMAL_MASK = 0x0010000000000000UL
private const val SIGN_MASK = 0x8000000000000000UL
private const val E_OFFSET = 1075

private const val EXPONENT_MASK_HI = 0x7FF00000u
private const val MANTISSA_MASK_HI = 0x000FFFFFu

private const val TEN_E3 = 0x3E8UL
private const val TEN_E4 = 0x2710UL
private const val TEN_E5 = 0x186A0UL
private const val TEN_E6 = 0xF4240UL
private const val TEN_E7 = 0x989680UL
private const val TEN_E8 = 0x5F5E100UL
private const val TEN_E9 = 0x3B9ACA00UL
private const val TEN_E19 = 0x8AC7230489E80000UL

// Macro replacements as functions
private inline fun highInU64(value: ULong): ULong = value shr 32
private inline fun lowInU64(value: ULong): ULong = value and 0x00000000FFFFFFFFUL
internal inline fun lowU32FromVar(u64: ULong): UInt = (u64 and 0x00000000FFFFFFFFUL).toUInt()
internal inline fun highU32FromVar(u64: ULong): UInt = (u64 shr 32).toUInt()
internal inline fun lowU32FromPtr(arr: ULongArray, idx: Int): UInt = (arr[idx] and 0x00000000FFFFFFFFUL).toUInt()
internal inline fun highU32FromPtr(arr: ULongArray, idx: Int): UInt = (arr[idx] shr 32).toUInt()
internal inline fun setLowU32Ptr(arr: ULongArray, idx: Int, value: UInt) {
    arr[idx] = (arr[idx] and 0xFFFFFFFF00000000UL) or value.toULong()
}

internal inline fun setHighU32Ptr(arr: ULongArray, idx: Int, value: UInt) {
    arr[idx] = (arr[idx] and 0x00000000FFFFFFFFUL) or (value.toULong() shl 32)
}

private inline fun createDoubleBits(normalizedM: ULong, e: Int): ULong = (normalizedM and MANTISSA_MASK) or (((e + E_OFFSET).toULong()) shl 52)
private inline fun timesTen(x: ULong): ULong = (x shl 3) + (x shl 1)
private inline fun bitSection(x: UInt, mask: UInt, shift: Int): UInt = (x and mask) shr shift

internal inline fun doubleMantissa(z: Double): ULong {
    var m = z.toRawBits().toULong()

    if ((m and EXPONENT_MASK) != 0UL) m = (m and MANTISSA_MASK) or NORMAL_MASK
    else m = (m and MANTISSA_MASK)

    return m
}

internal inline fun doubleExponent(z: Double): Int {
    /* assumes positive double */
    var k: Int = (highU32FromVar(z.toRawBits().toULong()) shr 20).toInt()

    if (k != 0) k -= E_OFFSET
    else k = 1 - E_OFFSET

    return k
}

internal inline fun isDenormalDouble(double: Double): Boolean {
    val doubleAsULong = double.toRawBits().toULong()
    val high = highU32FromVar(doubleAsULong)
    val low = lowU32FromVar(doubleAsULong)

    return (high and EXPONENT_MASK_HI == 0u) && (high and MANTISSA_MASK_HI != 0u || low != 0u)
}

private fun simpleAddHighPrecision(arg1: ULongArray, length: Int, arg2: ULong): Int {
    /* assumes length > 0 */
    var index = 1

    arg1[0] += arg2
    if (arg2 <= arg1[0])
        return 0
    else if (length == 1)
        return 1

    while (++arg1[index] == 0UL && ++index < length) {
        // no body
    }
    return if (index == length) 1 else 0
}

internal fun addHighPrecision(arg1: ULongArray, length1: Int, arg2: ULongArray, length2: Int): Int {
    var length2 = length2
    // addition is limited by length of arg1 as it this function is
    // storing the result in arg1
    var carry: ULong

    if (length1 == 0 || length2 == 0) {
        return 0
    } else if (length1 < length2) {
        length2 = length1
    }

    carry = 0UL
    var index = 0
    do {
        val temp1 = arg1[index]
        val temp2 = arg2[index]
        val temp3 = temp1 + temp2
        arg1[index] = temp3 + carry
        if (arg2[index] < arg1[index])
            carry = 0UL
        else if (arg2[index] != arg1[index])
            carry = 1UL
    } while (++index < length2)
    if (carry == 0UL)
        return 0
    else if (index == length1)
        return 1

    while (++arg1[index] == 0UL && ++index < length1) {
        // no body
    }

    return if (index == length1) 1 else 0
}

internal fun subtractHighPrecision(arg1: ULongArray, length1: Int, arg2: ULongArray, length2: Int) {
    var length2 = length2
    // assumes arg1 > arg2
    for (index in 0 until length1)
        arg1[index] = arg1[index].inv()
    simpleAddHighPrecision(arg1, length1, 1UL)

    while (length2 > 0 && arg2[length2 - 1] == 0UL)
        length2--

    addHighPrecision(arg1, length1, arg2, length2)

    for (index in 0 until length1)
        arg1[index] = arg1[index].inv()
    simpleAddHighPrecision(arg1, length1, 1UL)
}

private fun simpleMultiplyHighPrecision(arg1: ULongArray, length: Int, arg2: ULong): UInt {
    /* assumes arg2 only holds 32 bits of information */
    var product: ULong
    var index: Int

    index = 0
    product = 0UL

    do {
        product =
            highInU64(product) + arg2 * lowU32FromPtr(arg1, index)
        setLowU32Ptr(arg1, index, lowU32FromVar(product))
        product =
            highInU64(product) + arg2 * highU32FromPtr(arg1, index);
        setHighU32Ptr(arg1, index, lowU32FromVar(product))
    } while (++index < length)

    return highU32FromVar(product)
}

private value class ULongArrayView(val array: ULongArray)

private operator fun ULongArrayView.get(idx32: Int): UInt =
    if (idx32 % 2 == 0) lowU32FromPtr(array, idx32 / 2) else highU32FromPtr(array, idx32 / 2)

private operator fun ULongArrayView.set(idx32: Int, value: UInt) =
    if (idx32 % 2 == 0) setLowU32Ptr(array, idx32 / 2, value) else setHighU32Ptr(array, idx32 / 2, value)

private fun simpleMultiplyAddHighPrecision(arg1: ULongArray, length: Int, arg2: ULong, result: ULongArrayView, resultOffset: Int) {
    /* Assumes result can hold the product and arg2 only holds 32 bits
       of information */
    var product: ULong
    var index: Int
    var resultIndex: Int

    index = 0
    resultIndex = 0
    product = 0UL

    do {
        product =
            highInU64(product) + result[resultIndex + resultOffset] +
                    arg2 * lowU32FromPtr(arg1, index)
        result[resultIndex + resultOffset] = lowU32FromVar(product)
        ++resultIndex
        product =
            highInU64(product) + result[resultIndex + resultOffset] +
                    arg2 * highU32FromPtr(arg1, index)
        result[resultIndex + resultOffset] = lowU32FromVar(product)
        ++resultIndex
    } while (++index < length)

    result[resultIndex + resultOffset] += highU32FromVar(product)
    if (result[resultIndex + resultOffset] < highU32FromVar(product)) {
        /* must be careful with ++ operator and macro expansion */
        ++resultIndex
        while (++result[resultIndex + resultOffset] == 0u)
            ++resultIndex
    }
}

internal fun multiplyHighPrecision(arg1: ULongArray, length1: Int, arg2: ULongArray, length2: Int, result: ULongArray, length: Int) {
    var arg1 = arg1
    var arg2 = arg2
    var length1 = length1
    var length2 = length2
    /* assumes result is large enough to hold product */
    val temp: ULongArray
    val count: Int
    var index: Int

    if (length1 < length2) {
        temp = arg1
        arg1 = arg2
        arg2 = temp
        count = length1
        length1 = length2
        length2 = count
    }

    result.fill(0u, 0, length)

    val arrayView = ULongArrayView(result)

    /* length1 > length2 */
    index = -1
    for (count in 0 until length2) {
        simpleMultiplyAddHighPrecision(arg1, length1, lowInU64(arg2[count]), arrayView, ++index)
        simpleMultiplyAddHighPrecision(arg1, length1, highInU64(arg2[count]), arrayView, ++index)
    }
}

internal fun simpleAppendDecimalDigitHighPrecision(arg1: ULongArray, length: Int, digit: ULong): UInt {
    var digit = digit
    /* assumes digit is less than 32 bits */
    var arg: ULong
    var index = 0

    digit = digit shl 32
    do {
        arg = lowInU64(arg1[index]);
        digit = highInU64(digit) + timesTen(arg);
        setLowU32Ptr(arg1, index, lowU32FromVar(digit))

        arg = highInU64(arg1[index]);
        digit = highInU64(digit) + timesTen(arg);
        setHighU32Ptr(arg1, index, lowU32FromVar(digit))
    } while (++index < length)

    return highU32FromVar(digit)
}

internal fun simpleShiftLeftHighPrecision(arg1: ULongArray, length: Int, arg2: Int) {
    var arg2 = arg2
    var length = length
    /* assumes length > 0 */
    var index: Int
    var offset: Int
    if (arg2 >= 64) {
        offset = arg2 shr 6
        index = length

        while (--index - offset >= 0)
            arg1[index] = arg1[index - offset]

        do {
            arg1[index] = 0UL
        } while (--index >= 0)

        arg2 = arg2 and 0x3F
    }

    if (arg2 == 0)
        return

    while (--length > 0) {
        arg1[length] = (arg1[length] shl arg2) or (arg1[length - 1] shr (64 - arg2))
    }

    arg1[0] = arg1[0] shl arg2
}

private fun highestSetBit(y: ULong): Int =
    ULong.SIZE_BITS - y.countLeadingZeroBits()

private fun lowestSetBit(y: ULong): Int =
    if (y != 0UL) y.countTrailingZeroBits() + 1 else 0

internal fun highestSetBitHighPrecision(arg: ULongArray, length: Int): Int {
    var len = length
    while (--len >= 0) {
        val highBit = highestSetBit(arg[len])
        if (highBit != 0) return highBit + 64 * len
    }
    return 0
}

internal fun lowestSetBitHighPrecision(arg: ULongArray, length: Int): Int {
    var index = -1
    while (++index < length) {
        val lowBit = lowestSetBit(arg[index])
        if (lowBit != 0) return lowBit + 64 * index
    }
    return 0
}

internal fun compareHighPrecision(arg1: ULongArray, length1: Int, arg2: ULongArray, length2: Int): Int {
    var length1 = length1
    var length2 = length2
    while (--length1 >= 0 && arg1[length1] == 0UL) { /* no body */
    }
    while (--length2 >= 0 && arg2[length2] == 0UL) { /* no body */
    }

    if (length1 > length2)
        return 1
    else if (length1 < length2)
        return -1
    else if (length1 > -1) {
        do {
            if (arg1[length1] > arg2[length1])
                return 1
            else if (arg1[length1] < arg2[length1])
                return -1
        } while (--length1 >= 0)
    }

    return 0
}

internal fun toDoubleHighPrecision(arg: ULongArray, length: Int): Double {
    var length = length
    var highBit: Int
    var mantissa: ULong
    var test64: ULong
    var test: UInt
    var result: Double

    while (length > 0 && arg[length - 1] == 0UL)
        --length

    if (length == 0)
        result = 0.0
    else if (length > 16) {
        result = Double.fromBits(EXPONENT_MASK.toLong())
    } else if (length == 1) {
        highBit = highestSetBit(arg[0])
        if (highBit <= 53) {
            highBit = 53 - highBit
            mantissa = arg[0] shl highBit
            result = Double.fromBits(createDoubleBits(mantissa, -highBit).toLong())
        } else {
            highBit -= 53
            mantissa = arg[0] shr highBit
            result =
                Double.fromBits(createDoubleBits(mantissa, highBit).toLong())

            /* perform rounding, round to even in case of tie */
            test = (lowU32FromPtr(arg, 0) shl (11 - highBit)) and 0x7FFU
            if (test > 0x400U || ((test == 0x400U) && (mantissa and 1UL != 0UL)))
                result = Double.fromBits((result.toRawBits().toULong() + 1UL).toLong())
        }
    } else {
        highBit = highestSetBit(arg[--length])
        if (highBit <= 53) {
            highBit = 53 - highBit
            if (highBit > 0) {
                mantissa =
                    (arg[length] shl highBit) or (arg[length - 1] shr
                            (64 - highBit))
            } else {
                mantissa = arg[length]
            }
            result =
                Double.fromBits(createDoubleBits(mantissa, length * 64 - highBit).toLong())

            /* perform rounding, round to even in case of tie */
            test64 = arg[--length] shl highBit
            if (test64 > SIGN_MASK || ((test64 == SIGN_MASK) && (mantissa and 1UL != 0UL)))
                result = Double.fromBits((result.toRawBits().toULong() + 1UL).toLong())
            else if (test64 == SIGN_MASK) {
                while (--length >= 0) {
                    if (arg[length] != 0UL) {
                        result =
                            Double.fromBits((result.toRawBits().toULong() + 1UL).toLong())
                        break
                    }
                }
            }
        } else {
            highBit -= 53
            mantissa = arg[length] shr highBit
            result =
                Double.fromBits((createDoubleBits(mantissa, length * 64 + highBit)).toLong())

            /* perform rounding, round to even in case of tie */
            test = (lowU32FromPtr(arg, length) shl (11 - highBit)) and 0x7FFU
            if (test > 0x400UL || ((test == 0x400U) && (mantissa and 1UL != 0UL)))
                result = Double.fromBits((result.toRawBits().toULong() + 1UL).toLong())
            else if (test == 0x400U) {
                do {
                    if (arg[--length] != 0UL) {
                        result =
                            Double.fromBits((result.toRawBits().toULong() + 1UL).toLong())
                        break
                    }
                } while (length > 0)
            }
        }
    }

    return result
}

internal fun timesTenToTheEHighPrecision(result: ULongArray, length: Int, e: Int): Int {
    var length = length
    /* assumes result can hold value */
    var overflow: ULong
    var exp10 = e

    if (e == 0)
        return length

    /* bad O(n) way of doing it, but simple */
    /*
       do {
       overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0);
       if (overflow)
       result[length++] = overflow;
       } while (--e);
     */
    /* Replace the current implementation which performs a
     * "multiplication" by 10 e number of times with an actual
     * multiplication. 10e19 is the largest exponent to the power of ten
     * that will fit in a 64-bit integer, and 10e9 is the largest exponent to
     * the power of ten that will fit in a 64-bit integer. Not sure where the
     * break-even point is between an actual multiplication and a
     * simpleAappendDecimalDigit() so just pick 10e3 as that point for
     * now.
     */
    while (exp10 >= 19) {
        overflow = simpleMultiplyHighPrecision64(result, length, TEN_E19)
        if (overflow != 0UL)
            result[length++] = overflow
        exp10 -= 19
    }
    while (exp10 >= 9) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E9).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
        exp10 -= 9
    }
    if (exp10 == 0)
        return length
    else if (exp10 == 1) {
        overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0UL).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 2) {
        overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0UL).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
        overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0UL).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 3) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E3).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 4) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E4).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 5) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E5).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 6) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E6).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 7) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E7).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    } else if (exp10 == 8) {
        overflow = simpleMultiplyHighPrecision(result, length, TEN_E8).toULong()
        if (overflow != 0UL)
            result[length++] = overflow
    }
    return length
}

private fun simpleMultiplyHighPrecision64(arg1: ULongArray, length: Int, arg2: ULong): ULong {
    var intermediate: ULong
    var carry1: ULong
    var carry2: ULong
    var prod1: ULong
    var prod2: ULong
    var sum: ULong

    var pArg1: Int

    var index: Int
    var buf32: UInt

    index = 0
    intermediate = 0UL
    pArg1 = index
    carry2 = 0UL

    do {
        if ((arg1[pArg1] != 0UL) || (intermediate != 0UL)) {
            prod1 =
                lowU32FromVar(arg2).toULong() * lowU32FromPtr(arg1, pArg1).toULong()
            sum = intermediate + prod1
            if ((sum < prod1) || (sum < intermediate)) {
                carry1 = 1UL
            } else {
                carry1 = 0UL
            }
            prod1 =
                lowU32FromVar(arg2).toULong() * highU32FromPtr(arg1, pArg1).toULong()
            prod2 =
                highU32FromVar(arg2).toULong() * lowU32FromPtr(arg1, pArg1).toULong()
            intermediate = carry2 + highInU64(sum) + prod1 + prod2
            if ((intermediate < prod1) || (intermediate < prod2)) {
                carry2 = 1UL
            } else {
                carry2 = 0UL
            }

            setLowU32Ptr(arg1, pArg1, lowU32FromVar(sum))
            buf32 = highU32FromPtr(arg1, pArg1)
            setHighU32Ptr(arg1, pArg1, lowU32FromVar(intermediate))
            intermediate = carry1 + highInU64(intermediate) + highU32FromVar(arg2).toULong() * buf32.toULong()
        }
        pArg1++
    } while (++index < length)
    return intermediate
}
