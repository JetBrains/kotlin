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

import kotlin.math.pow

private const val MAX_ACCURACY_WIDTH_FLOAT = 8
private const val LOG5_OF_TWO_TO_THE_N_FLOAT = 11
private const val MANTISSA_MASK = 0x007FFFFFu
private const val EXPONENT_MASK = 0x7F800000u
private const val FLOAT_NORMAL_MASK = 0x00800000u
private const val E_OFFSET = 150

private val TENS = intArrayOf(
    0x3f800000, 0x41200000, 0x42c80000, 0x447a0000, 0x461c4000,
    0x47c35000, 0x49742400, 0x4b189680, 0x4cbebc20, 0x4e6e6b28, 0x501502f9
)

// Macro replacements as functions
private inline fun floatToIntBits(flt: Float): UInt = flt.toRawBits().toUInt()
private inline fun intBitsToFloat(bits: UInt): Float = Float.fromBits(bits.toInt())
private inline fun tenToTheEFloat(e: Int): Float = Float.fromBits(TENS[e])

private fun floatMantissa(z: Float): UInt {
    var m = floatToIntBits(z)

    if ((m and EXPONENT_MASK) != 0u)
        m = (m and MANTISSA_MASK) or FLOAT_NORMAL_MASK
    else
        m = (m and MANTISSA_MASK)

    return m
}

private fun floatExponent(z: Float): Int {
    /* assumes positive float */
    var k = (floatToIntBits(z) shr 23).toInt()
    if (k != 0)
        k -= E_OFFSET
    else
        k = 1 - E_OFFSET

    return k
}

private fun createFloat(s: String, e: Int): Float {
    var e = e
    /* assumes s is a string with at least one
     * character in it */
    val def = ULongArray(MAX_ACCURACY_WIDTH_FLOAT)
    val defBackup = ULongArray(MAX_ACCURACY_WIDTH_FLOAT)

    val f: ULongArray
    var fNoOverflow: ULongArray

    var overflow: UInt
    var result: Float
    var index = 1
    var unprocessedDigits: Int

    f = def
    fNoOverflow = defBackup
    f[0] = 0UL

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
            if (overflow != 0u) {

                f[index++] = overflow.toULong()
                /* There is an overflow, but there is no more room
                 * to store the result. We really only need the top 52
                 * bits anyway, so we must back out of the overflow,
                 * and ignore the rest of the string.
                 */
                if (index >= MAX_ACCURACY_WIDTH_FLOAT) {
                    index--
                    fNoOverflow.copyInto(f, 0, 0, index)
                    break
                }
            }
        } else
            index = -1
    } while (index > 0 && (++pS) < s.length)

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
            if (e <= 0) {
                result = createFloat1(f, index, e)
            } else {
                result = Float.POSITIVE_INFINITY
            }
        } else {
            result = Float.fromBits(index)
        }
    } else {
        if (index > -1) {
            result = createFloat1(f, index, e)
        } else {
            result = Float.fromBits(index)
        }
    }

    return result

}

private fun createFloat1(f: ULongArray, length: Int, e: Int): Float {
    val numBits: Int
    val dresult: Double
    var result = 0f

    numBits = highestSetBitHighPrecision(f, length) + 1
    if (numBits < 25 && e >= 0 && e < LOG5_OF_TWO_TO_THE_N_FLOAT) {
        return lowU32FromPtr(f, 0).toFloat() * tenToTheEFloat(e)
    } else if (numBits < 25 && e < 0 && (-e) < LOG5_OF_TWO_TO_THE_N_FLOAT) {
        return lowU32FromPtr(f, 0).toFloat() / tenToTheEFloat(-e)
    } else if (e >= 0 && e < 39) {
        result = (toDoubleHighPrecision(f, length) * 10.0.pow(e)).toFloat()
    } else if (e >= 39) {
        /* Convert the partial result to make sure that the
         * non-exponential part is not zero. This check fixes the case
         * where the user enters 0.0e309! */
        result = toDoubleHighPrecision(f, length).toFloat()

        if (result == 0f)
            result = Float.MIN_VALUE
        else
            result = Float.POSITIVE_INFINITY
    } else if (e > -309) {
        var dexp: Int
        var fmant: UInt
        var fovfl: UInt
        val dmant: ULong
        dresult = toDoubleHighPrecision(f, length) / 10.0.pow(-e)

        if (isDenormalDouble(dresult)) {
            result = 0f
            return result
        }
        dexp = doubleExponent(dresult) + 51
        dmant = doubleMantissa(dresult)
        /* Is it too small to be represented by a single-precision
         * float? */
        if (dexp <= -155) {
            result = 0f
            return result
        }
        /* Is it a denormalized single-precision float? */
        if ((dexp <= -127) && (dexp > -155)) {
            /* Only interested in 24 msb bits of the 53-bit double mantissa */
            fmant = (dmant shr 29).toUInt()
            fovfl = ((dmant and 0x1FFFFFFFUL).toUInt()) shl 3
            while ((dexp < -127) && ((fmant or fovfl) != 0u)) {
                if ((fmant and 1u) != 0u) {
                    fovfl = fovfl or 0x80000000u
                }
                fovfl = fovfl shr 1
                fmant = fmant shr 1
                dexp++
            }
            if ((fovfl and 0x80000000u) != 0u) {
                if ((fovfl and 0x7FFFFFFCu) != 0u) {
                    fmant++
                } else if ((fmant and 1u) != 0u) {
                    fmant++
                }
            } else if ((fovfl and 0x40000000u) != 0u) {
                if ((fovfl and 0x3FFFFFFCu) != 0u) {
                    fmant++
                }
            }
            result = Float.fromBits(fmant.toInt())
        } else {
            result = dresult.toFloat()
        }
    }

    /* Don't go straight to zero as the fact that x*0 = 0 independent
     * of x might cause the algorithm to produce an incorrect result.
     * Instead try the min  value first and let it fall to zero if need
     * be.
     */
    if (e <= -309 || result.toRawBits() == 0)
        result = Float.MIN_VALUE

    return floatAlgorithm(f, length, e, result)
}


/* The algorithm for the function floatAlgorithm() below can be found
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
private fun floatAlgorithm(f: ULongArray, length: Int, e: Int, z: Float): Float {
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

    var decApproxCount = 0
    var incApproxCount = 0

    do {
        m = floatMantissa(z).toULong()
        k = floatExponent(z)

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
            if (comparison < 0 && m == FLOAT_NORMAL_MASK.toULong()) {
                simpleShiftLeftHighPrecision(D2, D2Length, 1)
                if (compareHighPrecision(D2, D2Length, y, yLength) > 0) {
                    // DECREMENT_FLOAT (z, decApproxCount, incApproxCount) macro expansion
                    z = intBitsToFloat((floatToIntBits(z) - 1u))
                    decApproxCount++
                    if (incApproxCount > 2 && decApproxCount > 2) {
                        z = if (decApproxCount > incApproxCount)
                            intBitsToFloat((floatToIntBits(z) + (decApproxCount - incApproxCount).toUInt()))
                        else
                            intBitsToFloat((floatToIntBits(z) - (incApproxCount - decApproxCount).toUInt()))
                        break
                    }
                    // End DECREMENT_FLOAT macro expansion
                } else {
                    break
                }
            } else {
                break
            }
        } else if (comparison2 == 0) {
            if ((m and 1UL) == 0UL) {
                if (comparison < 0 && m == FLOAT_NORMAL_MASK.toULong()) {
                    // DECREMENT_FLOAT (z, decApproxCount, incApproxCount) macro expansion
                    z = intBitsToFloat((floatToIntBits(z) - 1u))
                    decApproxCount++
                    if (incApproxCount > 2 && decApproxCount > 2) {
                        z = if (decApproxCount > incApproxCount)
                            intBitsToFloat((floatToIntBits(z) + (decApproxCount - incApproxCount).toUInt()))
                        else
                            intBitsToFloat((floatToIntBits(z) - (incApproxCount - decApproxCount).toUInt()))
                        break
                    }
                    // End DECREMENT_FLOAT macro expansion
                } else {
                    break
                }
            } else if (comparison < 0) {
                // DECREMENT_FLOAT (z, decApproxCount, incApproxCount) macro expansion
                z = intBitsToFloat((floatToIntBits(z) - 1u))
                decApproxCount++
                if (incApproxCount > 2 && decApproxCount > 2) {
                    z = if (decApproxCount > incApproxCount)
                        intBitsToFloat((floatToIntBits(z) + (decApproxCount - incApproxCount).toUInt()))
                    else
                        intBitsToFloat((floatToIntBits(z) - (incApproxCount - decApproxCount).toUInt()))
                    break
                }
                // End DECREMENT_FLOAT macro expansion
                break
            } else {
                // INCREMENT_FLOAT (z, decApproxCount, incApproxCount) macro expansion
                z = intBitsToFloat((floatToIntBits(z) + 1u))
                incApproxCount++
                if (incApproxCount > 2 && decApproxCount > 2) {
                    z = if (decApproxCount > incApproxCount)
                        intBitsToFloat((floatToIntBits(z) + (decApproxCount - incApproxCount).toUInt()))
                    else
                        intBitsToFloat((floatToIntBits(z) - (incApproxCount - decApproxCount).toUInt()))
                    break
                }
                // End INCREMENT_FLOAT macro expansion
                break
            }
        } else if (comparison < 0) {
            // DECREMENT_FLOAT (z, decApproxCount, incApproxCount) macro expansion
            z = intBitsToFloat((floatToIntBits(z) - 1u))
            decApproxCount++
            if (incApproxCount > 2 && decApproxCount > 2) {
                z = if (decApproxCount > incApproxCount)
                    intBitsToFloat((floatToIntBits(z) + (decApproxCount - incApproxCount).toUInt()))
                else
                    intBitsToFloat((floatToIntBits(z) - (incApproxCount - decApproxCount).toUInt()))
                break
            }
            // End DECREMENT_FLOAT macro expansion
        } else {
            if (z.toRawBits() == EXPONENT_MASK.toInt())
                break
            // INCREMENT_FLOAT (z, decApproxCount, incApproxCount) macro expansion
            z = intBitsToFloat((floatToIntBits(z) + 1u))
            incApproxCount++
            if (incApproxCount > 2 && decApproxCount > 2) {
                z = if (decApproxCount > incApproxCount)
                    intBitsToFloat((floatToIntBits(z) + (decApproxCount - incApproxCount).toUInt()))
                else
                    intBitsToFloat((floatToIntBits(z) - (incApproxCount - decApproxCount).toUInt()))
                break
            }
            // End INCREMENT_FLOAT macro expansion
        }
    } while (true)

    return z
}

internal fun parseFloatImpl(s: String, e: Int): Float {
    val flt = createFloat(s, e)
    val bits = floatToIntBits(flt)
    if (bits.toInt() >= 0) return flt
    if (bits.toInt() == -1) throw NumberFormatException("Invalid float format")
    throw RuntimeException()
}
