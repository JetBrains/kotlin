/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface ScriptEvaluationConfigurationKeys

class ScriptEvaluationConfiguration(baseEvaluationEnvironments: Iterable<ScriptEvaluationConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseEvaluationEnvironments).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptEvaluationConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseEvaluationEnvironments: Iterable<ScriptEvaluationConfiguration>) :
        ScriptCompilationConfigurationKeys,
        PropertiesCollection.Builder(baseEvaluationEnvironments)

    companion object : ScriptEvaluationConfigurationKeys
}

val ScriptEvaluationConfigurationKeys.implicitReceivers by PropertiesCollection.key<List<Any>>()

val ScriptEvaluationConfigurationKeys.contextVariables by PropertiesCollection.key<Map<String, Any?>>() // external variables

val ScriptEvaluationConfigurationKeys.constructorArgs by PropertiesCollection.key<List<Any?>>()

val ScriptEvaluationConfigurationKeys.runArgs by PropertiesCollection.key<List<Any?>>()

sealed class ResultValue {
    class Value(val name: String, val value: Any?, val type: String) : ResultValue() {
        override fun toString(): String = "$name: $type = $value"
    }

    object Unit : ResultValue()
}

data class EvaluationResult(val returnValue: ResultValue, val configuration: ScriptEvaluationConfiguration?)

interface ScriptEvaluator {

    suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult>
}
