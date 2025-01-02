/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplCompilerPluginRegistrar
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

class K2ReplCompiler(
    private val state: K2ReplCompilationState,
) : ReplCompiler<CompiledSnippet> {

    override val lastCompiledSnippet: LinkedSnippet<CompiledSnippet>?
        get() = state.lastCompiledSnippet

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        snippets.forEach { snippet ->
            val res = state.compiler.compile(snippet, state.scriptCompilationConfiguration)
            when (res) {
                is ResultWithDiagnostics.Success -> {
                    state.lastCompiledSnippet.add(res.value)
                }
                is ResultWithDiagnostics.Failure -> {
                    return res
                }
            }
        }
        return state.lastCompiledSnippet?.asSuccess() ?: ResultWithDiagnostics.Failure("No snippets provided".asErrorDiagnostics())
    }

    companion object {

        fun createCompilationState(
            messageCollector: ScriptDiagnosticsMessageCollector,
            rootDisposable: Disposable,
            scriptCompilationConfiguration: ScriptCompilationConfiguration,
            hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration
        ): K2ReplCompilationState {
            val compilerContext = createIsolatedCompilationContext(
                scriptCompilationConfiguration,
                hostConfiguration,
                messageCollector,
                rootDisposable
            ) {
                add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ReplCompilerPluginRegistrar(hostConfiguration))
            }

            val compiler = ScriptJvmCompilerFromEnvironment(compilerContext.environment)

            return K2ReplCompilationState(scriptCompilationConfiguration, hostConfiguration, compiler)
        }
    }
}

class K2ReplCompilationState(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val hostConfiguration: ScriptingHostConfiguration,
    val compiler: ScriptJvmCompilerFromEnvironment,
) {
    internal var lastCompiledSnippet: LinkedSnippetImpl<CompiledSnippet>? = null
}
