/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameConfigurationKeys.DATAFRAME_DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameConfigurationKeys.DATAFRAME_PATH
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameConfigurationKeys.DATAFRAME_POLYMORPHIC_DATA_SCHEMAS
import org.jetbrains.kotlinx.dataframe.plugin.extensions.*

class FirDataFrameExtensionRegistrar(
    val isTest: Boolean,
    val dumpSchemas: Boolean,
    val disableTopLevelExtensionsGenerator: Boolean = false,
    val contextReader: ImportedSchemasData.Reader?,
    val polymorphicDataSchemas: Boolean = false,
) : FirExtensionRegistrar() {
    @OptIn(FirExtensionApiInternals::class)
    override fun ExtensionRegistrarContext.configurePlugin() {
        if (!disableTopLevelExtensionsGenerator) {
            +::TopLevelExtensionsGenerator
        }
        +::ReturnTypeBasedReceiverInjector
        +{ it: FirSession ->
            FunctionCallTransformer(it, isTest, polymorphicDataSchemas)
        }
        +::TokenContentGenerator
        +::DataRowSchemaSupertype
        if (polymorphicDataSchemas) {
            +::PolymorphicDataSchemasService
        }
        +{ it: FirSession ->
            ExpressionAnalysisAdditionalChecker(it, isTest, dumpSchemas)
        }
        if (dumpSchemas) {
            val withImportedSchemasReader = contextReader != null
            +::DataSchemaInfoCheckers.bind(withImportedSchemasReader)
        }

        val predicate = LookupPredicate.BuilderContext.annotated(FqName("org.jetbrains.kotlinx.dataframe.annotations.DataSchemaSource"))
        if (contextReader != null) {
            +::ImportedSchemasGenerator.bind(predicate)
            +::ImportedSchemasCompanionGenerator.bind(predicate)
            +::ImportedSchemasCheckers
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

        val path = configuration[DATAFRAME_PATH]
        FirExtensionRegistrar.registerExtension(
            FirDataFrameExtensionRegistrar(
                isTest = false,
                dumpSchemas = true,
                configuration[DATAFRAME_DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES] == true,
                contextReader = ImportedSchemasData.getReader(path),
                polymorphicDataSchemas = configuration[DATAFRAME_POLYMORPHIC_DATA_SCHEMAS] == true,
            )
        )

        IrGenerationExtension.registerExtension(IrBodyFiller())
    }

    override val pluginId: String get() = DataFramePluginNames.PLUGIN_ID

    override val supportsK2: Boolean = true
}

object DataFrameConfigurationKeys {
    // Disable generation of extension properties for @DataSchema annotated classes or interfaces.
    val DATAFRAME_DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("DATAFRAME_DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES")

    // Path to the directory with schemas JSON.
    val DATAFRAME_PATH: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("DATAFRAME_PATH")

    // Add compatible @DataSchema interfaces of the module as supertypes of the generated schema markers.
    val DATAFRAME_POLYMORPHIC_DATA_SCHEMAS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("DATAFRAME_POLYMORPHIC_DATA_SCHEMAS")
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

        val POLYMORPHIC_DATA_SCHEMAS_OPTION = CliOption(
            "polymorphicDataSchemas",
            "true/false",
            "Experimental: add every compatible @DataSchema interface of the module as a supertype of the generated schema markers",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId: String
        get() = DataFramePluginNames.PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> =
        listOf(DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION, SCHEMAS_OPTION, POLYMORPHIC_DATA_SCHEMAS_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        return when (option) {
            DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES_OPTION -> configuration.put(
                DATAFRAME_DISABLE_TOP_LEVEL_EXTENSION_PROPERTIES,
                value == "true"
            )
            SCHEMAS_OPTION -> configuration.put(DATAFRAME_PATH, value)
            POLYMORPHIC_DATA_SCHEMAS_OPTION -> configuration.put(DATAFRAME_POLYMORPHIC_DATA_SCHEMAS, value == "true")
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}

object DataFramePluginNames {
    const val PLUGIN_ID = "org.jetbrains.kotlin.dataframe"
}
