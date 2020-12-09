/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.descriptors.commonizer.stats

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Closeable
import java.io.File

fun StatsCollector(type: StatsType, targets: List<KonanTarget>, destination: File): StatsCollector? {
    return when (type) {
        StatsType.RAW -> RawStatsCollector(targets, FileStatsOutput(destination, "raw"))
        StatsType.AGGREGATED -> AggregatedStatsCollector(targets, FileStatsOutput(destination, "aggregated"))
        StatsType.NONE -> null
    }
}

enum class StatsType {
    RAW, AGGREGATED, NONE;
}

interface StatsCollector : Closeable {
    fun logStats(result: List<DeclarationDescriptor?>)
}
