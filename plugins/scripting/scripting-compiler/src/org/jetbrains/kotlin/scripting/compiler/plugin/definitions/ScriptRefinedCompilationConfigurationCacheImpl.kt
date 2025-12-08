/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with

class ScriptRefinedCompilationConfigurationCacheImpl : ScriptRefinedCompilationConfigurationCache {
    private val refinedCache = mutableMapOf<String, ResultWithDiagnostics<ScriptCompilationConfiguration>>()

    override fun getRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
        sourceCode.locationId?.let { refinedCache[it] }

    override fun storeRefinedCompilationConfiguration(
        sourceCode: SourceCode,
        configuration: ResultWithDiagnostics<ScriptCompilationConfiguration>
    ): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
        val locationId = sourceCode.locationId ?: error("Cannot cache script without location: ${sourceCode.name ?: sourceCode.text}")
        return refinedCache.put(locationId, configuration)
    }

    override fun clearRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
        val locationId = sourceCode.locationId ?: return null
        return refinedCache.remove(locationId)
    }
}

fun ScriptingHostConfiguration.withRefinedCompilationConfigurationCache(
    implementation : ScriptRefinedCompilationConfigurationCache = ScriptRefinedCompilationConfigurationCacheImpl()
): ScriptingHostConfiguration = with {
    ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache(implementation)
}
