/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

class JvmAbiCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.jvm.abi"

        val OUTPUT_PATH_OPTION: CliOption =
            CliOption(
                "outputDir",
                "<path>",
                "Output path for generated files. This can be either a directory or a jar file.",
                true
            )

        val REMOVE_DEBUG_INFO_OPTION: CliOption =
            CliOption(
                "removeDebugInfo",
                "true/false",
                "Remove debug info from the generated class files. False by default. Note that if ABI jars are used for incremental " +
                        "compilation, it's not safe to remove debug info because debugger will behave incorrectly on non-recompiled call " +
                        "sites of inline functions.",
                false,
            )

        val REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE_OPTION: CliOption =
            CliOption(
                "removeDataClassCopyIfConstructorIsPrivate",
                "true/false",
                "Remove the 'copy' function from ABI if the primary constructor of the data class is private. False by default. " +
                        "Note that if ABI jars are used for incremental compilation, removal of the 'copy' function might break usages " +
                        "outside of the compilation unit where it's declared.",
                false,
            )

        val PRESERVE_DECLARATION_ORDER_OPTION: CliOption =
            CliOption(
                "preserveDeclarationOrder",
                "true/false",
                "Do not sort class members in output abi.jar. Legacy flag that allows tools that rely on methods/fields order in the source" +
                        " to keep working. Flipping this to true reduces stability of the ABI.",
                false,
            )

        val REMOVE_PRIVATE_CLASSES_OPTION: CliOption =
            CliOption(
                "removePrivateClasses",
                "true/false",
                "Remove private classes from ABI. False by default due to backwards compatibility. If enabled, " +
                        "private top-level classes will no longer be available from Java classes in the same package.",
                false,
            )
    }

    override val pluginId: String
        get() = COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption>
        get() = listOf(
            OUTPUT_PATH_OPTION,
            REMOVE_DEBUG_INFO_OPTION,
            REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE_OPTION,
            PRESERVE_DECLARATION_ORDER_OPTION,
            REMOVE_PRIVATE_CLASSES_OPTION,
        )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OUTPUT_PATH_OPTION -> configuration.put(JvmAbiConfigurationKeys.OUTPUT_PATH, value)
            REMOVE_DEBUG_INFO_OPTION -> configuration.put(JvmAbiConfigurationKeys.REMOVE_DEBUG_INFO, value == "true")
            REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE_OPTION -> configuration.put(
                JvmAbiConfigurationKeys.REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE,
                value == "true"
            )
            PRESERVE_DECLARATION_ORDER_OPTION -> configuration.put(JvmAbiConfigurationKeys.PRESERVE_DECLARATION_ORDER, value == "true")
            REMOVE_PRIVATE_CLASSES_OPTION -> configuration.put(
                JvmAbiConfigurationKeys.REMOVE_PRIVATE_CLASSES,
                value == "true"
            )
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}
