/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.scripting.compiler.plugin.services.Fir2IrReplSnippetConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplCompilationConfigurationProviderService
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplSnippetConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplSnippetResolveExtensionImpl
import kotlin.script.experimental.host.ScriptingHostConfiguration

class FirReplCompilerExtensionRegistrar(
    private val hostConfiguration: ScriptingHostConfiguration
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirReplSnippetConfiguratorExtensionImpl.Companion.getFactory(hostConfiguration)
        +FirReplSnippetResolveExtensionImpl.Companion.getFactory(hostConfiguration)
        +Fir2IrReplSnippetConfiguratorExtensionImpl.Companion.getFactory(hostConfiguration)
        +::FirReplCompilationConfigurationProviderService
    }
}

@OptIn(ExperimentalCompilerApi::class)
class ReplCompilerPluginRegistrar(val hostConfiguration: ScriptingHostConfiguration) : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        with(this) {
            FirExtensionRegistrarAdapter.registerExtension(FirReplCompilerExtensionRegistrar(hostConfiguration))
        }
    }

    override val supportsK2: Boolean
        get() = true
}
