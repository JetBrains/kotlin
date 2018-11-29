/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

        val OUTPUT_DIR_OPTION: CliOption =
            CliOption("outputDir", "<path>", "Output path for the generated files", required = true)
    }

    override val pluginId: String
        get() = COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption>
        get() = listOf(OUTPUT_DIR_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OUTPUT_DIR_OPTION -> configuration.put(JvmAbiConfigurationKeys.OUTPUT_DIR, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}