/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class KaptLiteCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String
        get() = "org.jetbrains.kotlin.kaptlite"

    override val pluginOptions: Collection<AbstractCliOption>
        get() = KaptLiteCliOption.values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option !is KaptLiteCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        val options = configuration[KAPT_LITE_OPTIONS]
            ?: KaptLiteOptions.Builder().also { configuration.put(KAPT_LITE_OPTIONS, it) }

        when (option) {
            KaptLiteCliOption.STUBS_OUTPUT_DIR -> {
                options.stubsOutputDir = File(value)
            }
        }
    }
}

class KaptLiteComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val options = configuration[KAPT_LITE_OPTIONS]?.build() ?: return

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        AnalysisHandlerExtension.registerExtension(project, KaptLiteAnalysisHandlerExtension(configuration, options, messageCollector))
    }
}