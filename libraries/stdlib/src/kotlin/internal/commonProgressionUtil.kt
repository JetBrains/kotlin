/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

internal fun progressionUnsignedDivide(dividend: /*U*/Int, divisor: Int /* > 0 */): /*U*/Int =
    if (divisor == 1)
        dividend
    else
        ((dividend.toLong() and 0xFFFF_FFFFL) / divisor.toLong()).toInt()

internal fun progressionUnsignedDivide(dividend: /*U*/Long, divisor: Long /* > 0 */): /*U*/Long = when {
    divisor == 1L -> dividend
    dividend >= 0 -> dividend / divisor
    else -> {
        val quotient = ((dividend ushr 1) / divisor) shl 1
        val rem = dividend - quotient * divisor
        quotient + if ((rem xor Long.MIN_VALUE) >= (divisor xor Long.MIN_VALUE)) 1 else 0
    }
}

