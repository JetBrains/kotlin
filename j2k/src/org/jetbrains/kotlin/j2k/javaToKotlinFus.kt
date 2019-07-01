/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger

enum class ConversionType(val text: String) {
    FILES("Files"), PSI_EXPRESSION("PSI expression"), TEXT_EXPRESSION("Text expression");
}

fun logJ2kConversionStatistics(
    type: ConversionType,
    isNewJ2k: Boolean,
    conversionTime: Long,
    linesCount: Int,
    filesCount: Int
) {
    val data = mapOf(
        "Lines count" to linesCount,
        "Files count" to filesCount,
        "Is new J2K" to isNewJ2k,
        "Time" to conversionTime
    ).map { (key, value) ->
        key to value.toString()
    }.toMap()

    KotlinFUSLogger.log(FUSEventGroups.J2K, type.text, data)
}

