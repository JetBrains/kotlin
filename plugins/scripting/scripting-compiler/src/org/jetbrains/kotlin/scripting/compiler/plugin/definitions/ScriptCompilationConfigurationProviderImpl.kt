/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with

class ScriptCompilationConfigurationProviderOverDefinitionProvider(private val provider: ScriptDefinitionProvider) : ScriptCompilationConfigurationProvider {
    override fun isScript(source: SourceCode): Boolean = provider.isScript(source)

    override fun findBaseCompilationConfiguration(source: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>? =
        provider.findDefinition(source)?.compilationConfiguration?.asSuccess()

    override fun getDefaultCompilationConfiguration(): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        provider.getDefaultDefinition().compilationConfiguration.asSuccess()
}

fun ScriptingHostConfiguration.withCompilationConfigurationProvider(
    implementation : ScriptCompilationConfigurationProvider
): ScriptingHostConfiguration = with {
    ScriptingHostConfiguration.scriptCompilationConfigurationProvider(implementation)
}
