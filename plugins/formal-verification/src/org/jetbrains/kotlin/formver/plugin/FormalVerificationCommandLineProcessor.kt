/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.formver.plugin.FormalVerificationConfigurationKeys.LOG_LEVEL
import org.jetbrains.kotlin.formver.plugin.FormalVerificationPluginNames.LOG_LEVEL_OPTION_NAME

object FormalVerificationConfigurationKeys {
    val LOG_LEVEL: CompilerConfigurationKey<LogLevel> = CompilerConfigurationKey.create("viper log level")
}

class FormalVerificationCommandLineProcessor : CommandLineProcessor {
    companion object {
        val LOG_LEVEL_OPTION = CliOption(
            LOG_LEVEL_OPTION_NAME, "<log_level>", "Viper log level",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId: String = FormalVerificationPluginNames.PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(LOG_LEVEL_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) =
        when (option) {
            LOG_LEVEL_OPTION -> when (value) {
                "only_warnings" -> configuration.put(LOG_LEVEL, LogLevel.ONLY_WARNINGS)
                "short_viper_dump" -> configuration.put(LOG_LEVEL, LogLevel.SHORT_VIPER_DUMP)
                "full_viper_dump" -> configuration.put(LOG_LEVEL, LogLevel.FULL_VIPER_DUMP)
                else -> throw CliOptionProcessingException("Invalid setting $value for ${option.optionName}")
            }
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
}