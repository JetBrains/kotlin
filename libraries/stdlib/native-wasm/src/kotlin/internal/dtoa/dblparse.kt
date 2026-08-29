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

import kotlin.math.ceil
import kotlin.math.pow

private const val MAX_ACCURACY_WIDTH_DOUBLE = 17
private const val LOG5_OF_TWO_TO_THE_N = 23
private const val APPROX_MIN_MAGNITUDE = -309
private const val APPROX_MAX_MAGNITUDE = 309
private const val INV_LOG_OF_TEN_BASE_2 = 0.30102999566398114

private const val RM_SIZE = 21
private const val STemp_SIZE = 22

private val TENS = doubleArrayOf(
    1.0, 1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5, 1.0e6, 1.0e7, 1.0e8, 1.0e9,
    1.0e10, 1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15, 1.0e16, 1.0e17, 1.0e18,
    1.0e19, 1.0e20, 1.0e21, 1.0e22
)

// Macro replacements as functions
internal inline fun sizeOfTenToTheE(e: Int): Int = (e / 19) + 1
private inline fun tenToTheE(e: Int): Double = TENS[e]
private inline fun errorOccured(x: Double): Boolean = (x.toRawBits() shr 32).toInt() < 0
private inline fun longBitsToDouble(bits: ULong): Double = Double.fromBits(bits.toLong())

private fun createDouble(s: String, e: Int): Double {
    var e = e
    /* assumes s is a string with at least one
    * character in it */
    val def = ULongArray(MAX_ACCURACY_WIDTH_DOUBLE)
    val defBackup = ULongArray(MAX_ACCURACY_WIDTH_DOUBLE)

    val f: ULongArray
    var fNoOverflow: ULongArray

    var overflow: UInt
    val result: Double
    var index = 1
    val unprocessedDigits: Int

    f = def
    fNoOverflow = defBackup
    f[0] = 0U

    var pS = 0
    do {
        if (s[pS] in '0'..'9') {
            /* Make a back up of f before appending, so that we can
             * back out of it if there is no more room, i.e. index >
             * MAX_ACCURACY_WIDTH.
             */
            f.copyInto(fNoOverflow, 0, 0, index)

            overflow =
                simpleAppendDecimalDigitHighPrecision(f, index, (s[pS] - '0').toULong())
            if (overflow != 0U) {
                f[index++] = overflow.toULong()
                /* There is an overflow, but there is no more room
                 * to store the result. We really only need the top 52
                 * bits anyway, so we must back out of the overflow,
                 * and ignore the rest of the string.
                 */
                if (index >= MAX_ACCURACY_WIDTH_DOUBLE) {
                    index--
                    fNoOverflow.copyInto(f, 0, 0, index)
                    break
                }
            }
        } else
            index = -1
    } while (index > 0 && ++pS < s.length)

    /* We've broken out of the parse loop either because we've reached
    * the end of the string or we've overflowed the maximum accuracy
    * limit of a double. If we still have unprocessed digits in the
    * given string, then there are three possible results:
    *   1. (unprocessed digits + e) == 0, in which case we simply
    *      convert the existing bits that are already parsed
    *   2. (unprocessed digits + e) < 0, in which case we simply
    *      convert the existing bits that are already parsed along
    *      with the given e
    *   3. (unprocessed digits + e) > 0 indicates that the value is
    *      simply too big to be stored as a double, so return Infinity
    */
    unprocessedDigits = s.length - pS
    if (unprocessedDigits > 0) {
        e += unprocessedDigits
        if (index > -1) {
            if (e == 0)
                result = toDoubleHighPrecision(f, index)
            else if (e < 0)
                result = createDouble1(f, index, e)
            else {
                result = Double.POSITIVE_INFINITY
            }
        } else {
            result = Double.fromBits(-1)
        }
    } else {
        if (index > -1) {
            if (e == 0)
                result = toDoubleHighPrecision(f, index)
            else
                result = createDouble1(f, index, e)
        } else {
            result = Double.fromBits(-1)
        }
    }

    return result
}


private fun createDouble1(f: ULongArray, length: Int, e: Int): Double {
    var numBits: Int
    var result = 0.0

    numBits = highestSetBitHighPrecision(f, length) + 1
    numBits -= lowestSetBitHighPrecision(f, length)
    if (numBits < 54 && e >= 0 && e < LOG5_OF_TWO_TO_THE_N) {
        return toDoubleHighPrecision(f, length) * tenToTheE(e)
    } else if (numBits < 54 && e < 0 && (-e) < LOG5_OF_TWO_TO_THE_N) {
        return toDoubleHighPrecision(f, length) / tenToTheE(-e)
    } else if (e >= 0 && e < APPROX_MAX_MAGNITUDE) {
        result = toDoubleHighPrecision(f, length) * 10.0.pow((e).toDouble())
    } else if (e >= APPROX_MAX_MAGNITUDE) {
        /* Convert the partial result to make sure that the
         * non-exponential part is not zero. This check fixes the case
         * where the user enters 0.0e309! */
        result = toDoubleHighPrecision(f, length)
        /* Don't go straight to zero as the fact that x*0 = 0 independent of x might
           cause the algorithm to produce an incorrect result.  Instead try the min value
           first and let it fall to zero if need be. */

        if (result == 0.0)
            result = Double.MIN_VALUE
        else
            result = Double.POSITIVE_INFINITY
    } else if (e > APPROX_MIN_MAGNITUDE) {
        result = toDoubleHighPrecision(f, length) / 10.0.pow((-e).toDouble())
    }

    if (e <= APPROX_MIN_MAGNITUDE) {

        result = toDoubleHighPrecision(f, length) * 10.0.pow((e + 52).toDouble())
        result = result * 10.0.pow((-52).toDouble())

    }

    /* Don't go straight to zero as the fact that x*0 = 0 independent of x might
       cause the algorithm to produce an incorrect result.  Instead try the min value
       first and let it fall to zero if need be. */

    if (result == 0.0)
        result = Double.MIN_VALUE

    return doubleAlgorithm(f, length, e, result)
}


/* The algorithm for the function doubleAlgorithm() below can be found
 * in:
 *
 *      "How to Read Floating-Point Numbers Accurately", William D.
 *      Clinger, Proceedings of the ACM SIGPLAN '90 Conference on
 *      Programming Language Design and Implementation, June 20-22,
 *      1990, pp. 92-101.
 *
 * There is a possibility that the function will end up in an endless
 * loop if the given approximating floating-point number (a very small
 * floating-point whose value is very close to zero) straddles between
 * two approximating integer values. We modified the algorithm slightly
 * to detect the case where it oscillates back and forth between
 * incrementing and decrementing the floating-point approximation. It
 * is currently set such that if the oscillation occurs more than twice
 * then return the original approximation.
 */
private fun doubleAlgorithm(f: ULongArray, length: Int, e: Int, z: Double): Double {
    var z = z
    var m: ULong
    var k: Int
    var comparison: Int
    var comparison2: Int

    var x: ULongArray
    var y: ULongArray
    var D: ULongArray
    var D2: ULongArray

    var xLength: Int
    var yLength: Int
    var DLength: Int
    var D2Length: Int
    var decApproxCount: Int
    var incApproxCount: Int

    decApproxCount = 0
    incApproxCount = 0

    do {
        m = doubleMantissa(z)
        k = doubleExponent(z)

        if (e >= 0 && k >= 0) {
            xLength = sizeOfTenToTheE(e) + length
            x = ULongArray(xLength)
            f.copyInto(x, 0, 0, length)
            timesTenToTheEHighPrecision(x, xLength, e)

            yLength = (k shr 6) + 2
            y = ULongArray(yLength)
            y[0] = m
            simpleShiftLeftHighPrecision(y, yLength, k)
        } else if (e >= 0) {
            xLength = sizeOfTenToTheE(e) + length + ((-k) shr 6) + 1
            x = ULongArray(xLength)
            f.copyInto(x, 0, 0, length)
            timesTenToTheEHighPrecision(x, xLength, e)
            simpleShiftLeftHighPrecision(x, xLength, -k)

            yLength = 1
            y = ULongArray(1)
            y[0] = m
        } else if (k >= 0) {
            xLength = length
            x = f

            yLength = sizeOfTenToTheE(-e) + 2 + (k shr 6)
            y = ULongArray(yLength)
            y[0] = m
            timesTenToTheEHighPrecision(y, yLength, -e)
            simpleShiftLeftHighPrecision(y, yLength, k)
        } else {
            xLength = length + ((-k) shr 6) + 1
            x = ULongArray(xLength)
            f.copyInto(x, 0, 0, length)
            simpleShiftLeftHighPrecision(x, xLength, -k)

            yLength = sizeOfTenToTheE(-e) + 1
            y = ULongArray(yLength)
            y[0] = m
            timesTenToTheEHighPrecision(y, yLength, -e)
        }

        comparison = compareHighPrecision(x, xLength, y, yLength)
        if (comparison > 0) {                       /* x > y */
            DLength = xLength
            D = ULongArray(DLength)
            x.copyInto(D, 0, 0, DLength)
            subtractHighPrecision(D, DLength, y, yLength)
        } else if (comparison != 0) {                       /* y > x */
            DLength = yLength
            D = ULongArray(DLength)
            y.copyInto(D, 0, 0, DLength)
            subtractHighPrecision(D, DLength, x, xLength)
        } else {                       /* y == x */
            DLength = 1
            D = ULongArray(1)
            D[0] = 0UL
        }

        D2Length = DLength + 1
        D2 = ULongArray(D2Length)
        m = m shl 1
        multiplyHighPrecision(D, DLength, ulongArrayOf(m), 1, D2, D2Length)
        m = m shr 1

        comparison2 = compareHighPrecision(D2, D2Length, y, yLength)
        if (comparison2 < 0) {
            if (comparison < 0 && m == NORMAL_MASK) {
                simpleShiftLeftHighPrecision(D2, D2Length, 1)
                if (compareHighPrecision(D2, D2Length, y, yLength) > 0) {
                    // DECREMENT_DOUBLE (z, decApproxCount, incApproxCount) macro expansion
                    z = longBitsToDouble((z.toRawBits().toULong() - 1UL))
                    decApproxCount++
                    if (incApproxCount > 2 && decApproxCount > 2) {
                        z = if (decApproxCount > incApproxCount)
                            longBitsToDouble((z.toRawBits().toULong() + (decApproxCount - incApproxCount).toULong()))
                        else
                            longBitsToDouble((z.toRawBits().toULong() - (incApproxCount - decApproxCount).toULong()))
                        break
                    }
                    // End DECREMENT_DOUBLE macro expansion
                } else {
                    break
                }
            } else {
                break
            }
        } else if (comparison2 == 0) {
            if ((lowU32FromVar(m) and 1u) == 0u) {
                if (comparison < 0 && m == NORMAL_MASK) {
                    // DECREMENT_DOUBLE (z, decApproxCount, incApproxCount) macro expansion
                    z = longBitsToDouble((z.toRawBits().toULong() - 1UL))
                    decApproxCount++
                    if (incApproxCount > 2 && decApproxCount > 2) {
                        z = if (decApproxCount > incApproxCount)
                            longBitsToDouble((z.toRawBits().toULong() + (decApproxCount - incApproxCount).toULong()))
                        else
                            longBitsToDouble((z.toRawBits().toULong() - (incApproxCount - decApproxCount).toULong()))
                        break
                    }
                    // End DECREMENT_DOUBLE macro expansion
                } else {
                    break
                }
            } else if (comparison < 0) {
                // DECREMENT_DOUBLE (z, decApproxCount, incApproxCount) macro expansion
                z = longBitsToDouble((z.toRawBits().toULong() - 1UL))
                decApproxCount++
                if (incApproxCount > 2 && decApproxCount > 2) {
                    z = if (decApproxCount > incApproxCount)
                        longBitsToDouble((z.toRawBits().toULong() + (decApproxCount - incApproxCount).toULong()))
                    else
                        longBitsToDouble((z.toRawBits().toULong() - (incApproxCount - decApproxCount).toULong()))
                    break
                }
                // End DECREMENT_DOUBLE macro expansion
                break
            } else {
                // INCREMENT_DOUBLE (z, decApproxCount, incApproxCount) macro expansion
                z = longBitsToDouble((z.toRawBits().toULong() + 1UL))
                incApproxCount++
                if (incApproxCount > 2 && decApproxCount > 2) {
                    z = if (decApproxCount > incApproxCount)
                        longBitsToDouble((z.toRawBits().toULong() + (decApproxCount - incApproxCount).toULong()))
                    else
                        longBitsToDouble((z.toRawBits().toULong() - (incApproxCount - decApproxCount).toULong()))
                    break
                }
                // End INCREMENT_DOUBLE macro expansion
                break
            }
        } else if (comparison < 0) {
            // DECREMENT_DOUBLE (z, decApproxCount, incApproxCount) macro expansion
            z = longBitsToDouble((z.toRawBits().toULong() - 1UL))
            decApproxCount++
            if (incApproxCount > 2 && decApproxCount > 2) {
                z = if (decApproxCount > incApproxCount)
                    longBitsToDouble((z.toRawBits().toULong() + (decApproxCount - incApproxCount).toULong()))
                else
                    longBitsToDouble((z.toRawBits().toULong() - (incApproxCount - decApproxCount).toULong()))
                break
            }
            // End DECREMENT_DOUBLE macro expansion
        } else {
            if (z.isInfinite())
                break
            // INCREMENT_DOUBLE (z, decApproxCount, incApproxCount) macro expansion
            z = longBitsToDouble((z.toRawBits().toULong() + 1UL))
            incApproxCount++
            if (incApproxCount > 2 && decApproxCount > 2) {
                z = if (decApproxCount > incApproxCount)
                    longBitsToDouble((z.toRawBits().toULong() + (decApproxCount - incApproxCount).toULong()))
                else
                    longBitsToDouble((z.toRawBits().toULong() - (incApproxCount - decApproxCount).toULong()))
                break
            }
            // End INCREMENT_DOUBLE macro expansion
        }
    } while (true)

    return z
}

internal fun parseDoubleImpl(s: String, e: Int): Double {
    val dbl = createDouble(s, e)
    if (!errorOccured(dbl)) return dbl
    if ((dbl.toRawBits().toULong() and 0xFFFFFFFFUL).toInt() == -1) throw NumberFormatException("Invalid double format")
    throw RuntimeException()
}

/* The algorithm for this particular function can be found in:
 *
 *      Printing Floating-Point Numbers Quickly and Accurately, Robert
 *      G. Burger, and R. Kent Dybvig, Programming Language Design and
 *      Implementation (PLDI) 1996, pp.108-116.
 *
 * The previous implementation of this function combined m+ and m- into
 * one single M which caused some inaccuracy of the last digit. The
 * particular case below shows this inaccuracy:
 *
 *       System.out.println(new Double((1.234123412431233E107)).toString());
 *       System.out.println(new Double((1.2341234124312331E107)).toString());
 *       System.out.println(new Double((1.2341234124312332E107)).toString());
 *
 *       outputs the following:
 *
 *           1.234123412431233E107
 *           1.234123412431233E107
 *           1.234123412431233E107
 *
 *       instead of:
 *
 *           1.234123412431233E107
 *           1.2341234124312331E107
 *           1.2341234124312331E107
 *
 */
internal fun bigIntDigitGeneratorInstImpl(
    results: IntArray,
    uArray: IntArray,
    f: Long,
    e: Int,
    isDenormalized: Boolean,
    mantissaIsZero: Boolean,
    p: Int,
) {
    val R = ULongArray(RM_SIZE)
    val S = ULongArray(STemp_SIZE)
    val mplus = ULongArray(RM_SIZE)
    val mminus = ULongArray(RM_SIZE)
    val Temp = ULongArray(STemp_SIZE)

    val fULong = f.toULong()
    if (e >= 0) {
        R[0] = fULong
        mplus[0] = 1UL
        mminus[0] = 1UL
        simpleShiftLeftHighPrecision(mminus, RM_SIZE, e)
        if (fULong != (2UL shl (p - 1))) {
            simpleShiftLeftHighPrecision(R, RM_SIZE, e + 1)
            S[0] = 2UL
            /*
             * m+ = m+ << e results in 1.0e23 to be printed as
             * 0.9999999999999999E23
             * m+ = m+ << e+1 results in 1.0e23 to be printed as
             * 1.0e23 (caused too much rounding)
             *      470fffffffffffff = 2.0769187434139308E34
             *      4710000000000000 = 2.076918743413931E34
             */
            simpleShiftLeftHighPrecision(mplus, RM_SIZE, e)
        } else {
            simpleShiftLeftHighPrecision(R, RM_SIZE, e + 2)
            S[0] = 4UL
            simpleShiftLeftHighPrecision(mplus, RM_SIZE, e + 1)
        }
    } else {
        if (isDenormalized || (fULong != (2UL shl (p - 1)))) {
            R[0] = fULong shl 1
            S[0] = 1UL
            simpleShiftLeftHighPrecision(S, STemp_SIZE, 1 - e)
            mplus[0] = 1UL
            mminus[0] = 1UL
        } else {
            R[0] = fULong shl 2
            S[0] = 1UL
            simpleShiftLeftHighPrecision(S, STemp_SIZE, 2 - e)
            mplus[0] = 2UL
            mminus[0] = 1UL
        }
    }

    val k = ceil((e + p - 1) * INV_LOG_OF_TEN_BASE_2 - 1e-10).toInt()

    if (k > 0) {
        timesTenToTheEHighPrecision(S, STemp_SIZE, k)
    } else {
        timesTenToTheEHighPrecision(R, RM_SIZE, -k)
        timesTenToTheEHighPrecision(mplus, RM_SIZE, -k)
        timesTenToTheEHighPrecision(mminus, RM_SIZE, -k)
    }

    var RLength = RM_SIZE
    var SLength = STemp_SIZE
    var TempLength = STemp_SIZE
    var mplus_Length = RM_SIZE
    var mminus_Length = RM_SIZE

    R.copyInto(Temp, 0, 0, RM_SIZE)

    while (RLength > 1 && R[RLength - 1] == 0UL) RLength--
    while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0UL) mplus_Length--
    while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0UL) mminus_Length--
    while (SLength > 1 && S[SLength - 1] == 0UL) SLength--
    TempLength = (if (RLength > mplus_Length) RLength else mplus_Length) + 1
    addHighPrecision(Temp, TempLength, mplus, mplus_Length)

    var firstK: Int
    if (compareHighPrecision(Temp, TempLength, S, SLength) >= 0) {
        firstK = k
    } else {
        firstK = k - 1
        simpleAppendDecimalDigitHighPrecision(R, ++RLength, 0UL)
        simpleAppendDecimalDigitHighPrecision(mplus, ++mplus_Length, 0UL)
        simpleAppendDecimalDigitHighPrecision(mminus, ++mminus_Length, 0UL)
        while (RLength > 1 && R[RLength - 1] == 0UL)
            RLength--
        while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0UL)
            mplus_Length--
        while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0UL)
            mminus_Length--
    }

    var getCount = 0
    var setCount = 0
    var low: Boolean
    var high: Boolean
    var U: Int

    while (true) {
        U = 0
        for (i in 3 downTo 0) {
            TempLength = SLength + 1
            Temp[SLength] = 0UL
            S.copyInto(Temp, 0, 0, SLength)

            simpleShiftLeftHighPrecision(Temp, TempLength, i)
            if (compareHighPrecision(R, RLength, Temp, TempLength) >= 0) {
                subtractHighPrecision(R, RLength, Temp, TempLength)
                U += 1 shl i
            }
        }

        low = compareHighPrecision(R, RLength, mminus, mminus_Length) <= 0

        Temp.fill(0UL, RLength, STemp_SIZE)
        R.copyInto(Temp, 0, 0, RLength)
        TempLength = (if (RLength > mplus_Length) RLength else mplus_Length) + 1
        addHighPrecision(Temp, TempLength, mplus, mplus_Length)

        high = compareHighPrecision(Temp, TempLength, S, SLength) >= 0

        if (low || high)
            break

        simpleAppendDecimalDigitHighPrecision(R, ++RLength, 0UL)
        simpleAppendDecimalDigitHighPrecision(mplus, ++mplus_Length, 0UL)
        simpleAppendDecimalDigitHighPrecision(mminus, ++mminus_Length, 0UL)
        while (RLength > 1 && R[RLength - 1] == 0UL)
            --RLength
        while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0UL)
            --mplus_Length
        while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0UL)
            --mminus_Length
        uArray[setCount++] = U
    }

    simpleShiftLeftHighPrecision(R, ++RLength, 1)
    if (low && !high)
        uArray[setCount++] = U
    else if (high && !low)
        uArray[setCount++] = U + 1
    else if (compareHighPrecision(R, RLength, S, SLength) < 0)
        uArray[setCount++] = U
    else
        uArray[setCount++] = U + 1

    results[0] = setCount
    results[1] = getCount
    results[2] = firstK
}
