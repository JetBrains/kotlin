/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

import com.intellij.util.containers.FactoryMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector.StatsKey
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsOutput.StatsHeader
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsOutput.StatsRow
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*
import kotlin.collections.ArrayList

/**
 * Allows printing commonization statistics to the file system.
 *
 * Output format is defined in [StatsOutput].
 *
 * Header row: "ID, Extension Receiver, Parameter Names, Parameter Types, Declaration Type, common, <platform1>, <platform2> [<platformN>...]"
 *
 * Possible values for "Declaration Type":
 * - MODULE
 * - CLASS
 * - INTERFACE
 * - OBJECT
 * - COMPANION_OBJECT
 * - ENUM_CLASS
 * - ENUM_ENTRY
 * - TYPE_ALIAS
 * - CLASS_CONSTRUCTOR
 * - FUN
 * - VAL
 *
 * Possible values for "common" column:
 * - L = declaration lifted up to common fragment
 * - E = successfully commonized, expect declaration generated
 * - "-" = no common declaration
 *
 * Possible values for each target platform column:
 * - A = successfully commonized, actual declaration generated
 * - O = not commonized, the declaration is as in the original library
 * - "-" = no such declaration in the original library (or declaration has been lifted up)
 *
 * Example of output:

ID|Extension Receiver|Parameter Names|Parameter Types|Declaration Type|common|macos_x64|ios_x64
SystemConfiguration||||MODULE|E|A|A
platform/SystemConfiguration/SCPreferencesContext||||CLASS|E|A|A
platform/SystemConfiguration/SCPreferencesContext.Companion||||COMPANION_OBJECT|E|A|A
platform/SystemConfiguration/SCNetworkConnectionContext||||CLASS|E|A|A
platform/SystemConfiguration/SCNetworkConnectionContext.Companion||||COMPANION_OBJECT|E|A|A
platform/SystemConfiguration/SCDynamicStoreRefVar||||TYPE_ALIAS|-|O|O
platform/SystemConfiguration/SCVLANInterfaceRef||||TYPE_ALIAS|-|O|O

 */
class RawStatsCollector(private val targets: List<KonanTarget>) : StatsCollector {
    private inline val dimension get() = targets.size + 1
    private inline val targetNames get() = targets.map { it.name }

    private inline val indexOfCommon get() = targets.size
    private inline val platformDeclarationsCount get() = targets.size

    private val stats = FactoryMap.create<StatsKey, StatsValue> { StatsValue(dimension) }

    override fun logDeclaration(targetIndex: Int, lazyStatsKey: () -> StatsKey) {
        stats.getValue(lazyStatsKey())[targetIndex] = true
    }

    override fun writeTo(statsOutput: StatsOutput) {
        val mergedStats = stats.filterTo(mutableMapOf()) { (statsKey, _) ->
            when (statsKey.declarationType) {
                DeclarationType.TOP_LEVEL_CLASS, DeclarationType.TOP_LEVEL_INTERFACE -> false
                else -> true
            }
        }

        stats.forEach { (statsKey, statsValue) ->
            when (statsKey.declarationType) {
                DeclarationType.TOP_LEVEL_CLASS, DeclarationType.TOP_LEVEL_INTERFACE -> {
                    if (statsValue[indexOfCommon]) {
                        val alternativeKey = statsKey.copy(declarationType = DeclarationType.TYPE_ALIAS)
                        val alternativeValue = mergedStats[alternativeKey]
                        if (alternativeValue != null && !alternativeValue[indexOfCommon]) {
                            alternativeValue[indexOfCommon] = true
                            return@forEach
                        }
                    }

                    mergedStats[statsKey] = statsValue
                }
                else -> Unit
            }
        }

        statsOutput.use {
            statsOutput.writeHeader(RawStatsHeader(targetNames))

            mergedStats.forEach { (statsKey, statsValue) ->
                val commonIsMissing = !statsValue[indexOfCommon]

                var isLiftedUp = !commonIsMissing
                val platform = ArrayList<PlatformDeclarationStatus>(platformDeclarationsCount)

                for (index in 0 until platformDeclarationsCount) {
                    platform += when {
                        !statsValue[index] -> PlatformDeclarationStatus.MISSING
                        commonIsMissing -> PlatformDeclarationStatus.ORIGINAL
                        else -> {
                            isLiftedUp = false
                            PlatformDeclarationStatus.ACTUAL
                        }
                    }
                }

                val common = when {
                    isLiftedUp -> CommonDeclarationStatus.LIFTED_UP
                    commonIsMissing -> CommonDeclarationStatus.MISSING
                    else -> CommonDeclarationStatus.EXPECT
                }

                statsOutput.writeRow(RawStatsRow(statsKey, common, platform))
            }
        }
    }

    class RawStatsHeader(private val targetNames: List<String>) : StatsHeader {
        override fun toList() =
            listOf("ID", "Extension Receiver", "Parameter Names", "Parameter Types", "Declaration Type", "common") + targetNames
    }

    class RawStatsRow(
        val statsKey: StatsKey,
        val common: CommonDeclarationStatus,
        val platform: List<PlatformDeclarationStatus>
    ) : StatsRow {
        override fun toList(): List<String> {
            val result = mutableListOf(
                statsKey.id,
                statsKey.extensionReceiver.orEmpty(),
                statsKey.parameterNames.joinToString(),
                statsKey.parameterTypes.joinToString(),
                statsKey.declarationType.alias,
                common.alias.toString()
            )

            platform.mapTo(result) { it.alias.toString() }

            return result
        }
    }

    enum class CommonDeclarationStatus(val alias: Char) {
        LIFTED_UP('L'),
        EXPECT('E'),
        MISSING('-')
    }

    enum class PlatformDeclarationStatus(val alias: Char) {
        ACTUAL('A'),
        ORIGINAL('O'),
        MISSING('-')
    }
}

private typealias StatsValue = BitSet
