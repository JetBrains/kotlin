/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.concurrent.getOrSet

internal actual val durationAssertionsEnabled: Boolean = Duration::class.java.desiredAssertionStatus()

private val precisionFormats = Array(4) { ThreadLocal<DecimalFormat>() }

private fun createFormatForDecimals(decimals: Int) = DecimalFormat("0").apply {
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
