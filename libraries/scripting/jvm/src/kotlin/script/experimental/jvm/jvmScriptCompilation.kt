/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.*

open class JvmScriptCompiler(
    val compilerProxy: KJVMCompilerProxy,
    val cache: CompiledJvmScriptsCache
) : ScriptCompiler {

    override suspend fun compile(configuration: ScriptCompileConfiguration, configurator: ScriptCompilationConfigurator?): ResultWithDiagnostics<CompiledScript<*>> {
        val refinedConfiguration = configurator?.refineConfiguration(configuration)?.let {
            when (it) {
                is ResultWithDiagnostics.Failure -> return it
                is ResultWithDiagnostics.Success -> it.value
                        ?: return ResultWithDiagnostics.Failure("Null script compile configuration received".asErrorDiagnostics())
            }
        } ?: configuration
        val cached = cache[refinedConfiguration[ScriptCompileConfigurationParams.scriptSourceFragments]]

        if (cached != null) return cached.asSuccess()

        return compilerProxy.compile(refinedConfiguration, configurator).also {
            if (it is ResultWithDiagnostics.Success) {
                cache.store(it.value as CompiledScript<*>)
            }
        }
    }
}

interface CompiledJvmScriptsCache {
    operator fun get(script: ScriptSourceFragments): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>)
}

interface KJVMCompilerProxy {
    fun compile(
        scriptCompilerConfiguration: ScriptCompileConfiguration,
        configurator: ScriptCompilationConfigurator?
    ): ResultWithDiagnostics<CompiledScript<*>>
}

class DummyCompiledJvmScriptCache : CompiledJvmScriptsCache {
    override operator fun get(script: ScriptSourceFragments): CompiledScript<*>? = null
    override fun store(compiledScript: CompiledScript<*>) {}
}

