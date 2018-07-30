/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment
import kotlin.script.experimental.jvmhost.impl.KJvmCompilerImpl
import kotlin.script.experimental.jvmhost.impl.mergeConfigurations
import kotlin.script.experimental.jvmhost.impl.withDefaults
import kotlin.script.experimental.util.getOrNull

interface CompiledJvmScriptsCache {
    fun get(script: ScriptSource, scriptDefinition: ScriptDefinition, configuration: ScriptCompileConfiguration?): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, scriptDefinition: ScriptDefinition, configuration: ScriptCompileConfiguration?)

    object NoCache : CompiledJvmScriptsCache {
        override fun get(
            script: ScriptSource, scriptDefinition: ScriptDefinition, configuration: ScriptCompileConfiguration?
        ): CompiledScript<*>? = null

        override fun store(
            compiledScript: CompiledScript<*>, scriptDefinition: ScriptDefinition, configuration: ScriptCompileConfiguration?
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
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration?
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val refineConfigurationFn = scriptDefinition.getOrNull(ScriptDefinition.refineConfigurationHandler)
        val refinedConfiguration =
            if (scriptDefinition.getOrNull(ScriptDefinition.refineConfigurationBeforeParsing) == true) {
                if (refineConfigurationFn == null) {
                    return ResultWithDiagnostics.Failure("Non-null configurator expected".asErrorDiagnostics())
                }
                refineConfigurationFn(script, scriptDefinition, additionalConfiguration).let {
                    when (it) {
                        is ResultWithDiagnostics.Failure -> return it
                        is ResultWithDiagnostics.Success -> it.value
                    }
                }
            } else {
                null
            }
        val actualConfiguration = mergeConfigurations(additionalConfiguration, refinedConfiguration)
        val cached = cache.get(script, scriptDefinition, actualConfiguration)

        if (cached != null) return cached.asSuccess()

        return compilerProxy.compile(script, scriptDefinition, actualConfiguration).also {
            if (it is ResultWithDiagnostics.Success) {
                cache.store(it.value, scriptDefinition, actualConfiguration)
            }
        }
    }
}

interface KJvmCompilerProxy {
    fun compile(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration?
    ): ResultWithDiagnostics<CompiledScript<*>>
}

