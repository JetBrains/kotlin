/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.fdlibm.__ieee754_pow

internal actual fun formatToExactDecimals(value: Double, decimals: Int): String {
    // TODO Make correct implementation (KT-60964)
    val pow = __ieee754_pow(10.0, decimals.toDouble())
    val round = kotlin.math.fdlibm.rint(value * pow)
    return (round / pow).toString()
}

internal actual inline val durationAssertionsEnabled: Boolean get() = true