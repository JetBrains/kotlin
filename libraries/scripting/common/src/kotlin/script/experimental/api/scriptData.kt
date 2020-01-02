/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import java.io.Serializable
import java.net.URL
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The interface to the script or snippet source code
 */
interface SourceCode {
    /**
     * The source code text
     */
    val text: String

    /**
     * The script file or display name
     */
    val name: String?

    /**
     * The path or other script location identifier
     */
    val locationId: String?

    /**
     * The source code position
     * @param line source code position line
     * @param col source code position column
     * @param absolutePos absolute source code text position, if available
     */
    data class Position(val line: Int, val col: Int, val absolutePos: Int? = null) : Serializable

    /**
     * The source code positions range
     * @param start range start position
     * @param end range end position (after the last char)
     */
    data class Range(val start: Position, val end: Position) : Serializable

    /**
     * The source code location, pointing either at a position or at a range
     * @param start location start position
     * @param end optional range location end position (after the last char)
     */
    data class Location(val start: Position, val end: Position? = null) : Serializable
}

/**
 * The interface for the source code located externally
 */
interface ExternalSourceCode : SourceCode {
    /**
     * The source code location url
     */
    val externalLocation: URL
}

/**
 * The source code [range] with the the optional [name]
 */
data class ScriptSourceNamedFragment(val name: String?, val range: SourceCode.Range) : Serializable {
    companion object { private const val serialVersionUID: Long = 1L }
}

/**
 * The general interface to the Script dependency (see platform-specific implementations)
 */
interface ScriptDependency : Serializable

interface ScriptCollectedDataKeys

/**
 * The container for script data collected during compilation
 * Used for transferring data to the configuration refinement callbacks
 */
class ScriptCollectedData(properties: Map<PropertiesCollection.Key<*>, Any>) : PropertiesCollection(properties) {

    companion object : ScriptCollectedDataKeys
}

/**
 * The script file-level annotations found during script source parsing
 */
val ScriptCollectedDataKeys.foundAnnotations by PropertiesCollection.key<List<Annotation>>()

/**
 * The facade to the script data for compilation configuration refinement callbacks
 */
data class ScriptConfigurationRefinementContext(
    val script: SourceCode,
    val compilationConfiguration: ScriptCompilationConfiguration,
    val collectedData: ScriptCollectedData? = null
)

interface ScriptEvaluationContextDataKeys

/**
 * The container for script evaluation context data
 * Used for transferring data to the evaluation refinement callbacks
 */
class ScriptEvaluationContextData(baseConfigurations: Iterable<ScriptEvaluationContextData>, body: Builder.() -> Unit = {}) :
    PropertiesCollection(Builder(baseConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptEvaluationContextData, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseConfigurations: Iterable<ScriptEvaluationContextData>) :
        ScriptEvaluationContextDataKeys,
        PropertiesCollection.Builder(baseConfigurations)

    companion object : ScriptEvaluationContextDataKeys
}

/**
 * optimized alternative to the constructor with multiple base configurations
 */
fun merge(vararg contexts: ScriptEvaluationContextData?): ScriptEvaluationContextData? {
    val nonEmpty = ArrayList<ScriptEvaluationContextData>()
    for (data in contexts) {
        if (data != null && !data.isEmpty()) {
            nonEmpty.add(data)
        }
    }
    return when {
        nonEmpty.isEmpty() -> null
        nonEmpty.size == 1 -> nonEmpty.first()
        else -> ScriptEvaluationContextData(nonEmpty.asIterable())
    }
}

/**
 * Command line arguments of the current process, could be provided by an evaluation host
 */
val ScriptEvaluationContextDataKeys.commandLineArgs by PropertiesCollection.key<List<String>>()

/**
 * The facade to the script data for evaluation configuration refinement callbacks
 */
data class ScriptEvaluationConfigurationRefinementContext(
    val compiledScript: CompiledScript<*>,
    val evaluationConfiguration: ScriptEvaluationConfiguration,
    val contextData: ScriptEvaluationContextData? = null
)
