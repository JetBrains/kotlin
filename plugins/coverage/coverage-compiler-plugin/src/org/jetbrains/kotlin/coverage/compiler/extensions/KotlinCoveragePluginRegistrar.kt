/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.compiler.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

private const val PLUGIN_ID = "org.jetbrains.kotlin.coverage"

private val MODULE_PATH: CompilerConfigurationKey<String> =
    CompilerConfigurationKey.create("Path to the root of the current module.")

private val METADATA_PATH: CompilerConfigurationKey<String> =
    CompilerConfigurationKey.create("Path to the kotlin coverage metadata file.")

class KotlinCoveragePluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PLUGIN_ID

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val modulePath = configuration.get(MODULE_PATH)
            ?: throw CliOptionProcessingException("${KotlinCoveragePluginOptions.MODULE_PATH_OPTION.optionName} option not specified")
        val metadataFilePath = configuration.get(METADATA_PATH)
            ?: throw CliOptionProcessingException("${KotlinCoveragePluginOptions.METADATA_PATH_OPTION.optionName} option not specified")

        IrGenerationExtension.registerExtension(CoverageLoweringExtension(modulePath, metadataFilePath))
    }

    override val supportsK2: Boolean = true
}

class KotlinCoveragePluginOptions : CommandLineProcessor {
    companion object {
        val MODULE_PATH_OPTION = CliOption(
            "modulePath", "Module directory path",
            "Path to the root of the current module",
            required = true, allowMultipleOccurrences = false
        )
        val METADATA_PATH_OPTION = CliOption(
            "metadataFilePath", "Coverage metadata file path",
            "Path to the file with coverage metadata",
            required = true, allowMultipleOccurrences = false
        )
    }

    override val pluginId get() = PLUGIN_ID
    override val pluginOptions = listOf(MODULE_PATH_OPTION, METADATA_PATH_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        MODULE_PATH_OPTION -> configuration.put(MODULE_PATH, value)
        METADATA_PATH_OPTION -> configuration.put(METADATA_PATH, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}
