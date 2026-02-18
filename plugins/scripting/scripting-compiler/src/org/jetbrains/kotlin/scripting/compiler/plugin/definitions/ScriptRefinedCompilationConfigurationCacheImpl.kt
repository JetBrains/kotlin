/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.toSystemIndependentScriptPath
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.valueOrNull

class ScriptRefinedCompilationConfigurationCacheImpl : ScriptRefinedCompilationConfigurationCache {
    private val refinedCache = mutableMapOf<String, ResultWithDiagnostics<ScriptCompilationConfiguration>>()

    override fun getRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
        sourceCode.locationId?.let { refinedCache[it.toSystemIndependentScriptPath()] }

    override fun storeRefinedCompilationConfiguration(
        sourceCode: SourceCode,
        configuration: ResultWithDiagnostics<ScriptCompilationConfiguration>
    ): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
        val locationId = sourceCode.locationId ?: error("Cannot cache script without location: ${sourceCode.name ?: sourceCode.text}")
        return refinedCache.put(locationId.toSystemIndependentScriptPath(), configuration)
    }

    override fun clearRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
        val locationId = sourceCode.locationId ?: return null
        return refinedCache.remove(locationId.toSystemIndependentScriptPath())
    }
}

class ScriptRefinedCompilationConfigurationCacheOverConfigurationsProvider(
    private val legacyConfigurationsProvider: ScriptConfigurationsProvider,
    private val definitionsProvider: ScriptCompilationConfigurationProvider?
) : ScriptRefinedCompilationConfigurationCache {

    override fun getRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
        definitionsProvider?.findBaseCompilationConfiguration(sourceCode).let { providedConfiguration ->
            legacyConfigurationsProvider.getScriptCompilationConfiguration(sourceCode, providedConfiguration?.valueOrNull())
                ?.onSuccess { it.configuration?.asSuccess() ?: return@getRefinedCompilationConfiguration null }
        }

    override fun storeRefinedCompilationConfiguration(
        sourceCode: SourceCode,
        configuration: ResultWithDiagnostics<ScriptCompilationConfiguration>,
    ): ResultWithDiagnostics<ScriptCompilationConfiguration>? = null

    override fun clearRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? = null
}
