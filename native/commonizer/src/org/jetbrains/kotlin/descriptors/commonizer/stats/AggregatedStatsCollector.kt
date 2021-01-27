/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

import org.jetbrains.kotlin.descriptors.commonizer.stats.RawStatsCollector.CommonDeclarationStatus.*
import org.jetbrains.kotlin.konan.target.KonanTarget

class AggregatedStatsCollector(
    targets: List<KonanTarget>
) : StatsCollector {
    private val wrappedCollector = RawStatsCollector(targets)

    override fun logDeclaration(targetIndex: Int, lazyStatsKey: () -> StatsCollector.StatsKey) {
        wrappedCollector.logDeclaration(targetIndex, lazyStatsKey)
    }

    override fun writeTo(statsOutput: StatsOutput) {
        val aggregatingOutput = AggregatingOutput()
        wrappedCollector.writeTo(aggregatingOutput)

        statsOutput.use {
            statsOutput.writeHeader(AggregatedStatsHeader)

            aggregatingOutput.aggregatedStats.keys.sortedBy { it }.forEach { key ->
                val row = aggregatingOutput.aggregatedStats.getValue(key)
                statsOutput.writeRow(row)
            }
        }
    }

    object AggregatedStatsHeader : StatsOutput.StatsHeader {
        override fun toList(): List<String> = listOf(
            "Declaration Type",
            "Lifted Up",
            "Lifted Up, %%",
            "Commonized",
            "Commonized, %%",
            "Missing in s. targets",
            "Missing in s. targets, %%",
            "Failed: Other",
            "Failed: Other, %%",
            "Total"
        )
    }

    private class AggregatedStatsRow(
        private val declarationType: DeclarationType
    ) : StatsOutput.StatsRow {
        var liftedUp: Int = 0
        var successfullyCommonized: Int = 0
        var failedBecauseMissing: Int = 0
        var failedOther: Int = 0

        override fun toList(): List<String> {
            val total = liftedUp + successfullyCommonized + failedBecauseMissing + failedOther

            fun fraction(amount: Int): Double = if (total > 0) amount.toDouble() / total else 0.0

            return listOf(
                declarationType.alias,
                liftedUp.toString(),
                fraction(liftedUp).toString(),
                successfullyCommonized.toString(),
                fraction(successfullyCommonized).toString(),
                failedBecauseMissing.toString(),
                fraction(failedBecauseMissing).toString(),
                failedOther.toString(),
                fraction(failedOther).toString(),
                total.toString()
            )
        }
    }

    private class AggregatingOutput : StatsOutput {
        val aggregatedStats = HashMap<DeclarationType, AggregatedStatsRow>()

        override fun writeHeader(header: StatsOutput.StatsHeader) {
            check(header is RawStatsCollector.RawStatsHeader)
            // do nothing
        }

        override fun writeRow(row: StatsOutput.StatsRow) {
            check(row is RawStatsCollector.RawStatsRow)

            val declarationType = row.statsKey.declarationType
            val aggregatedStatsRow = aggregatedStats.getOrPut(declarationType) { AggregatedStatsRow(declarationType) }
            when (row.common) {
                LIFTED_UP -> aggregatedStatsRow.liftedUp++
                EXPECT -> aggregatedStatsRow.successfullyCommonized++
                MISSING -> {
                    if (row.platform.any { it == RawStatsCollector.PlatformDeclarationStatus.MISSING }) {
                        aggregatedStatsRow.failedBecauseMissing++
                    } else {
                        aggregatedStatsRow.failedOther++
                    }
                }
            }
        }

        override fun close() = Unit
    }
}
