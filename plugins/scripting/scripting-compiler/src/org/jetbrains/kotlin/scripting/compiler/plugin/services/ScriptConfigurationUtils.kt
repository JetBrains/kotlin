/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.getRefinedOrBaseCompilationConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.scriptCompilationComponent
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull

@Deprecated("Use ScriptingHost based provider and cache, see `scriptCompilationConfigurationProvider` and `scriptRefinedCompilationConfigurationsCache`")
fun FirSession.getScriptCompilationConfiguration(
    sourceCode: SourceCode?,
    getDefault: FirScriptDefinitionProviderService.() -> ScriptCompilationConfiguration? = { getDefaultConfiguration().valueOrNull() }
): ScriptCompilationConfiguration? {
    val hostConfiguration = scriptCompilationComponent?.hostConfiguration
    return if (hostConfiguration != null) {
        hostConfiguration.getRefinedOrBaseCompilationConfiguration(sourceCode).valueOrNull()
    } else {
        @Suppress("DEPRECATION")
        scriptDefinitionProviderService?.let { providerService ->
            sourceCode?.let { script ->
                providerService.getRefinedConfiguration(script)?.valueOrNull()
                    ?: providerService.getBaseConfiguration(script)?.valueOrNull()
                    ?: providerService.getDefaultConfiguration().valueOrNull()
            } ?: providerService.getDefault()
        }
    }
}
