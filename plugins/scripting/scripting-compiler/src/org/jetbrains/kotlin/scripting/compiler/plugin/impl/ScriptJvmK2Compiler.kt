/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import kotlin.script.experimental.api.*

class ScriptJvmK2Compiler(
    val state: K2ScriptingCompilerEnvironment,
    private val convertToFir: SourceCode.(FirSession) -> FirFile
) : ScriptCompilerProxy {

    fun compile(script: SourceCode) = compile(script, state.baseScriptCompilationConfiguration)

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<CompiledScript> {

        return scriptCompilationConfiguration.refineAll(script, convertToFir).onSuccess { refinedConfiguration ->
            // imports -> compile one by one or all here?
            // add all deps
            // create source session (with dependent one if 1.1
            // convert to fir
            // analyze ...
//            refinedConfiguration.asSuccess()
            TODO()
        }
    }
}

private fun ScriptCompilationConfiguration.refineAll(
    script: SourceCode,
    convertToFir: SourceCode.(FirSession) -> FirFile
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    refineBeforeParsing(script).onSuccess {
        val collectedData by lazy(LazyThreadSafetyMode.NONE) {
            ScriptCollectedData(
                mapOf(
                    ScriptCollectedData.fir to listOf(script.convertToFir(createDummySessionForScriptRefinement(script)))
                )
            )
        }
        it.refineOnFir(script, collectedData)
    }.onSuccess {
        it.refineBeforeCompiling(script)
    }

@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
private fun createDummySessionForScriptRefinement(script: SourceCode): FirSession =
    object : FirSession(Kind.Source) {}.apply {
        val moduleData = FirSourceModuleData(
            Name.identifier("<${script.name}stub module for script refinement>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
        register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(
                LanguageVersionSettingsImpl.DEFAULT,
                isMetadataCompilation = false
            ))
        register(FirExtensionService::class, FirExtensionService(this))
    }

