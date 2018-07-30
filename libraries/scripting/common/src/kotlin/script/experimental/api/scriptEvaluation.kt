/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface ScriptEvaluationEnvironment : PropertiesCollection {

    companion object : ScriptEvaluationEnvironment {

        class Builder internal constructor() : PropertiesCollection.Builder(), ScriptEvaluationEnvironment {
            override val properties = data
        }

        fun create(body: Builder.() -> Unit): ScriptEvaluationEnvironment = Builder().apply(body)
    }
}

val ScriptEvaluationEnvironment.implicitReceivers by PropertiesCollection.key<List<Any>>()

val ScriptEvaluationEnvironment.contextVariables by PropertiesCollection.key<Map<String, Any?>>() // external variables

val ScriptEvaluationEnvironment.constructorArgs by PropertiesCollection.key<List<Any?>>()

val ScriptEvaluationEnvironment.runArgs by PropertiesCollection.key<List<Any?>>()

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
