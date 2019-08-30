/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getEvaluationContext
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptEvaluationConfigurationKeys

/**
 * The container for script evaluation configuration
 * For usages see actual code examples
 */
open class ScriptEvaluationConfiguration(baseEvaluationConfigurations: Iterable<ScriptEvaluationConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseEvaluationConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptEvaluationConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseEvaluationConfigurations: Iterable<ScriptEvaluationConfiguration>) :
        ScriptEvaluationConfigurationKeys,
        PropertiesCollection.Builder(baseEvaluationConfigurations)

    companion object : ScriptEvaluationConfigurationKeys

    object Default : ScriptEvaluationConfiguration()
}

/**
 * An alternative to the constructor with base configuration, which returns a new configuration only if [body] adds anything
 * to the original one, otherwise returns original
 */
fun ScriptEvaluationConfiguration?.with(body: ScriptEvaluationConfiguration.Builder.() -> Unit): ScriptEvaluationConfiguration {
    val newConfiguration =
        if (this == null) ScriptEvaluationConfiguration(body = body)
        else ScriptEvaluationConfiguration(this, body = body)
    return if (newConfiguration != this) newConfiguration else this
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
 * If the script is a snippet in a REPL, this property expected to contain previous REPL snippets in historical order
 * For the first snippet in a REPL an empty list should be passed explicitly
 * An array of the previous snippets will be passed to the current snippet constructor
 */
val ScriptEvaluationConfigurationKeys.previousSnippets by PropertiesCollection.key<List<Any?>>(isTransient = true)

@Deprecated("use scriptsInstancesSharing flag instead", level = DeprecationLevel.ERROR)
val ScriptEvaluationConfigurationKeys.scriptsInstancesSharingMap by PropertiesCollection.key<MutableMap<KClass<*>, EvaluationResult>>(isTransient = true)

/**
 * If enabled - the evaluator will try to get imported script from a shared container
 * only create/evaluate instances if not found, and evaluator will put newly created instances into the container
 * This allows to have a single instance of the script if it is imported several times via different import paths.
 */
val ScriptEvaluationConfigurationKeys.scriptsInstancesSharing by PropertiesCollection.key<Boolean>(false)

/**
 * Scripting host configuration
 */
val ScriptEvaluationConfigurationKeys.hostConfiguration by PropertiesCollection.key<ScriptingHostConfiguration>(isTransient = true)

/**
 * The callback that will be called on the script compilation immediately before starting the compilation
 */
val ScriptEvaluationConfigurationKeys.refineConfigurationBeforeEvaluate by PropertiesCollection.key<List<RefineEvaluationConfigurationData>>(isTransient = true)

/**
 * A helper to enable scriptsInstancesSharingMap with default implementation
 */
fun ScriptEvaluationConfiguration.Builder.enableScriptsInstancesSharing() {
    this {
        scriptsInstancesSharing(true)
    }
}

/**
 * A helper to enable passing lambda directly to the refinement "keyword"
 */
fun ScriptEvaluationConfiguration.Builder.refineConfigurationBeforeEvaluate(handler: RefineScriptEvaluationConfigurationHandler) {
    ScriptEvaluationConfiguration.refineConfigurationBeforeEvaluate.append(RefineEvaluationConfigurationData(handler))
}

/**
 * The refinement callback function signature
 */
typealias RefineScriptEvaluationConfigurationHandler =
            (ScriptEvaluationConfigurationRefinementContext) -> ResultWithDiagnostics<ScriptEvaluationConfiguration>

data class RefineEvaluationConfigurationData(
    val handler: RefineScriptEvaluationConfigurationHandler
) : Serializable {
    companion object { private const val serialVersionUID: Long = 1L }
}

fun ScriptEvaluationConfiguration.refineBeforeEvaluation(
    script: CompiledScript<*>,
    contextData: ScriptEvaluationContextData? = null
): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val hostConfiguration = get(ScriptEvaluationConfiguration.hostConfiguration)
    val baseContextData = hostConfiguration?.get(ScriptingHostConfiguration.getEvaluationContext)?.invoke(hostConfiguration)
    val actualContextData = merge(baseContextData, contextData)
    return simpleRefineImpl(ScriptEvaluationConfiguration.refineConfigurationBeforeEvaluate) { config, refineData ->
        refineData.handler.invoke(ScriptEvaluationConfigurationRefinementContext(script, config, actualContextData))
    }
}

/**
 * The script evaluation result value
 */
sealed class ResultValue(val scriptClass: KClass<*>? = null, val scriptInstance: Any? = null) {

    /**
     * The result value representing a script return value - the value of the last expression in the script
     * @param name assigned name of the result field - used e.g. in REPL
     * @param value actual result value
     * @param type name of the result type
     * @param scriptClass the loaded class of the script
     * @param scriptInstance instance of the script class
     */
    class Value(val name: String, val value: Any?, val type: String, scriptClass: KClass<*>?, scriptInstance: Any?) :
        ResultValue(scriptClass, scriptInstance) {

        override fun toString(): String = "$name: $type = $value"
    }

    /**
     * The result value representing unit result, e.g. when the script ends with a statement
     * @param scriptClass the loaded class of the script
     * @param scriptInstance instance of the script class
     */
    class Unit(scriptClass: KClass<*>, scriptInstance: Any) : ResultValue(scriptClass, scriptInstance) {
        override fun toString(): String = "Unit"
    }

    /**
     * The result value representing an exception from script itself
     * @param error the actual exception thrown on script evaluation
     * @param wrappingException the wrapping exception e.g. InvocationTargetException, sometimes useful for calculating the relevant stacktrace
     * @param scriptClass the loaded class of the script, if any
     */
    class Error(val error: Throwable, val wrappingException: Throwable? = null, scriptClass: KClass<*>? = null) : ResultValue(scriptClass) {
        override fun toString(): String = error.toString()
    }

    /**
     * The result value used in non-evaluating "evaluators"
     */
    object NotEvaluated : ResultValue()
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
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration.Default
    ): ResultWithDiagnostics<EvaluationResult>
}
