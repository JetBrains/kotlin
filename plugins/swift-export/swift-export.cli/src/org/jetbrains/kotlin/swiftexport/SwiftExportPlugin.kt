/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport

import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.swiftexport.SwiftExportPluginNames.OUTPUT_DIR_KEY
import org.jetbrains.kotlin.swiftexport.SwiftExportPluginNames.PLUGIN_ID
import org.jetbrains.kotlin.swiftexport.SwiftExportPluginNames.RESULT_NAME_KEY
import java.io.File

object SwiftExportConfigurationKeys {
    val OUTPUT_DIR: CompilerConfigurationKey<String> = CompilerConfigurationKey.create(
        "Destination directory for swift-export resulted files"
    )
    val RESULT_NAME: CompilerConfigurationKey<String> = CompilerConfigurationKey.create(
        "Filenames for resulted files. There will be 3 files - %RESULT_NAME%.swift, %RESULT_NAME%.h and %RESULT_NAME%.kt."
    )
}

class SwiftExportCommandLineProcessor : CommandLineProcessor {
    companion object {
        val OUTPUT_DIR = CliOption(
            OUTPUT_DIR_KEY, "output destination",
            "Destination directory for swift-export resulted files",
            required = true, allowMultipleOccurrences = false
        )
        val RESULT_NAME = CliOption(
            RESULT_NAME_KEY, "Filenames for resulted files",
            "Filenames for resulted files. By default - \"result\"",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(
        OUTPUT_DIR, RESULT_NAME
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        OUTPUT_DIR -> configuration.put(SwiftExportConfigurationKeys.OUTPUT_DIR, value)
        RESULT_NAME -> configuration.put(SwiftExportConfigurationKeys.RESULT_NAME, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class SwiftExportComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val dir = File(
            configuration.get(SwiftExportConfigurationKeys.OUTPUT_DIR)
                ?: throw IllegalArgumentException("output_dir is a required argument for org.jetbrains.kotlin.swiftexport")
        )

        val named = configuration.get(SwiftExportConfigurationKeys.RESULT_NAME)
            ?: "result"
        FirAnalysisHandlerExtension.registerExtension(
            SwiftExportExtension(dir, named)
        )
    }
}
