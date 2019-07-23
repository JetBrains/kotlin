/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptJvmCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.impl.withDefaults

interface CompiledJvmScriptsCache {
    fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration)

    object NoCache : CompiledJvmScriptsCache {
        override fun get(
            script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration
        ): CompiledScript<*>? = null

        override fun store(
            compiledScript: CompiledScript<*>, script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration
        ) {
        }
    }
}

open class JvmScriptCompiler(
    baseHostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    compilerProxy: ScriptJvmCompilerProxy? = null,
    val cache: CompiledJvmScriptsCache = CompiledJvmScriptsCache.NoCache
) : ScriptCompiler {

    val hostConfiguration = baseHostConfiguration.withDefaults()

    val compilerProxy: ScriptJvmCompilerProxy = compilerProxy ?: ScriptJvmCompilerIsolated(hostConfiguration)

    override suspend operator fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> =
        scriptCompilationConfiguration.with {
            hostConfiguration(this@JvmScriptCompiler.hostConfiguration)
        }.refineBeforeParsing(script).onSuccess { refinedConfiguration ->
            val cached = cache.get(script, refinedConfiguration)

            if (cached != null) return cached.asSuccess()

            compilerProxy.compile(script, refinedConfiguration).also {
                if (it is ResultWithDiagnostics.Success) {
                    cache.store(it.value, script, refinedConfiguration)
                }
            }
        }
}

