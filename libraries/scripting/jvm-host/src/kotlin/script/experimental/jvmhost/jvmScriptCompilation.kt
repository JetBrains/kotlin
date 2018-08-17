/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment
import kotlin.script.experimental.jvmhost.impl.KJvmCompilerImpl
import kotlin.script.experimental.jvmhost.impl.withDefaults

interface CompiledJvmScriptsCache {
    fun get(script: ScriptSource, scriptDefinition: ScriptDefinition): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, scriptDefinition: ScriptDefinition)

    object NoCache : CompiledJvmScriptsCache {
        override fun get(
            script: ScriptSource, scriptDefinition: ScriptDefinition
        ): CompiledScript<*>? = null

        override fun store(
            compiledScript: CompiledScript<*>, scriptDefinition: ScriptDefinition
        ) {
        }
    }
}

open class JvmScriptCompiler(
    hostEnvironment: ScriptingEnvironment = defaultJvmScriptingEnvironment,
    val compilerProxy: KJvmCompilerProxy = KJvmCompilerImpl(hostEnvironment.withDefaults()),
    val cache: CompiledJvmScriptsCache = CompiledJvmScriptsCache.NoCache
) : ScriptCompiler {

    override suspend operator fun invoke(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val refineConfigurationFn = scriptDefinition[ScriptDefinition.refineConfigurationBeforeParsing]
        val refinedConfiguration =
            refineConfigurationFn?.handler?.invoke(ScriptDataFacade(script, scriptDefinition))?.let {
                when (it) {
                    is ResultWithDiagnostics.Failure -> return it
                    is ResultWithDiagnostics.Success -> it.value
                }
            } ?: scriptDefinition
        val cached = cache.get(script, refinedConfiguration)

        if (cached != null) return cached.asSuccess()

        return compilerProxy.compile(script, refinedConfiguration).also {
            if (it is ResultWithDiagnostics.Success) {
                cache.store(it.value, refinedConfiguration)
            }
        }
    }
}

interface KJvmCompilerProxy {
    fun compile(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition
    ): ResultWithDiagnostics<CompiledScript<*>>
}

