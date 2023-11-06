/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.formver.FormalVerificationConfigurationKeys.CONVERSION_TARGETS_SELECTION
import org.jetbrains.kotlin.formver.FormalVerificationConfigurationKeys.ERROR_STYLE
import org.jetbrains.kotlin.formver.FormalVerificationConfigurationKeys.LOG_LEVEL
import org.jetbrains.kotlin.formver.FormalVerificationConfigurationKeys.UNSUPPORTED_FEATURE_BEHAVIOUR
import org.jetbrains.kotlin.formver.FormalVerificationConfigurationKeys.VERIFICATION_TARGETS_SELECTION
import org.jetbrains.kotlin.formver.FormalVerificationPluginNames.CONVERSION_TARGETS_SELECTION_OPTION_NAME
import org.jetbrains.kotlin.formver.FormalVerificationPluginNames.ERROR_STYLE_NAME
import org.jetbrains.kotlin.formver.FormalVerificationPluginNames.LOG_LEVEL_OPTION_NAME
import org.jetbrains.kotlin.formver.FormalVerificationPluginNames.UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION_NAME
import org.jetbrains.kotlin.formver.FormalVerificationPluginNames.VERIFICATION_TARGETS_SELECTION_OPTION_NAME
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

object FormalVerificationConfigurationKeys {
    val LOG_LEVEL: CompilerConfigurationKey<LogLevel> = CompilerConfigurationKey.create("viper log level")
    val ERROR_STYLE: CompilerConfigurationKey<ErrorStyle> = CompilerConfigurationKey.create("error style")
    val UNSUPPORTED_FEATURE_BEHAVIOUR: CompilerConfigurationKey<UnsupportedFeatureBehaviour> =
        CompilerConfigurationKey.create("unsupported feature behaviour")
    val CONVERSION_TARGETS_SELECTION: CompilerConfigurationKey<TargetsSelection> =
        CompilerConfigurationKey.create("conversion targets selection")
    val VERIFICATION_TARGETS_SELECTION: CompilerConfigurationKey<TargetsSelection> =
        CompilerConfigurationKey.create("verification targets selection")
}

class FormalVerificationCommandLineProcessor : CommandLineProcessor {
    companion object {
        val LOG_LEVEL_OPTION = CliOption(
            LOG_LEVEL_OPTION_NAME, "<log_level>", "Viper log level",
            required = false, allowMultipleOccurrences = false
        )
        val ERROR_STYLE_OPTION = CliOption(
            ERROR_STYLE_NAME, "<error_style>", "Style of error messages",
            required = false, allowMultipleOccurrences = false
        )
        val UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION = CliOption(
            UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION_NAME,
            "<unsupported_feature_behaviour>",
            "Selected behaviour when encountering unsupported Kotlin features",
            required = false,
            allowMultipleOccurrences = false
        )
        val CONVERSION_TARGETS_SELECTION_OPTION = CliOption(
            CONVERSION_TARGETS_SELECTION_OPTION_NAME,
            "<conversion_targets_selection>",
            "Choice of targets to convert to Viper",
            required = false,
            allowMultipleOccurrences = false
        )
        val VERIFICATION_TARGETS_SELECTION_OPTION = CliOption(
            VERIFICATION_TARGETS_SELECTION_OPTION_NAME,
            "<verification_targets_selection>",
            "Choice of targets to verify",
            required = false,
            allowMultipleOccurrences = false
        )
    }

    override val pluginId: String = FormalVerificationPluginNames.PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        LOG_LEVEL_OPTION,
        ERROR_STYLE_OPTION,
        UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION,
        CONVERSION_TARGETS_SELECTION_OPTION,
        VERIFICATION_TARGETS_SELECTION_OPTION
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) =
        try {
            when (option) {
                LOG_LEVEL_OPTION -> configuration.put(LOG_LEVEL, LogLevel.valueOf(value.toUpperCaseAsciiOnly()))
                ERROR_STYLE_OPTION ->
                    configuration.put(ERROR_STYLE, ErrorStyle.valueOf(value.toUpperCaseAsciiOnly()))
                UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION ->
                    configuration.put(UNSUPPORTED_FEATURE_BEHAVIOUR, UnsupportedFeatureBehaviour.valueOf(value.toUpperCaseAsciiOnly()))
                CONVERSION_TARGETS_SELECTION_OPTION ->
                    configuration.put(CONVERSION_TARGETS_SELECTION, TargetsSelection.valueOf(value.toUpperCaseAsciiOnly()))
                VERIFICATION_TARGETS_SELECTION_OPTION ->
                    configuration.put(VERIFICATION_TARGETS_SELECTION, TargetsSelection.valueOf(value.toUpperCaseAsciiOnly()))
                else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
            }
        } catch (e: IllegalArgumentException) {
            throw CliOptionProcessingException("Value $value is not a valid argument for option ${option.optionName}.", e)
        }
}