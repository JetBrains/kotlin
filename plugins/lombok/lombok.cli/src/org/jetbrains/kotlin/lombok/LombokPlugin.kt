/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.lombok.LombokConfigurationKeys.CONFIG_FILE
import org.jetbrains.kotlin.lombok.LombokPluginNames.CONFIG_OPTION_NAME
import org.jetbrains.kotlin.lombok.LombokPluginNames.PLUGIN_ID
import org.jetbrains.kotlin.lombok.k2.FirLombokRegistrar
import org.jetbrains.kotlin.resolve.jvm.extensions.SyntheticJavaResolveExtension
import java.io.File

class LombokComponentRegistrar : CompilerPluginRegistrar() {
    companion object {
        fun registerComponents(extensionStorage: ExtensionStorage, compilerConfiguration: CompilerConfiguration) = with(extensionStorage) {
            val configFile = compilerConfiguration[CONFIG_FILE]
            val config = LombokPluginConfig(configFile)
            SyntheticJavaResolveExtension.registerExtension(LombokResolveExtension(config))
            FirExtensionRegistrarAdapter.registerExtension(FirLombokRegistrar(configFile))
        }
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        configuration.messageCollector
            .report(
                CompilerMessageSeverity.WARNING,
                "Lombok Kotlin compiler plugin is an experimental feature." +
                        " See: https://kotlinlang.org/docs/components-stability.html."
            )
        registerComponents(this, configuration)
    }

    override val supportsK2: Boolean
        get() = true
}

object LombokConfigurationKeys {
    val CONFIG_FILE: CompilerConfigurationKey<File> = CompilerConfigurationKey.create("lombok config file location")
}

class LombokCommandLineProcessor : CommandLineProcessor {

    companion object {
        val CONFIG_FILE_OPTION = CliOption(
            optionName = CONFIG_OPTION_NAME,
            valueDescription = "<path>",
            description = "Lombok configuration file location",
            required = false
        )
    }

    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(CONFIG_FILE_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            CONFIG_FILE_OPTION -> {
                val file = File(value)
                if (!file.exists()) {
                    throw IllegalArgumentException("Config file not found ${file.absolutePath}")
                }
                configuration.put(CONFIG_FILE, file)
            }
            else -> throw IllegalArgumentException("Unknown option $option")
        }
    }
}
