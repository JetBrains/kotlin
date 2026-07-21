/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.plugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import test.helper.CompilerPluginHelper

@OptIn(ExperimentalCompilerApi::class)
class TestCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    override val pluginId: String = "test.native-transitive-classpath"

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        CompilerPluginHelper.verifyAvailable()
    }
}
