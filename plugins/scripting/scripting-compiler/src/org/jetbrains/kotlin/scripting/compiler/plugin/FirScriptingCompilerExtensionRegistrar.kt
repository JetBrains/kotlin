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
import org.jetbrains.kotlin.fir.extensions.FirReplSnippetResolveExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.services.ClasspathBackedFirReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.Fir2IrReplSnippetConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.Fir2IrScriptConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplSnippetConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplSnippetResolveExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptDefinitionProviderService
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptResolutionConfigurationExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtScript
import kotlin.script.experimental.api.repl
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

        // Regular-pipeline REPL-snippet compilation (see ScriptingConfigurationKeys.REPL_SNIPPET_REGULAR_MODE):
        // a source marked as a REPL snippet has its previous snippets, given as classpath-reachable
        // ClassIds via REPL_SNIPPET_PRIOR_CLASSES, resolved by a ClasspathBackedFirReplHistoryProvider.
        if (compilerConfiguration.getBoolean(ScriptingConfigurationKeys.REPL_SNIPPET_REGULAR_MODE)) {
            val priorClassIds = compilerConfiguration.getList(ScriptingConfigurationKeys.REPL_SNIPPET_PRIOR_CLASSES)
            var capturedSession: FirSession? = null
            val historyProvider = ClasspathBackedFirReplHistoryProvider(priorClassIds) { capturedSession }
            val baseHostConfiguration = (compilerConfiguration.scriptingHostConfiguration as? ScriptingHostConfiguration)
                ?: defaultJvmScriptingHostConfiguration
            @OptIn(KtExperimentalApi::class)
            val replHostConfiguration = ScriptingHostConfiguration(baseHostConfiguration) {
                repl {
                    firReplHistoryProvider(historyProvider)
                    isReplSnippetSource { _, scriptSource -> (scriptSource.psi as? KtScript)?.isReplSnippet == true }
                }
            }
            +FirReplSnippetConfiguratorExtension.Factory { session ->
                capturedSession = session
                FirReplSnippetConfiguratorExtensionImpl(session, replHostConfiguration)
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
