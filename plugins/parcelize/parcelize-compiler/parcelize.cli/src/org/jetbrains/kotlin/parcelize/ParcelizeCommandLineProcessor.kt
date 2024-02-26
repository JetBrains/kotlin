/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

class ParcelizeCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.parcelize"

        val ADDITIONAL_ANNOTATION_OPTION: CliOption =
            CliOption(
                "additionalAnnotation",
                "<fully qualified name>",
                "Additional annotation to trigger parcelize processing.",
                required = false,
                allowMultipleOccurrences = true
            )
    }

    override val pluginId: String
        get() = COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption>
        get() = listOf(ADDITIONAL_ANNOTATION_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ADDITIONAL_ANNOTATION_OPTION -> configuration.appendList(ParcelizeConfigurationKeys.ADDITIONAL_ANNOTATION, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}