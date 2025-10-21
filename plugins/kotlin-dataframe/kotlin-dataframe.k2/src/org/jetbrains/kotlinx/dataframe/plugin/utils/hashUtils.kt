/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.text
import kotlin.math.abs

internal fun CallInfo.twoDigitHash(): String {
    val hash = run {
        val hash = name.hashCode() + arguments.sumOf {
            when (it) {
                is FirLiteralExpression -> it.value.hashCode()
                else -> it.source?.text?.hashCode() ?: 42
            }
        }
        hashToTwoCharString(abs(hash))
    }
    return hash
}

internal fun hashToTwoCharString(hash: Int): String {
    val baseChars = "0123456789"
    val base = baseChars.length
    val positiveHash = abs(hash)
    val char1 = baseChars[positiveHash % base]
    val char2 = baseChars[(positiveHash / base) % base]

    return "$char1$char2"
}