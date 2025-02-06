/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class FirTestReplCompilerExtensionRegistrar(
    private val hostConfiguration: ScriptingHostConfiguration
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirTestReplSnippetConfiguratorExtensionImpl.getFactory(hostConfiguration)
        +FirTestReplSnippetResolveExtensionImpl.getFactory(hostConfiguration)
    }
}

@OptIn(ExperimentalCompilerApi::class)
class TestReplCompilerPluginRegistrar : CompilerPluginRegistrar() {
    companion object {
        fun registerComponents(extensionStorage: ExtensionStorage, compilerConfiguration: CompilerConfiguration) = with(extensionStorage) {
            val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                // TODO: add jdk path and other params if needed
            }
            FirExtensionRegistrarAdapter.registerExtension(FirTestReplCompilerExtensionRegistrar(hostConfiguration))
        }
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        registerComponents(this, configuration)
    }

    override val supportsK2: Boolean
        get() = true
}
