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
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameConfigurationKeys.DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameConfigurationKeys.PATH
import org.jetbrains.kotlinx.dataframe.plugin.extensions.*

class FirDataFrameExtensionRegistrar(
    val isTest: Boolean,
    val dumpSchemas: Boolean,
    val disableTopLevelExtensionsGenerator: Boolean = false,
    val contextReader: ImportedSchemasData.Reader?
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

        val predicate = LookupPredicate.BuilderContext.annotated(FqName("org.jetbrains.kotlinx.dataframe.annotations.DataSchemaSource"))
        if (contextReader != null) {
            +::ImportedSchemasGenerator.bind(predicate)
            +::ImportedSchemasCompanionGenerator.bind(predicate)
            +::ImportedSchemasCheckers.bind(dumpSchemas)
            +ImportedSchemasService.getFactory(contextReader)
        }

        registerDiagnosticContainers(FirDataFrameErrors)
        registerDiagnosticContainers(ImportedSchemasDiagnostics)
        registerDiagnosticContainers(SchemaInfoDiagnostics)
    }
}

@OptIn(ExperimentalCompilerApi::class)
class FirDataFrameComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {

        val path = configuration.get(PATH)
        FirExtensionRegistrarAdapter.registerExtension(
            FirDataFrameExtensionRegistrar(
                isTest = false,
                dumpSchemas = true,
                configuration.get(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES) == true,
                contextReader = ImportedSchemasData.getReader(path)
            )
        )

        IrGenerationExtension.registerExtension(IrBodyFiller())
    }

    override val supportsK2: Boolean = true
}



object DataFrameConfigurationKeys {
    val DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Disable generation of extension properties for @DataSchema annotated classes or interfaces")

    val PATH: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("Path to the directory with schemas JSON")
}

class DataFrameCommandLineProcessor : CommandLineProcessor {
    companion object {
        val DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION = CliOption(
            "disableTopLevelExtensionProperties",
            "true/false",
            "Disable generation of extension properties for @DataSchema annotated classes or interfaces",
            required = false, allowMultipleOccurrences = false
        )

        val SCHEMAS_OPTION = CliOption(
            "schemasPath",
            "path string",
            "Path to a directory with dataframe schema JSON files. Should match output directory of the schema generator",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId: String = "org.jetbrains.kotlin.dataframe"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION, SCHEMAS_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        return when (option) {
            DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION -> configuration.put(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES, value == "true")
            SCHEMAS_OPTION -> configuration.put(PATH, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}