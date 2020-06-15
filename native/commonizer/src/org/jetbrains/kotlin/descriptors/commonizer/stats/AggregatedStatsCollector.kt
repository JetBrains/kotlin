/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.stats.AggregatedStatsCollector.AggregatedStatsRow
import org.jetbrains.kotlin.descriptors.commonizer.stats.RawStatsCollector.CommonDeclarationStatus.*
import org.jetbrains.kotlin.konan.target.KonanTarget

class AggregatedStatsCollector(
    targets: List<KonanTarget>,
    private val output: StatsOutput
) : StatsCollector {
    private val aggregatingOutput = AggregatingOutput()
    private val wrappedCollector = RawStatsCollector(targets, aggregatingOutput)

    override fun logStats(result: List<DeclarationDescriptor?>) {
        wrappedCollector.logStats(result)
    }

    override fun close() {
        output.writeHeader(AggregatedStatsHeader)

        aggregatingOutput.aggregatedStats.keys.sortedBy { it }.forEach { key ->
            val row = aggregatingOutput.aggregatedStats.getValue(key)
            output.writeRow(row)
        }

        output.close()
        wrappedCollector.close()
    }

    object AggregatedStatsHeader : StatsOutput.StatsHeader {
        private val headerItems = listOf(
            "Declaration Type",
            "Lifted Up",
            "Lifted Up, %%",
            "Commonized",
            "Commonized, %%",
            "Missed in s. targets",
            "Missed in s. targets, %%",
            "Failed: Other",
            "Failed: Other, %%",
            "Total"
        )

        override fun toList(): List<String> = headerItems
    }

    class AggregatedStatsRow(
        private val declarationType: DeclarationType
    ) : StatsOutput.StatsRow {
        var liftedUp: Int = 0
        var successfullyCommonized: Int = 0
        var failedBecauseAbsent: Int = 0
        var failedOther: Int = 0

        override fun toList(): List<String> {
            val total = liftedUp + successfullyCommonized + failedBecauseAbsent + failedOther

            fun fraction(amount: Int): Double = if (total > 0) amount.toDouble() / total else 0.0

            return listOf(
                declarationType.alias,
                liftedUp.toString(),
                fraction(liftedUp).toString(),
                successfullyCommonized.toString(),
                fraction(successfullyCommonized).toString(),
                failedBecauseAbsent.toString(),
                fraction(failedBecauseAbsent).toString(),
                failedOther.toString(),
                fraction(failedOther).toString(),
                total.toString()
            )
        }
    }

}

@Suppress("MoveVariableDeclarationIntoWhen")
private class AggregatingOutput : StatsOutput {
    val aggregatedStats = HashMap<DeclarationType, AggregatedStatsRow>()

    override fun writeHeader(header: StatsOutput.StatsHeader) {
        check(header is RawStatsCollector.RawStatsHeader)
        // do nothing
    }

    override fun writeRow(row: StatsOutput.StatsRow) {
        check(row is RawStatsCollector.RawStatsRow)

        val aggregatedStatsRow = aggregatedStats.computeIfAbsent(row.declarationType, ::AggregatedStatsRow)
        when (row.common) {
            LIFTED_UP -> aggregatedStatsRow.liftedUp++
            EXPECT -> aggregatedStatsRow.successfullyCommonized++
            ABSENT -> {
                if (row.platform.any { it == RawStatsCollector.PlatformDeclarationStatus.ABSENT }) {
                    aggregatedStatsRow.failedBecauseAbsent++
                } else {
                    aggregatedStatsRow.failedOther++
                }
            }
        }
    }

    override fun close() = Unit
}
