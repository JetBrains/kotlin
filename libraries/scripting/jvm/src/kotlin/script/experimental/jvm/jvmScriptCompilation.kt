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

    override suspend fun compile(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration?
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val baseConfiguration = additionalConfiguration?.cloneWithNewParent(scriptDefinition.compilationConfigurator?.defaultConfiguration)
                ?: scriptDefinition.compilationConfigurator?.defaultConfiguration
                ?: ScriptCompileConfiguration()
        val refinedConfiguration =
            if (baseConfiguration.getOrNull(ScriptCompileConfigurationProperties.refineBeforeParsing) == true) {
                if (scriptDefinition.compilationConfigurator == null) {
                    return ResultWithDiagnostics.Failure("Non-null configurator expected".asErrorDiagnostics())
                }
                scriptDefinition.compilationConfigurator!!.refineConfiguration(script, baseConfiguration).let {
                    when (it) {
                        is ResultWithDiagnostics.Failure -> return it
                        is ResultWithDiagnostics.Success -> it.value
                    }
                }
            } else {
                baseConfiguration
            }
        val cached = cache.get(script, refinedConfiguration)

        if (cached != null) return cached.asSuccess()

        return compilerProxy.compile(script, scriptDefinition, refinedConfiguration).also {
            if (it is ResultWithDiagnostics.Success) {
                cache.store(it.value, refinedConfiguration)
            }
        }
    }
}

interface CompiledJvmScriptsCache {
    fun get(script: ScriptSource, configuration: ScriptCompileConfiguration): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, configuration: ScriptCompileConfiguration)
}

interface KJVMCompilerProxy {
    fun compile(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>>
}

class DummyCompiledJvmScriptCache : CompiledJvmScriptsCache {
    override fun get(script: ScriptSource, configuration: ScriptCompileConfiguration): CompiledScript<*>? = null
    override fun store(compiledScript: CompiledScript<*>, configuration: ScriptCompileConfiguration) {}
}

