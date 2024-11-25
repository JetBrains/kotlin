/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlinx.dataframe.plugin.FirDataFrameExtensionRegistrar
import org.jetbrains.kotlinx.dataframe.plugin.PATH
import org.jetbrains.kotlinx.dataframe.plugin.extensions.IrBodyFiller
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class ExperimentalExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.put(PATH, System.getenv("TEST_RESOURCES"))
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        val dumpSchemas = testServices.moduleStructure.allDirectives.contains(Directives.DUMP_SCHEMAS)
        FirExtensionRegistrarAdapter.registerExtension(
            FirDataFrameExtensionRegistrar(
                configuration.get(PATH)!!,
                null,
                isTest = true,
                dumpSchemas
            )
        )
        IrGenerationExtension.registerExtension(IrBodyFiller(configuration.get(PATH), null))
    }
}

internal object Directives : SimpleDirectivesContainer() {
    val DUMP_SCHEMAS by directive(
        description = "Whether checkers should report schemas as info warnings"
    )
}
