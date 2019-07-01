/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.js.json
import kotlin.math.*

internal actual fun formatToExactDecimals(value: Double, decimals: Int): String {
    val rounded = if (decimals == 0) {
        value
    } else {
        val pow = 10.0.pow(decimals)
        @Suppress("DEPRECATION", "DEPRECATION_ERROR")
        kotlin.js.Math.round(abs(value) * pow) / pow * sign(value)
    }
    return rounded.asDynamic().toFixed(decimals).unsafeCast<String>()
}

internal actual fun formatUpToDecimals(value: Double, decimals: Int): String {
    return value.asDynamic().toLocaleString("en-us", json("maximumFractionDigits" to decimals)).unsafeCast<String>()
}

internal actual fun formatScientific(value: Double): String {
    return value.asDynamic().toExponential(2).unsafeCast<String>()
}