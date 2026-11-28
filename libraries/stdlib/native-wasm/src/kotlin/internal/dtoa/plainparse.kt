/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlin.internal.dtoa

/*
 * A fast path for the most common floating-point literals: plain decimals like "0.1234567890", "-42" or "6.02e23".
 *
 * The general parser trims the string, normalizes the digits into a new string and accumulates them in a big integer
 * before it decides how to compute the result. For a short decimal that is exactly representable in a Long,
 * the result can be computed directly (Clinger, "How to Read Floating-Point Numbers Accurately", 1990):
 * when both the mantissa and the power of ten are exactly representable in the target type,
 * a single IEEE multiplication or division of them is correctly rounded.
 *
 * Any string this fast path does not handle (whitespace, a type suffix, hex, named values, ".5" or "1." forms,
 * long mantissas, large exponents, invalid input) is left to the general parser unchanged.
 */

/** 10^0 .. 10^22, every one of them is exactly representable as a Double. 10^0 .. 10^10 are also exact as Float. */
private val POWERS_OF_TEN = doubleUnsafeArrayOf(
    1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11,
    1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22,
)

/** Integers up to 2^53 are exactly representable as a Double. */
private const val DOUBLE_MAX_EXACT_MANTISSA = 1L shl 53

/** The largest power of ten exactly representable as a Double. */
private const val DOUBLE_MAX_EXACT_POWER_OF_TEN = 22

/** Integers up to 2^24 are exactly representable as a Float. */
private const val FLOAT_MAX_EXACT_MANTISSA = 1L shl 24

/** The largest power of ten exactly representable as a Float. */
private const val FLOAT_MAX_EXACT_POWER_OF_TEN = 10

/**
 * Bounds the exponent and fraction digit count while scanning, so that computing the decimal power cannot overflow.
 * Anything beyond it is far outside the fast path range anyway.
 */
private const val MAX_SCANNED_POWER = 10_000

/**
 * Parses [s] if it has the form `[+-]? digits ('.' digits)? ([eE] [+-]? digits)?`
 * and its value is `±mantissa * 10^power` with `mantissa <= maxMantissa`.
 * Calls [onSuccess] with the sign, the mantissa and the decimal power in that case, and returns [notApplicable] otherwise.
 */
private inline fun <T> parsePlainDecimal(
    s: String,
    maxMantissa: Long,
    notApplicable: T,
    onSuccess: (negative: Boolean, mantissa: Long, power: Int) -> T,
): T {
    val end = s.length
    if (end == 0) return notApplicable
    var i = 0

    val first = s[0]
    val negative = first == '-'
    if (negative || first == '+') i++

    // Integer part: at least one digit is required.
    var mantissa = 0L
    val integerStart = i
    while (i < end) {
        val digit = s[i] - '0'
        if (digit < 0 || digit > 9) break
        mantissa = mantissa * 10 + digit
        // mantissa <= maxMantissa <= 2^53 before this step, so the Long can't overflow.
        if (mantissa > maxMantissa) return notApplicable
        i++
    }
    if (i == integerStart) return notApplicable

    // Fraction part: if there is a dot, at least one digit is required after it.
    var fractionDigits = 0
    if (i < end && s[i] == '.') {
        i++
        val fractionStart = i
        while (i < end) {
            val digit = s[i] - '0'
            if (digit < 0 || digit > 9) break
            mantissa = mantissa * 10 + digit
            if (mantissa > maxMantissa) return notApplicable
            i++
        }
        fractionDigits = i - fractionStart
        if (fractionDigits == 0 || fractionDigits > MAX_SCANNED_POWER) return notApplicable
    }

    // Exponent part: if there is an exponent separator, at least one digit is required after it and an optional sign.
    var exponent = 0
    if (i < end && (s[i] == 'e' || s[i] == 'E')) {
        i++
        var negativeExponent = false
        if (i < end && (s[i] == '-' || s[i] == '+')) {
            negativeExponent = s[i] == '-'
            i++
        }
        val exponentStart = i
        while (i < end) {
            val digit = s[i] - '0'
            if (digit < 0 || digit > 9) break
            exponent = exponent * 10 + digit
            if (exponent > MAX_SCANNED_POWER) return notApplicable
            i++
        }
        if (i == exponentStart) return notApplicable
        if (negativeExponent) exponent = -exponent
    }

    // Trailing characters (a type suffix, whitespace, anything invalid) are left to the general parser.
    if (i != end) return notApplicable

    return onSuccess(negative, mantissa, exponent - fractionDigits)
}

/**
 * Returns the Double value of [s] if it is a plain decimal whose value can be computed exactly with the fast path,
 * or `NaN` if [s] has to be parsed with the general parser. The fast path never produces `NaN` itself.
 */
internal fun parsePlainDoubleOrNaN(s: String): Double =
    parsePlainDecimal(s, DOUBLE_MAX_EXACT_MANTISSA, Double.NaN) { negative, mantissa, power ->
        // Both the mantissa and the power of ten are exact doubles, so a single operation is correctly rounded.
        val m = mantissa.toDouble()
        val value = when {
            power == 0 -> m
            power > 0 -> if (power <= DOUBLE_MAX_EXACT_POWER_OF_TEN) m * POWERS_OF_TEN[power] else return Double.NaN
            else -> if (power >= -DOUBLE_MAX_EXACT_POWER_OF_TEN) m / POWERS_OF_TEN[-power] else return Double.NaN
        }
        if (negative) -value else value
    }

/**
 * Returns the Float value of [s] if it is a plain decimal whose value can be computed exactly with the fast path,
 * or `NaN` if [s] has to be parsed with the general parser. The fast path never produces `NaN` itself.
 */
internal fun parsePlainFloatOrNaN(s: String): Float =
    parsePlainDecimal(s, FLOAT_MAX_EXACT_MANTISSA, Float.NaN) { negative, mantissa, power ->
        // Both the mantissa and the power of ten are exact floats, so a single operation is correctly rounded.
        val m = mantissa.toFloat()
        val value = when {
            power == 0 -> m
            power > 0 -> if (power <= FLOAT_MAX_EXACT_POWER_OF_TEN) m * POWERS_OF_TEN[power].toFloat() else return Float.NaN
            else -> if (power >= -FLOAT_MAX_EXACT_POWER_OF_TEN) m / POWERS_OF_TEN[-power].toFloat() else return Float.NaN
        }
        if (negative) -value else value
    }
