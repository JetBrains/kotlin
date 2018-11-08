/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.contracts.contextual.ContractsCliOptions.CONTRACTS_JAR

object ContractsCliOptions {
    val CONTRACTS_JAR = CliOption(
        optionName = "contract",
        valueDescription = "<path>",
        description = "Specify path to .jar file with specific contracts",
        allowMultipleOccurrences = true
    )
}

object ContractsConfigurationKeys {
    val PATHS_TO_JARS = CompilerConfigurationKey.create<List<String>>("Paths to jar's with contracts")
}

class ContractsCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val PLUGIN_ID = "org.jetbrains.kotlin.contracts"
    }

    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(ContractsCliOptions.CONTRACTS_JAR)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            CONTRACTS_JAR -> configuration.add(ContractsConfigurationKeys.PATHS_TO_JARS, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}