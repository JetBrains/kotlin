/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

val ScriptingHostConfigurationKeys.scriptCompilationConfigurationProvider by PropertiesCollection.key<ScriptCompilationConfigurationProvider>(isTransient = true)

interface ScriptCompilationConfigurationProvider {
    fun isScript(source: SourceCode): Boolean

    fun findBaseCompilationConfiguration(source: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration>?
    fun getDefaultCompilationConfiguration(): ResultWithDiagnostics<ScriptCompilationConfiguration>
}

fun ScriptingHostConfiguration.getBaseOrDefaultCompilationConfiguration(
    source: SourceCode?
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    (get(ScriptingHostConfiguration.scriptCompilationConfigurationProvider)
        ?: error("ScriptCompilationConfigurationProvider is not configured"))
        .let { provider ->
            source?.let { provider.findBaseCompilationConfiguration(it) }
                ?: provider.getDefaultCompilationConfiguration()
        }
