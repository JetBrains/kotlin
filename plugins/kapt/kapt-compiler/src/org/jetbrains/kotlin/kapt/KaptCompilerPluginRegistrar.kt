/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt.cli.KaptCliOption.Companion.ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

class KaptCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        doOpenInternalPackagesIfRequired()

        FirAnalysisHandlerExtension.registerExtension(FirKaptAnalysisHandlerExtension())
    }

    override val pluginId: String get() = ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID

    override val supportsK2: Boolean
        get() = true
}
