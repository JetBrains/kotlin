/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
     * The source code position
     * @param line source code position line
     * @param col source code position column
     * @param absolutePos absolute source code text position, if available
     */
    data class Position(val line: Int, val col: Int, val absolutePos: Int? = null)

    /**
     * The source code positions range
     * @param start range start position
     * @param end range end position (after the last char)
     */
    data class Range(val start: Position, val end: Position)

    /**
     * The source code location, pointing either at a position or at a range
     * @param start location start position
     * @param end optional range location end position (after the last char)
     */
    data class Location(val start: Position, val end: Position? = null)
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
data class ScriptSourceNamedFragment(val name: String?, val range: SourceCode.Range)

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
 * The facade to the script data for refinement callbacks
 */
class ScriptConfigurationRefinementContext(
    val script: SourceCode,
    val compilationConfiguration: ScriptCompilationConfiguration,
    val collectedData: ScriptCollectedData? = null
)
