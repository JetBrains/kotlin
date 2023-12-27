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

        val REMOVE_COPY_ALONG_WITH_CONSTRUCTOR_OPTION: CliOption =
            CliOption(
                "removeCopyAlongWithConstructor",
                "true/false",
                "Remove copy method from ABI if data class' constructor is also being removed. False by default. " +
                        "Note that if copy constructor is being used outside of the compilation unit - it won't be available.",
                false,
            )
    }

    override val pluginId: String
        get() = COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption>
        get() = listOf(OUTPUT_PATH_OPTION, REMOVE_DEBUG_INFO_OPTION, REMOVE_COPY_ALONG_WITH_CONSTRUCTOR_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OUTPUT_PATH_OPTION -> configuration.put(JvmAbiConfigurationKeys.OUTPUT_PATH, value)
            REMOVE_DEBUG_INFO_OPTION -> configuration.put(JvmAbiConfigurationKeys.REMOVE_DEBUG_INFO, value == "true")
            REMOVE_COPY_ALONG_WITH_CONSTRUCTOR_OPTION -> configuration.put(
                JvmAbiConfigurationKeys.REMOVE_COPY_ALONG_WITH_CONSTRUCTOR,
                value == "true"
            )
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}
