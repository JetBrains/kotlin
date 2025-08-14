/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameConfigurationKeys.DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES
import org.jetbrains.kotlinx.dataframe.plugin.extensions.*

class FirDataFrameExtensionRegistrar(
    val isTest: Boolean,
    val dumpSchemas: Boolean,
    val disableTopLevelExtensionsGenerator: Boolean = false,
) : FirExtensionRegistrar() {
    @OptIn(FirExtensionApiInternals::class)
    override fun ExtensionRegistrarContext.configurePlugin() {
        if (!disableTopLevelExtensionsGenerator) {
            +::TopLevelExtensionsGenerator
        }
        +::ReturnTypeBasedReceiverInjector
        +{ it: FirSession ->
            FunctionCallTransformer(it, isTest)
        }
        +::TokenGenerator
        +::DataRowSchemaSupertype
        +{ it: FirSession ->
            ExpressionAnalysisAdditionalChecker(it, isTest, dumpSchemas)
        }

        registerDiagnosticContainers(FirDataFrameErrors)
    }
}

@OptIn(ExperimentalCompilerApi::class)
class FirDataFrameComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            FirDataFrameExtensionRegistrar(
                isTest = false,
                dumpSchemas = true,
                configuration.get(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES) == true
            )
        )
        IrGenerationExtension.registerExtension(IrBodyFiller())
    }

    override val pluginId: String get() = DataFramePluginNames.PLUGIN_ID

    override val supportsK2: Boolean = true
}

object DataFrameConfigurationKeys {
    val DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Disable generation of extension properties for @DataSchema annotated classes or interfaces")
}

class DataFrameCommandLineProcessor : CommandLineProcessor {
    companion object {
        val DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION = CliOption(
            "disableTopLevelExtensionProperties",
            "true/false",
            "Disable generation of extension properties for @DataSchema annotated classes or interfaces",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId: String
        get() = DataFramePluginNames.PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        return when (option) {
            DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION -> configuration.put(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES, value == "true")
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}

object DataFramePluginNames {
    const val PLUGIN_ID = "org.jetbrains.kotlin.dataframe"
}
