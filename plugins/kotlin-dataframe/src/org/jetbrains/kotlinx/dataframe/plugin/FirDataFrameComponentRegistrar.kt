/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlinx.dataframe.plugin.extensions.DataRowSchemaSupertype
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ExpressionAnalysisAdditionalChecker
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FunctionCallTransformer
import org.jetbrains.kotlinx.dataframe.plugin.extensions.IrBodyFiller
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ReturnTypeBasedReceiverInjector
import org.jetbrains.kotlinx.dataframe.plugin.extensions.TokenGenerator
import org.jetbrains.kotlinx.dataframe.plugin.extensions.TopLevelExtensionsGenerator

class FirDataFrameExtensionRegistrar(
    val isTest: Boolean,
    val dumpSchemas: Boolean,
) : FirExtensionRegistrar() {
    @OptIn(FirExtensionApiInternals::class)
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::TopLevelExtensionsGenerator
        +::ReturnTypeBasedReceiverInjector
        +{ it: FirSession ->
            FunctionCallTransformer(it, isTest)
        }
        +::TokenGenerator
        +::DataRowSchemaSupertype
        +{ it: FirSession ->
            ExpressionAnalysisAdditionalChecker(it, isTest, dumpSchemas)
        }
    }
}

@OptIn(ExperimentalCompilerApi::class)
class FirDataFrameComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            FirDataFrameExtensionRegistrar(isTest = false, dumpSchemas = true)
        )
        IrGenerationExtension.registerExtension(IrBodyFiller())
    }

    override val supportsK2: Boolean = true
}
