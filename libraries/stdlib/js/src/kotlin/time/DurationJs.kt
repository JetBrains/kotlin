/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.js.json
import kotlin.math.*

internal actual inline val durationAssertionsEnabled: Boolean get() = true

internal actual fun formatToExactDecimals(value: Double, decimals: Int): String {
    val rounded = if (decimals == 0) {
        value
    } else {
        val pow = 10.0.pow(decimals)
        JsMath.round(abs(value) * pow) / pow * sign(value)
    }
    return if (abs(rounded) < 1e21) {
        // toFixed switches to scientific format after 1e21
        rounded.asDynamic().toFixed(decimals).unsafeCast<String>()
    } else {
        // toPrecision outputs the specified number of digits, but only for positive numbers
        val positive = abs(rounded)
        val positiveString = positive.asDynamic().toPrecision(ceil(log10(positive)) + decimals).unsafeCast<String>()
        if (rounded < 0) "-$positiveString" else positiveString
    }
}
