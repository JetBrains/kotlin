/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface ScriptEvaluationEnvironmentKeys

class ScriptEvaluationEnvironment(baseEvaluationEnvironments: Iterable<ScriptEvaluationEnvironment>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseEvaluationEnvironments).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseEvaluationEnvironments: ScriptEvaluationEnvironment, body: Builder.() -> Unit = {}
    ) : this(baseEvaluationEnvironments.asIterable(), body)

    class Builder internal constructor(baseEvaluationEnvironments: Iterable<ScriptEvaluationEnvironment>) :
        ScriptDefinitionKeys,
        PropertiesCollection.Builder(baseEvaluationEnvironments)

    companion object : ScriptEvaluationEnvironmentKeys
}

val ScriptEvaluationEnvironmentKeys.implicitReceivers by PropertiesCollection.key<List<Any>>()

val ScriptEvaluationEnvironmentKeys.contextVariables by PropertiesCollection.key<Map<String, Any?>>() // external variables

val ScriptEvaluationEnvironmentKeys.constructorArgs by PropertiesCollection.key<List<Any?>>()

val ScriptEvaluationEnvironmentKeys.runArgs by PropertiesCollection.key<List<Any?>>()

sealed class ResultValue {
    class Value(val name: String, val value: Any?, val type: String) : ResultValue() {
        override fun toString(): String = "$name: $type = $value"
    }

    object Unit : ResultValue()
}

data class EvaluationResult(val returnValue: ResultValue, val environment: ScriptEvaluationEnvironment?)

interface ScriptEvaluator {

    suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationEnvironment: ScriptEvaluationEnvironment?
    ): ResultWithDiagnostics<EvaluationResult>
}
