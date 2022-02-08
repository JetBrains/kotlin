/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.*

internal actual inline val durationAssertionsEnabled: Boolean get() = true

@JsFun("(value, decimals) => value.toFixed(decimals)")
private external fun toFixed(value: Double, decimals: Int): String

@JsFun("(value, decimals) => value.toPrecision(decimals)")
private external fun toPrecision(value: Double, decimals: Int): String

internal actual fun formatToExactDecimals(value: Double, decimals: Int): String {
    val rounded = if (decimals == 0) {
        value
    } else {
        val pow = (10.0).pow(decimals)
        round(abs(value) * pow) / pow * sign(value)
        round(abs(value) * pow) / pow * sign(value)
    }
    return if (abs(rounded) < 1e21) {
        // toFixed switches to scientific format after 1e21
        toFixed(rounded, decimals)
    } else {
        // toPrecision outputs the specified number of digits, but only for positive numbers
        val positive = abs(rounded)
        val positiveString = toPrecision(positive, ceil(log10(positive)).toInt() + decimals)
        if (rounded < 0) "-$positiveString" else positiveString
    }
}

@JsFun("(value, decimals) => value.toLocaleString(\"en-us\", ({\"maximumFractionDigits\": decimals}))")
external internal actual fun formatUpToDecimals(value: Double, decimals: Int): String