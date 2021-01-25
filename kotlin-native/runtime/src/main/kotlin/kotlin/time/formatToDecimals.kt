/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.native.internal.GCCritical


@SymbolName("Kotlin_DurationValue_formatToExactDecimals")
@GCCritical
internal actual external fun formatToExactDecimals(value: Double, decimals: Int): String

internal actual fun formatUpToDecimals(value: Double, decimals: Int): String {
    return formatToExactDecimals(value, decimals).trimEnd('0')
}

@SymbolName("Kotlin_DurationValue_formatScientificImpl")
@GCCritical
internal external fun formatScientificImpl(value: Double): String

internal actual fun formatScientific(value: Double): String {
    val result = formatScientificImpl(value)
    val expIndex = result.indexOf("e+0")
    return if (expIndex < 0) result else result.removeRange(expIndex + 2, expIndex + 3)
}