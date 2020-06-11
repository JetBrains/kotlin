/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_ENTRY
import org.jetbrains.kotlin.descriptors.commonizer.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.utils.firstNonNull
import org.jetbrains.kotlin.descriptors.commonizer.utils.fqNameWithTypeParameters
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

/**
 * Allows printing commonization statistics to the file system.
 *
 * File format: text, "|"-separated columns.
 *
 * Header row: "FQ Name|Extension Receiver|Parameter Names|Parameter Types|Declaration Type|common|<platform1>|<platform2>[|<platformN>...]"
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
            appendName(firstNotNull)
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
            append("Extension Receiver")
            append(SEPARATOR)
            append("Parameter Names")
            append(SEPARATOR)
            append("Parameter Types")
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

        @Suppress("NOTHING_TO_INLINE")
        private inline fun StringBuilder.appendName(descriptor: DeclarationDescriptor) {
            // name
            append(if (descriptor is ModuleDescriptor) descriptor.name.asString() else descriptor.fqNameSafe.asString())
            append(SEPARATOR)

            // extension receiver
            if (descriptor is PropertyDescriptor || descriptor is SimpleFunctionDescriptor) {
                append((descriptor as CallableDescriptor).extensionReceiverParameter?.type?.fqNameWithTypeParameters.orEmpty())
            }
            append(SEPARATOR)

            if (descriptor is ConstructorDescriptor || descriptor is SimpleFunctionDescriptor) {
                // parameter names
                (descriptor as FunctionDescriptor).valueParameters.joinTo(this) { it.name.asString() }
                append(SEPARATOR)
                // parameter types
                descriptor.valueParameters.joinTo(this) { it.type.fqNameWithTypeParameters }
            } else {
                append(SEPARATOR)
            }
        }

        private inline val DeclarationDescriptor.topLevelnessPrefix: String
            get() = if (DescriptorUtils.isTopLevelDeclaration(this)) "TOP-LEVEL " else "NESTED "

        private inline val DeclarationDescriptor.declarationType: String
            get() = when (this) {
                is ClassDescriptor -> when {
                    isCompanionObject -> "COMPANION_OBJECT"
                    kind == ENUM_CLASS || kind == ENUM_ENTRY -> kind.toString()
                    else -> topLevelnessPrefix + kind.toString()
                }
                is TypeAliasDescriptor -> "TYPE_ALIAS"
                is ClassConstructorDescriptor -> "CLASS_CONSTRUCTOR"
                is FunctionDescriptor -> topLevelnessPrefix + "FUN"
                is PropertyDescriptor -> topLevelnessPrefix + if (isConst) "CONST-VAL" else "VAL"
                is ModuleDescriptor -> "MODULE"
                else -> "UNKNOWN: ${this::class.java}"
            }
    }
}
