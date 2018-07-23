/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment
import kotlin.script.experimental.jvmhost.impl.KJvmCompilerImpl
import kotlin.script.experimental.util.chainPropertyBags

interface CompiledJvmScriptsCache {
    fun get(script: ScriptSource, configuration: ScriptCompileConfiguration): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, configuration: ScriptCompileConfiguration)

    object NoCache : CompiledJvmScriptsCache {
        override fun get(script: ScriptSource, configuration: ScriptCompileConfiguration): CompiledScript<*>? = null
        override fun store(compiledScript: CompiledScript<*>, configuration: ScriptCompileConfiguration) {}
    }
}

open class JvmScriptCompiler(
    hostEnvironment: ScriptingEnvironment = defaultJvmScriptingEnvironment,
    val compilerProxy: KJvmCompilerProxy = KJvmCompilerImpl(hostEnvironment.cloneWithNewParent(defaultJvmScriptingEnvironment)),
    val cache: CompiledJvmScriptsCache = CompiledJvmScriptsCache.NoCache
) : ScriptCompiler {

    override suspend operator fun invoke(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration?
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val baseConfiguration = chainPropertyBags(additionalConfiguration, scriptDefinition)
        val refineConfigurationFn = baseConfiguration.getOrNull(ScriptDefinitionProperties.refineConfigurationHandler)
        val refinedConfiguration =
            if (baseConfiguration.getOrNull(ScriptDefinitionProperties.refineConfigurationBeforeParsing) == true) {
                if (refineConfigurationFn == null) {
                    return ResultWithDiagnostics.Failure("Non-null configurator expected".asErrorDiagnostics())
                }
                refineConfigurationFn(script, baseConfiguration).let {
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

interface KJvmCompilerProxy {
    fun compile(
        script: ScriptSource,
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>>
}

