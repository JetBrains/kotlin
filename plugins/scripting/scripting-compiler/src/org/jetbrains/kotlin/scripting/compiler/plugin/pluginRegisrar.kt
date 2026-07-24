/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.extensions.ReplFactoryExtension
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.extensions.CollectAdditionalSourcesExtension
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor
import org.jetbrains.kotlin.extensions.ProcessSourcesBeforeCompilingExtension
import org.jetbrains.kotlin.fir.extensions.CollectAdditionalSourceFilesExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptReportSink
import org.jetbrains.kotlin.scripting.compiler.plugin.extensions.JvmStandardReplFactoryExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.extensions.ReplLoweringExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.extensions.ScriptLoweringExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.extensions.ScriptingCollectAdditionalSourcesExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.extensions.ScriptingIrExplainGenerationExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.extensions.ScriptingProcessSourcesBeforeCompilingExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.CollectAdditionalScriptSourcesExtension
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys.ENABLE_SCRIPT_EXPLANATION_OPTION
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.extensions.ScriptExtraImportsProviderExtension
import org.jetbrains.kotlin.scripting.extensions.ScriptingResolveExtension
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

private fun <T : Any> ExtensionPointDescriptor<T>.registerExtensionIfRequired(
    extensionStorage: CompilerPluginRegistrar.ExtensionStorage,
    extension: T,
) {
    with(extensionStorage) {
        try {
            registerExtension(extension)
        } catch (_: IllegalArgumentException) {
            // ignore
        }
    }
}

// Scripting infrastructure still depends on project-based components, therefore we still need a separate registrar above - ScriptingCompilerConfigurationComponentRegistrar
// TODO: refactor components and migrate the plugin to the project-independent operation
class ScriptingK2CompilerPluginRegistrar : CompilerPluginRegistrar() {
    companion object {
        fun registerComponents(extensionStorage: ExtensionStorage, compilerConfiguration: CompilerConfiguration) = with(extensionStorage) {
            FirExtensionRegistrar.registerExtension(FirScriptingCompilerExtensionRegistrar(compilerConfiguration))
            FirExtensionRegistrar.registerExtension(FirScriptingSamWithReceiverExtensionRegistrar())

            with(extensionStorage) {
                if (compilerConfiguration.get(ENABLE_SCRIPT_EXPLANATION_OPTION, false)) {
                    IrGenerationExtension.registerExtension(ScriptingIrExplainGenerationExtension())
                }

                IrGenerationExtension.registerExtension(ScriptLoweringExtension())
                IrGenerationExtension.registerExtension(ReplLoweringExtension())
            }
        }
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        registerComponents(this, configuration)

        CollectAdditionalSourceFilesExtension.registerExtension(CollectAdditionalScriptSourcesExtension())
        CollectAdditionalSourcesExtension.registerExtension(ScriptingCollectAdditionalSourcesExtension())
        ScriptEvaluationExtension.registerExtension(JvmCliScriptEvaluationExtension())
        ReplFactoryExtension.registerExtensionIfRequired(this, JvmStandardReplFactoryExtension())
        SyntheticResolveExtension.registerExtension(ScriptingResolveExtension())
        ExtraImportsProviderExtension.registerExtension(ScriptExtraImportsProviderExtension())
        ProcessSourcesBeforeCompilingExtension.registerExtension(ScriptingProcessSourcesBeforeCompilingExtension())

        val scriptDefinitionProvider = CliScriptDefinitionProvider()
        ScriptDefinitionProvider.registerExtension(scriptDefinitionProvider)

        @OptIn(MessageCollectorAccess::class) // TODO(KT-84516)
        val messageCollector = configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY]
        ScriptConfigurationsProvider.registerExtension(
            CliScriptConfigurationsProvider(project = null) {
                scriptDefinitionProvider
            }.apply {
                if (messageCollector != null) {
                    reportSink = CliScriptReportSink(messageCollector)
                }
            }
        )

        val hostConfiguration = configuration.scriptingHostConfiguration as? ScriptingHostConfiguration
            ?: defaultJvmScriptingHostConfiguration
        CompilerConfigurationExtension.registerExtension(ScriptingCompilerConfigurationExtension(hostConfiguration, scriptDefinitionProvider))
    }

    override val pluginId: String get() = KOTLIN_SCRIPTING_PLUGIN_ID

    override val supportsK2: Boolean
        get() = true
}

