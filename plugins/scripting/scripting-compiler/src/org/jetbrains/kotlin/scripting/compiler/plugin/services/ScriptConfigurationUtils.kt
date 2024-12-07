/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.StringScriptSource

fun FirSession.getScriptCompilationConfiguration(
    sourceCode: SourceCode?,
    getDefault: FirScriptDefinitionProviderService.() -> ScriptCompilationConfiguration? = { definitionProvider?.getDefaultDefinition()?.compilationConfiguration }
) =
    scriptDefinitionProviderService?.let { providerService ->
        sourceCode?.let { script ->
            val ktFile = (script as? KtFileScriptSource)?.ktFile ?: error("only PSI scripts are supported at the moment")
            providerService.configurationProvider?.getScriptConfigurationResult(ktFile)?.valueOrNull()?.configuration
                ?: providerService.getDefault()
        } ?: providerService.getDefault()
    }
