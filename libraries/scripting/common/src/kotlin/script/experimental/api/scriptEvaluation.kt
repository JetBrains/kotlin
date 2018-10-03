/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

interface ScriptEvaluationConfigurationKeys

/**
 * The container for script evaluation configuration
 * For usages see actual code examples
 */
class ScriptEvaluationConfiguration(baseEvaluationConfigurations: Iterable<ScriptEvaluationConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseEvaluationConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptEvaluationConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseEvaluationConfigurations: Iterable<ScriptEvaluationConfiguration>) :
        ScriptEvaluationConfigurationKeys,
        PropertiesCollection.Builder(baseEvaluationConfigurations)

    companion object : ScriptEvaluationConfigurationKeys
}

/**
 * The list of actual script implicit receiver object, in the same order as specified in {@link ScriptCompilationConfigurationKeys#implicitReceivers}
 */
val ScriptEvaluationConfigurationKeys.implicitReceivers by PropertiesCollection.key<List<Any>>()

/**
 * The map of names to actual provided properties objects, according to the properties specified in
 * {@link ScriptCompilationConfigurationKeys#providedProperties}
 */
val ScriptEvaluationConfigurationKeys.providedProperties by PropertiesCollection.key<Map<String, Any?>>() // external variables

/**
 * Constructor arguments, additional to implicit receivers and provided properties, according to the script base class constructor
 */
val ScriptEvaluationConfigurationKeys.constructorArgs by PropertiesCollection.key<List<Any?>>()

/**
 * The script evaluation result value
 */
sealed class ResultValue {
    class Value(val name: String, val value: Any?, val type: String) : ResultValue() {
        override fun toString(): String = "$name: $type = $value"
    }

    object Unit : ResultValue()
}

/**
 * The facade for the evaluation result and evaluation configuration, used in the evaluator interface
 */
data class EvaluationResult(val returnValue: ResultValue, val configuration: ScriptEvaluationConfiguration?)

/**
 * The functional interface to the script evaluator
 */
interface ScriptEvaluator {

    /**
     * Evaluates [compiledScript] using the data from [scriptEvaluationConfiguration]
     * @param compiledScript the compiled script class
     * @param scriptEvaluationConfiguration evaluation configuration
     */
    suspend operator fun invoke(
        compiledScript: CompiledScript<*>,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult>
}
