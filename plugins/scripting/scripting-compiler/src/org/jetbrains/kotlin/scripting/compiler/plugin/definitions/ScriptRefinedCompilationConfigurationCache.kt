/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

val ScriptingHostConfigurationKeys.scriptRefinedCompilationConfigurationsCache by PropertiesCollection.key<ScriptRefinedCompilationConfigurationCache>(isTransient = true)


interface ScriptRefinedCompilationConfigurationCache {
    fun getRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>?

    fun storeRefinedCompilationConfiguration(
        sourceCode: SourceCode,
        configuration: ResultWithDiagnostics<ScriptCompilationConfiguration>
    ): ResultWithDiagnostics<ScriptCompilationConfiguration>?

    fun clearRefinedCompilationConfiguration(sourceCode: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>?
}

fun ScriptingHostConfiguration.getRefinedCompilationConfiguration(
    source: SourceCode,
): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
    (get(ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache)
        ?: error("ScriptRefinedCompilationConfigurationCache is not configured"))
        .getRefinedCompilationConfiguration(source)

fun ScriptingHostConfiguration.getOrStoreRefinedCompilationConfiguration(
    source: SourceCode,
    refine: (SourceCode, ScriptCompilationConfiguration) -> ResultWithDiagnostics<ScriptCompilationConfiguration>
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    (get(ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache)
        ?: error("ScriptRefinedCompilationConfigurationCache is not configured"))
        .let { cache ->
            cache.getRefinedCompilationConfiguration(source)
                ?: refine(source, getBaseOrDefaultCompilationConfiguration(source).valueOr { return it })
                    .also { cache.storeRefinedCompilationConfiguration(source, it) }
        }

