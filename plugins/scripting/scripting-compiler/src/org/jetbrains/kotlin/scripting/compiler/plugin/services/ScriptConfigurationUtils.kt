/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull

fun FirSession.getScriptCompilationConfiguration(
    sourceCode: SourceCode?,
    getDefault: FirScriptDefinitionProviderService.() -> ScriptCompilationConfiguration? = { getDefaultConfiguration().valueOrNull() }
) =
    scriptDefinitionProviderService?.let { providerService ->
        sourceCode?.let { script ->
            providerService.getRefinedConfiguration(script)?.valueOrNull()
                ?: providerService.getBaseConfiguration(script)?.valueOrNull()
                ?: providerService.getDefaultConfiguration().valueOrNull()
        } ?: providerService.getDefault()
    }
