/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.stats.DeclarationType.Companion.declarationType
import org.jetbrains.kotlin.descriptors.commonizer.utils.firstNonNull
import org.jetbrains.kotlin.descriptors.commonizer.utils.fqNameWithTypeParameters
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * Allows printing commonization statistics to the file system.
 *
 * Output format is defined in [RawStatsCollector.output].
 *
 * Header row: "FQ Name, Extension Receiver, Parameter Names, Parameter Types, Declaration Type, common, <platform1>, <platform2> [<platformN>...]"
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

FQ Name|Extension Receiver|Parameter Names|Parameter Types|Declaration Type|common|macos_x64|ios_x64
<SystemConfiguration>||||MODULE|E|A|A
platform.SystemConfiguration.SCPreferencesContext||||CLASS|E|A|A
platform.SystemConfiguration.SCPreferencesContext.Companion||||COMPANION_OBJECT|E|A|A
platform.SystemConfiguration.SCNetworkConnectionContext||||CLASS|E|A|A
platform.SystemConfiguration.SCNetworkConnectionContext.Companion||||COMPANION_OBJECT|E|A|A
platform.SystemConfiguration.SCDynamicStoreRefVar||||TYPE_ALIAS|-|O|O
platform.SystemConfiguration.SCVLANInterfaceRef||||TYPE_ALIAS|-|O|O

 */
class RawStatsCollector(
    private val targets: List<KonanTarget>,
    private val output: StatsOutput
) : StatsCollector {
    private var headerWritten = false

    override fun logStats(result: List<DeclarationDescriptor?>) {
        if (!headerWritten) {
            writeHeader()
            headerWritten = true
        }

        val firstNotNull = result.firstNonNull()
        val lastIsNull = result.last() == null

        val statsRow = RawStatsRow(
            dimension = targets.size,
            fqName = if (firstNotNull is ModuleDescriptor) firstNotNull.name.asString() else firstNotNull.fqNameSafe.asString(),
            declarationType = firstNotNull.declarationType
        )

        // extension receiver
        if (firstNotNull is PropertyDescriptor || firstNotNull is SimpleFunctionDescriptor) {
            statsRow.extensionReceiver =
                (firstNotNull as CallableDescriptor).extensionReceiverParameter?.type?.fqNameWithTypeParameters.orEmpty()
        }

        if (firstNotNull is ConstructorDescriptor || firstNotNull is SimpleFunctionDescriptor) {
            val functionDescriptor = (firstNotNull as FunctionDescriptor)
            // parameter names
            statsRow.parameterNames = functionDescriptor.valueParameters.joinToString { it.name.asString() }
            // parameter types
            statsRow.parameterTypes = functionDescriptor.valueParameters.joinToString { it.type.fqNameWithTypeParameters }
        }

        var isLiftedUp = !lastIsNull
        for (index in 0 until result.size - 1) {
            statsRow.platform += when {
                result[index] == null -> PlatformDeclarationStatus.ABSENT
                lastIsNull -> PlatformDeclarationStatus.ORIGINAL
                else -> {
                    isLiftedUp = false
                    PlatformDeclarationStatus.ACTUAL
                }
            }
        }

        statsRow.common = when {
            isLiftedUp -> CommonDeclarationStatus.LIFTED_UP
            lastIsNull -> CommonDeclarationStatus.ABSENT
            else -> CommonDeclarationStatus.EXPECT
        }

        output.writeRow(statsRow)
    }

    override fun close() {
        output.close()
    }

    private fun writeHeader() {
        output.writeHeader(RawStatsHeader(targets.map { it.name }))
    }

    class RawStatsHeader(
        private val targetNames: List<String>
    ) : StatsOutput.StatsHeader {
        override fun toList(): List<String> = mutableListOf<String>().apply {
            this += "FQ Name"
            this += "Extension Receiver"
            this += "Parameter Names"
            this += "Parameter Types"
            this += "Declaration Type"
            this += "common"
            this += targetNames
        }
    }

    class RawStatsRow(
        dimension: Int,
        val fqName: String,
        val declarationType: DeclarationType
    ) : StatsOutput.StatsRow {
        var extensionReceiver: String = ""
        var parameterNames: String = ""
        var parameterTypes: String = ""
        lateinit var common: CommonDeclarationStatus
        val platform: MutableList<PlatformDeclarationStatus> = ArrayList(dimension)

        override fun toList(): List<String> = mutableListOf<String>().apply {
            this += fqName
            this += extensionReceiver
            this += parameterNames
            this += parameterTypes
            this += declarationType.alias
            this += common.alias.toString()
            platform.forEach { this += it.alias.toString() }
        }
    }

    enum class CommonDeclarationStatus(val alias: Char) {
        LIFTED_UP('L'),
        EXPECT('E'),
        ABSENT('-')
    }

    enum class PlatformDeclarationStatus(val alias: Char) {
        ACTUAL('A'),
        ORIGINAL('O'),
        ABSENT('-')
    }
}
