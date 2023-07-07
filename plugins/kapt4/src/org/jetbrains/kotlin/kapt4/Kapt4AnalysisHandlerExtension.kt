/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt3.KAPT_OPTIONS


class Kapt4AnalysisHandlerExtension : FirAnalysisHandlerExtension() {
    override fun isApplicable(configuration: CompilerConfiguration): Boolean =
        configuration.getBoolean(CommonConfigurationKeys.USE_FIR) && configuration[KAPT_OPTIONS] != null

    override fun doAnalysis(configuration: CompilerConfiguration): Boolean {
        TODO("Not yet implemented")
    }
}

class Kapt4CompilerPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        Companion.registerExtensions(this)
    }

    override val supportsK2: Boolean
        get() = true


    companion object {
        fun registerExtensions(extensionStorage: ExtensionStorage) = with(extensionStorage) {
            FirAnalysisHandlerExtension.registerExtension(Kapt4AnalysisHandlerExtension())
        }
    }
}