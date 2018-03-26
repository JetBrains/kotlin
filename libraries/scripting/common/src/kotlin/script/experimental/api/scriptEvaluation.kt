/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

open class ScriptEvaluationEnvironmentParams : HeterogeneousMapBuilder() {
    companion object {
        val implicitReceivers by typedKey<List<Any>>()

        val contextVariables by typedKey<Map<String, Any?>>() // external variables

        val constructorArgs by typedKey<List<Any?>>()

        val runArgs by typedKey<List<Any?>>()
    }
}

inline fun scriptEvaluationEnvironment(from: HeterogeneousMap = HeterogeneousMap(), body: ScriptEvaluationEnvironmentParams.() -> Unit) =
    ScriptEvaluationEnvironmentParams().build(from, body)

typealias ScriptEvaluationEnvironment = HeterogeneousMap

data class EvaluationResult(val returnValue: Any?, val environment: ScriptEvaluationEnvironment)

// NOTE: name inconsistency: run vs evaluate
interface ScriptEvaluator<in ScriptBase : Any> {

    // constructor(environment: ScriptingEnvironment) // the constructor is expected from implementations

    suspend fun eval(
        compiledScript: CompiledScript<ScriptBase>,
        scriptEvaluationEnvironment: ScriptEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult>
}
