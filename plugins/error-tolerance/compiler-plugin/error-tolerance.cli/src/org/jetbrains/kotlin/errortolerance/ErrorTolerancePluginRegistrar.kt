/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.errortolerance

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.errortolerance.backend.ErrorTolerantIrGenerationExtension
import org.jetbrains.kotlin.errortolerance.fir.ErrorTolerantFirExtension
import org.jetbrains.kotlin.fir.backend.FirErrorTolerantCompilationExtension

object ErrorTolerancePluginNames {
    const val PLUGIN_ID: String = "org.jetbrains.kotlin.errortolerance"
}

val ERROR_TOLERANCE_ENABLED: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("error tolerance enabled")

class ErrorToleranceCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ENABLED_OPTION: CliOption = CliOption(
            optionName = "enabled",
            valueDescription = "true|false",
            description = "Compile code with errors, replacing erroneous bodies with 'throw java.lang.Error(...)'",
            required = false,
        )
    }

    override val pluginId: String get() = ErrorTolerancePluginNames.PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(ENABLED_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ENABLED_OPTION -> configuration.put(ERROR_TOLERANCE_ENABLED, value.toBooleanStrict())
            else -> error("Unexpected config option: ${option.optionName}")
        }
    }
}

class ErrorTolerancePluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String get() = ErrorTolerancePluginNames.PLUGIN_ID

    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!configuration.get(ERROR_TOLERANCE_ENABLED, true)) return
        FirErrorTolerantCompilationExtension.registerExtension(ErrorTolerantFirExtension())
        IrGenerationExtension.registerExtension(ErrorTolerantIrGenerationExtension())
    }
}
