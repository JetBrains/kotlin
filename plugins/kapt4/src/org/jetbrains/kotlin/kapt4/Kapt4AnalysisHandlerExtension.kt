/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired

class Kapt4CompilerPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!configuration.getBoolean(USE_FIR)) return

        doOpenInternalPackagesIfRequired()

        FirAnalysisHandlerExtension.registerExtension(FirKaptAnalysisHandlerExtension())
    }

    override val supportsK2: Boolean
        get() = true
}
