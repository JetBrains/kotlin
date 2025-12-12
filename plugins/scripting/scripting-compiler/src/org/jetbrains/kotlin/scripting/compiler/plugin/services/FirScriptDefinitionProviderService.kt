/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.ScriptCompilationConfigurationProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.ScriptRefinedCompilationConfigurationCache
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.scriptCompilationConfigurationProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.scriptRefinedCompilationConfigurationsCache
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.scriptCompilationComponent
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.collectAndResolveScriptAnnotationsViaFir
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.refineAllForK2
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.toKtSourceFile
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.getScriptCollectedData
import org.jetbrains.kotlin.toSourceLinesMapping
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

class FirScriptDefinitionProviderService(
    session: FirSession,
    getDefaultHostConfiguration: () -> ScriptingHostConfiguration,
) : FirExtensionSessionComponent(session) {

    @Deprecated("The host configuration based provider should be used instead")
    var definitionProvider: ScriptDefinitionProvider? = null

    @Deprecated("The host configuration based cache should be used instead")
    var configurationProvider: ScriptConfigurationsProvider? = null

    private val defaultHostConfiguration: ScriptingHostConfiguration by lazy { getDefaultHostConfiguration() }

    // host configuration owns the script definitions and (optionally) refined configurations cache
    // if not configured via scriptCompilationComponent, the properly configured default should be provided by the plugin registrar
    val hostConfiguration: ScriptingHostConfiguration
        get() = session.scriptCompilationComponent?.hostConfiguration
            ?: defaultHostConfiguration

    private val compilationConfigurationProvider: ScriptCompilationConfigurationProvider
        get() = hostConfiguration.get(ScriptingHostConfiguration.scriptCompilationConfigurationProvider)
            ?: error("ScriptCompilationConfigurationProvider is not configured in the host configuration")

    private val refinedCompilationConfigurationCache: ScriptRefinedCompilationConfigurationCache?
        get() = hostConfiguration.get(
            ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache
        )

    fun getDefaultConfiguration(): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        @Suppress("DEPRECATION")
        return definitionProvider?.getDefaultDefinition()?.compilationConfiguration?.asSuccess()
            ?: compilationConfigurationProvider.getDefaultCompilationConfiguration()
    }

    fun getBaseConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
        @Suppress("DEPRECATION")
        return definitionProvider?.let { it.findDefinition(sourceCode)?.compilationConfiguration?.asSuccess() }
            ?: compilationConfigurationProvider.findBaseCompilationConfiguration(sourceCode)
    }

    fun getRefinedConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
        @Suppress("DEPRECATION")
        return if (configurationProvider != null)
            configurationProvider!!.getScriptCompilationConfiguration(sourceCode)?.onSuccess {
                it.configuration?.asSuccess() ?: return null
            }
        else {
            val hostBasedCache = refinedCompilationConfigurationCache
            // if the cache is not configured, performing refinement on every request
            hostBasedCache?.getRefinedCompilationConfiguration(sourceCode) ?: run {
                getBaseConfiguration(sourceCode)?.onSuccess {
                    (it.refineAllForK2(
                        sourceCode,
                        hostConfiguration
                    ) { source, configuration, hostConfiguration ->
                        if (source is KtFileScriptSource) {
                            getScriptCollectedData(
                                source.ktFile,
                                configuration,
                                hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
                            ).asSuccess()
                        } else {
                            collectAndResolveScriptAnnotationsViaFir(
                                sourceCode, it, hostConfiguration,
                                getSessionForAnnotationResolution =
                                    { source, configuration ->
                                        session.scriptCompilationComponent?.getSessionForAnnotationResolution(source, configuration)
                                            ?: session
                                    },
                                convertToFir = { session, diagnosticsReporter ->
                                    val sourcesToPathsMapper = session.sourcesToPathsMapper
                                    val builder = LightTree2Fir(session, session.kotlinScopeProvider, diagnosticsReporter)
                                    val linesMapping = this.text.toSourceLinesMapping()
                                    builder.buildFirFile(text, toKtSourceFile(), linesMapping).also { firFile ->
                                        (session.firProvider as FirProviderImpl).recordFile(firFile)
                                        sourcesToPathsMapper.registerFileSource(firFile.source!!, locationId ?: name!!)
                                    }
                                }
                            )
                        }
                    }).also { refined ->
                        hostBasedCache?.storeRefinedCompilationConfiguration(sourceCode, refined)
                    }
                }
            }
        }
    }

    fun storeRefinedConfiguration(
        sourceCode: SourceCode,
        configuration: ResultWithDiagnostics<ScriptCompilationConfiguration>
    ): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
        refinedCompilationConfigurationCache?.storeRefinedCompilationConfiguration(sourceCode, configuration)

    fun clearRefinedConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
        refinedCompilationConfigurationCache?.clearRefinedCompilationConfiguration(sourceCode)

    companion object {
        fun getFactory(
            getDefaultHostConfiguration: () -> ScriptingHostConfiguration
        ): Factory {
            return Factory { session -> FirScriptDefinitionProviderService(session, getDefaultHostConfiguration) }
        }
    }
}

val FirSession.scriptDefinitionProviderService: FirScriptDefinitionProviderService? by FirSession.nullableSessionComponentAccessor()
