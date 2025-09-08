/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlinx.dataframe.plugin.FirDataFrameExtensionRegistrar
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ImportedSchemasData
import org.jetbrains.kotlinx.dataframe.plugin.extensions.IrBodyFiller

class ExperimentalExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        val schema =
            """
            {
              "name": "test",
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "data": "test.json",
              "schema": {
                "id": "kotlin.Int",
                "lastName": "kotlin.String",
                "group: ColumnGroup": {
                  "col": "kotlin.String",
                  "col1": "kotlin.String",
                  "deepGroup: ColumnGroup": {
                    "col2": "kotlin.String",
                    "col3": "kotlin.String"
                  }
                },
                "frame: FrameColumn": {
                  "col3": "kotlin.String",
                  "col4": "kotlin.String"
                }
              }
            }
            """.trimIndent()

        val testData = mapOf("Schema" to schema)
        val dumpSchemas = testServices.moduleStructure.allDirectives.contains(DataFrameDirectives.DUMP_SCHEMAS)
        FirExtensionRegistrarAdapter.registerExtension(
            FirDataFrameExtensionRegistrar(
                isTest = true,
                dumpSchemas,
                contextReader = ImportedSchemasData.getReader(testData)
            )
        )
        IrGenerationExtension.registerExtension(IrBodyFiller())
    }
}
