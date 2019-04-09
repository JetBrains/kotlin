/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

private val rootFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e" }
private val precisionFormats = Array<DecimalFormat?>(4) { null }

private fun createFormatForDecimals(decimals: Int) = DecimalFormat("0", rootFormatSymbols).apply {
    if (decimals > 0) minimumFractionDigits = decimals
    roundingMode = RoundingMode.HALF_UP
}

internal actual fun formatToExactDecimals(value: Double, decimals: Int): String {
    val format = if (decimals < precisionFormats.size) {
        precisionFormats[decimals] ?: createFormatForDecimals(decimals).also { precisionFormats[decimals] = it }
    } else
        createFormatForDecimals(decimals)
    return format.format(value)
}

internal actual fun formatUpToDecimals(value: Double, decimals: Int): String =
    createFormatForDecimals(0)
        .apply { maximumFractionDigits = decimals }
        .format(value)

private val scientificFormat = DecimalFormat("0E0", rootFormatSymbols).apply { maximumFractionDigits = 2 }
internal actual fun formatScientific(value: Double): String =
    scientificFormat.format(value)