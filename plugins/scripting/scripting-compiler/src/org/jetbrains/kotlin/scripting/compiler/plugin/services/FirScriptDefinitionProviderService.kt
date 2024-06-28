/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider

class FirScriptDefinitionProviderService(
    session: FirSession,
    private val makeDefaultDefinitionProvider: () -> ScriptDefinitionProvider,
    private val makeDefaultConfigurationProvider: () -> ScriptDependenciesProvider
) : FirExtensionSessionComponent(session) {

    // TODO: get rid of project-based implementation, write and use own singleton in K2

    private var _definitionProvider: ScriptDefinitionProvider? = null
    var definitionProvider: ScriptDefinitionProvider?
        get() = synchronized(this) {
            if (_definitionProvider == null) _definitionProvider = makeDefaultDefinitionProvider()
            _definitionProvider
        }
        set(value) { synchronized(this) { _definitionProvider = value} }

    private var _configurationProvider: ScriptDependenciesProvider? = null
    var configurationProvider: ScriptDependenciesProvider?
        get() = synchronized(this) {
            if (_configurationProvider == null) _configurationProvider = makeDefaultConfigurationProvider()
            _configurationProvider
        }
        set(value) { synchronized(this) { _configurationProvider = value} }

    companion object {
        fun getFactory(
            definitions: List<ScriptDefinition>,
            definitionSources: List<ScriptDefinitionsSource>,
            definitionProvider: ScriptDefinitionProvider? = null,
            configurationProvider: ScriptDependenciesProvider? = null
        ): Factory {
            val makeDefinitionsProvider = definitionProvider?.let { { it } }
                ?: {
                    CliScriptDefinitionProvider().also {
                        it.setScriptDefinitionsSources(definitionSources)
                        it.setScriptDefinitions(definitions)
                    }
                }

            val makeConfigurationProvider = configurationProvider?.let { { it } }
                ?: {
                    // TODO: check if memory can leak in MockProject (probably not too important, since currently the providers are set externaly in important cases)
                    CliScriptDependenciesProvider(
                        MockProject(
                            null,
                            Disposer.newDisposable(
                                "Disposable for project of ${CliScriptDependenciesProvider::class.simpleName} created by" +
                                        " ${FirScriptDefinitionProviderService::class.simpleName}"
                            ),
                        ),
                    )
                }

            return Factory { session -> FirScriptDefinitionProviderService(session, makeDefinitionsProvider, makeConfigurationProvider) }
        }
    }
}

val FirSession.scriptDefinitionProviderService: FirScriptDefinitionProviderService? by FirSession.nullableSessionComponentAccessor()
