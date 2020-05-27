/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.utils.firstNonNull
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

/**
 * Allows printing commonization statistics to the file system.
 *
 * File format: text, "|"-separated columns.
 *
 * Header row: "FQ Name|Declaration Type|common|<platform1>|<platform2>[|<platformN>...]"
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
 * - E = successfully commonized, expect declaration generated
 * - "-" = no common declaration
 *
 * Possible values for each target platform column:
 * - A = successfully commonized, actual declaration generated
 * - O = not commonized, the declaration is as in the original library
 * - "-" = no such declaration in the original library
 *
 * Example of output:

FQ Name|Declaration Type|common|macos_x64|ios_x64
<SystemConfiguration>|MODULE|E|A|A
platform.SystemConfiguration.SCPreferencesContext|CLASS|E|A|A
platform.SystemConfiguration.SCPreferencesContext.Companion|COMPANION_OBJECT|E|A|A
platform.SystemConfiguration.SCNetworkConnectionContext|CLASS|E|A|A
platform.SystemConfiguration.SCNetworkConnectionContext.Companion|COMPANION_OBJECT|E|A|A
platform.SystemConfiguration.SCDynamicStoreRefVar|TYPE_ALIAS|-|O|O
platform.SystemConfiguration.SCVLANInterfaceRef|TYPE_ALIAS|-|O|O

 */
class NativeStatsCollector(
    private val targets: List<KonanTarget>,
    destination: File
) : StatsCollector {

    init {
        destination.mkdirs()
    }

    private val writer = destination.resolve("plain_stats.csv").printWriter()
    private var headerWritten = false

    override fun logStats(output: List<DeclarationDescriptor?>) {
        if (!headerWritten) {
            headerWritten = true
            writeHeader()
        }

        val firstNotNull = output.firstNonNull()
        val lastIsNull = output.last() == null

        val row = buildString {
            append((firstNotNull as? ModuleDescriptor)?.name ?: firstNotNull.fqNameSafe) // FQN (or name for module descriptor)
            append(SEPARATOR)
            append(firstNotNull.declarationType) // readable declaration type
            append(SEPARATOR)

            var isLiftedUp = !lastIsNull
            val platformItems = StringBuilder().apply {
                for (index in 0 until output.size - 1) {
                    append(SEPARATOR)
                    append(
                        when {
                            output[index] == null -> '-' // absent
                            lastIsNull -> 'O' // original (not commonized)
                            else -> {
                                isLiftedUp = false
                                'A' // actual (commonized)
                            }
                        }
                    )
                }
            }

            append(
                when {
                    isLiftedUp -> 'L'
                    lastIsNull -> '-'
                    else -> 'E'
                }
            ) // common

            append(platformItems)
        }

        writer.println(row)
    }

    override fun close() = writer.close()

    private fun writeHeader() {
        val row = buildString {
            append("FQ Name")
            append(SEPARATOR)
            append("Declaration Type")
            append(SEPARATOR)
            append("common")

            targets.forEach { target ->
                append(SEPARATOR)
                append(target.name)
            }
        }

        writer.println(row)
    }

    companion object {
        private const val SEPARATOR = '|'

        private inline val DeclarationDescriptor.declarationType: String
            get() = when (this) {
                is ClassDescriptor -> if (isCompanionObject) "COMPANION_OBJECT" else kind.toString()
                is TypeAliasDescriptor -> "TYPE_ALIAS"
                is ClassConstructorDescriptor -> "CLASS_CONSTRUCTOR"
                is FunctionDescriptor -> "FUN"
                is PropertyDescriptor -> "VAL"
                is ModuleDescriptor -> "MODULE"
                else -> "UNKNOWN: ${this::class.java}"
            }
    }
}
