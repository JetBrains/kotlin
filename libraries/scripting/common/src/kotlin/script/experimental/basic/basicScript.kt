/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.basic

import kotlin.script.experimental.api.*


class PassThroughCompilationConfigurator(val environment: ScriptingEnvironment) : ScriptCompilationConfigurator {

    override val defaultConfiguration = ScriptCompileConfiguration(
        ScriptCompileConfigurationParams.baseClass to environment[ScriptingEnvironmentParams.baseClass]
    )

    override suspend fun baseConfiguration(scriptSource: ScriptSource): ResultWithDiagnostics<ScriptCompileConfiguration> =
        defaultConfiguration.asSuccess()

    override suspend fun refineConfiguration(
        configuration: ScriptCompileConfiguration,
        processedScriptData: ProcessedScriptData
    ): ResultWithDiagnostics<ScriptCompileConfiguration> =
        configuration.asSuccess()
}

class DummyEvaluator<ScriptBase : Any>(val environment: ScriptingEnvironment) : ScriptEvaluator<ScriptBase> {
    override suspend fun eval(
        compiledScript: CompiledScript<ScriptBase>,
        scriptEvaluationEnvironment: ScriptEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult> =
        ResultWithDiagnostics.Failure("not implemented".asErrorDiagnostics())
}

// TODO: from org.jetbrains.kotlin.utils.addToStdlib, take it from the stdlib when available
private inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

