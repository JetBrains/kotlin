/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importsDumper

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class ImportsDumperComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val destinationPath = configuration[ImportsDumperConfigurationKeys.DESTINATION] ?: return
        AnalysisHandlerExtension.registerExtension(ImportsDumperExtension(destinationPath))
    }

    override val supportsK2: Boolean
        get() = false
}
