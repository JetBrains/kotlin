/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

interface ScriptCompiler {

    suspend fun compile(
        script: ScriptSource,
        configurator: ScriptCompilationConfigurator? = null,
        additionalConfiguration: ScriptCompileConfiguration? = null // overrides parameters from configurator.defaultConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>>
}


interface CompiledScript<out ScriptBase : Any> {

    val configuration: ScriptCompileConfiguration

    suspend fun instantiate(scriptEvaluationEnvironment: ScriptEvaluationEnvironment): ResultWithDiagnostics<ScriptBase>
}
