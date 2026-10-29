/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.kotlinPaths
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplSnippetConfigurationCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplLoadedPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplPluginRequest
import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplPluginRequestsCodec
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptCompilerPlugin
import kotlin.script.experimental.api.compilerOptions

@OptIn(MessageCollectorAccess::class)
class StatelessReplPluginRequestReporter(private val compilerConfiguration: CompilerConfiguration) {
    private val messageCollector: MessageCollector = compilerConfiguration.messageCollector
    private val baseCompilerOptions = compilerConfiguration[ScriptingConfigurationKeys.REPL_SNIPPET_CONFIGURATION_FILE]
        ?.let(ReplSnippetConfigurationCodec::readFrom)
        ?.get(ScriptCompilationConfiguration.compilerOptions).orEmpty()
    private val loadedPlugins = compilerConfiguration.getList(ScriptingConfigurationKeys.REPL_SNIPPET_LOADED_PLUGINS)
        .mapNotNull { encoded ->
            runCatching { StatelessReplPluginRequestsCodec.decodeLoadedPluginOption(encoded) }
                .onFailure {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Invalid loaded REPL compiler plugin request: ${it.message}"
                    )
                }
                .getOrNull()
        }
        .associateBy(StatelessReplLoadedPlugin::key)
    private val globalPluginIds = compilerConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
        .mapTo(mutableSetOf()) { it.pluginId }
    private val requests = linkedMapOf<String, StatelessReplPluginRequest>()

    fun report(source: kotlin.script.experimental.api.SourceCode, refinedConfiguration: ScriptCompilationConfiguration) {
        val plugins = collectScriptCompilerPlugins(
            baseCompilerOptions,
            listOf(source),
            { refinedConfiguration },
            messageCollector,
        )

        val newRequests = mutableListOf<StatelessReplPluginRequest>()
        for (plugin in plugins) {
            val key = plugin.requestKey
            val loadedOptions = loadedPlugins[key]
            if (loadedOptions != null) {
                if (loadedOptions.pluginId !in globalPluginIds) {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Stateless REPL compiler plugin '${loadedOptions.pluginId}' was marked loaded but is not registered."
                    )
                } else if (loadedOptions.options != plugin.options) {
                    reportConflict(plugin.descriptionForDiagnostic())
                }
                continue
            }

            val previous = requests[key]
            if (previous != null) {
                if (previous.options != plugin.options) reportConflict(plugin.descriptionForDiagnostic())
                continue
            }

            val classpath = validateAndResolveScriptCompilerPlugin(
                plugin,
                compilerConfiguration.kotlinPaths,
                globalPluginIds,
                messageCollector,
            ) ?: continue
            if (classpath.isEmpty()) continue

            val registrarCount = compilerConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS).size
            val disposable = Disposer.newDisposable("stateless REPL compiler plugin validation")
            val pluginId = try {
                val validatedConfiguration = compilerConfiguration.copy()
                    .withScriptCompilerPlugins(listOf(plugin), disposable, messageCollector)
                    ?: continue
                validatedConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
                    .drop(registrarCount)
                    .singleOrNull()
                    ?.pluginId
                    ?: continue
            } finally {
                Disposer.dispose(disposable)
            }

            val request = StatelessReplPluginRequest(key, pluginId, classpath, plugin.options)
            requests[key] = request
            newRequests += request
        }

        // The host extracts the requests from these diagnostics, so that it can retry the compilation with the plugins loaded.
        newRequests.forEach { request ->
            messageCollector.report(CompilerMessageSeverity.ERROR, StatelessReplPluginRequestsCodec.missingPluginMessage(request))
        }
    }

    private fun reportConflict(description: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, "Conflicting options for compiler plugin '$description'.")
    }

    private fun ScriptCompilerPlugin.descriptionForDiagnostic(): String = id ?: classpath.joinToString(",")
}
