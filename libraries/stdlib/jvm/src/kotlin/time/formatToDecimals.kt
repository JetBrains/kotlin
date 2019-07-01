/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.concurrent.getOrSet

private val rootNegativeExpFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e" }
private val rootPositiveExpFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e+" }
private val precisionFormats = Array(4) { ThreadLocal<DecimalFormat>() }

private fun createFormatForDecimals(decimals: Int) = DecimalFormat("0", rootNegativeExpFormatSymbols).apply {
    if (decimals > 0) minimumFractionDigits = decimals
    roundingMode = RoundingMode.HALF_UP
}

internal actual fun formatToExactDecimals(value: Double, decimals: Int): String {
    val format = if (decimals < precisionFormats.size) {
        precisionFormats[decimals].getOrSet { createFormatForDecimals(decimals) }
    } else
        createFormatForDecimals(decimals)
    return format.format(value)
}

internal actual fun formatUpToDecimals(value: Double, decimals: Int): String =
    createFormatForDecimals(0)
        .apply { maximumFractionDigits = decimals }
        .format(value)

private val scientificFormat = ThreadLocal<DecimalFormat>()
internal actual fun formatScientific(value: Double): String =
    scientificFormat.getOrSet { DecimalFormat("0E0", rootNegativeExpFormatSymbols).apply { minimumFractionDigits = 2 } }
        .apply {
            decimalFormatSymbols = if (value >= 1 || value <= -1) rootPositiveExpFormatSymbols else rootNegativeExpFormatSymbols
        }
        .format(value)