/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.StatelessReplPluginRequestReporter
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.isSnippetDefinition
import org.jetbrains.kotlin.scripting.compiler.plugin.services.*
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.resolve.toSourceCode
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class FirScriptingCompilerExtensionRegistrar(
    private val compilerConfiguration: CompilerConfiguration
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        if (compilerConfiguration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPTING_PLUGIN_OPTION)) return

        +FirScriptDefinitionProviderService.getFactory(compilerConfiguration)
        +FirScriptConfiguratorExtensionImpl.getFactory()
        +FirScriptResolutionConfigurationExtensionImpl.getFactory()
        +Fir2IrScriptConfiguratorExtensionImpl.getFactory()

        if (compilerConfiguration.getBoolean(ScriptingConfigurationKeys.REPL_SNIPPET_STATELESS_MODE)) {
            val pluginRequestReporter = StatelessReplPluginRequestReporter(compilerConfiguration)
            val priorClassIds = compilerConfiguration.getList(ScriptingConfigurationKeys.REPL_SNIPPET_PRIOR_CLASSES)
            var capturedSession: FirSession? = null
            val historyProvider = ClasspathBackedFirReplHistoryProvider(priorClassIds) { capturedSession }
            val baseHostConfiguration = (compilerConfiguration.scriptingHostConfiguration as? ScriptingHostConfiguration)
                ?: defaultJvmScriptingHostConfiguration

            val replHostConfiguration = ScriptingHostConfiguration(baseHostConfiguration) {
                repl {
                    firReplHistoryProvider(historyProvider)
                    isReplSnippetSource { sourceFile, _ ->
                        sourceFile != null && capturedSession?.scriptDefinitionProviderService
                            ?.getBaseConfiguration(sourceFile.toSourceCode())?.valueOrNull()
                            ?.get(ScriptCompilationConfiguration.repl.isSnippetDefinition) == true
                    }
                }
            }
            +FirReplSnippetConfiguratorExtension.Factory { session ->
                capturedSession = session
                FirReplSnippetConfiguratorExtensionImpl(session, replHostConfiguration, pluginRequestReporter)
            }
            +FirExtensionSessionComponent.Factory { session ->
                capturedSession = session
                FirReplSnippetResolveExtensionImpl(session, replHostConfiguration)
            }
            +Fir2IrReplSnippetConfiguratorExtension.Factory { session ->
                capturedSession = session
                Fir2IrReplSnippetConfiguratorExtensionImpl(session, replHostConfiguration)
            }
        }
    }
}
