/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import java.net.URL
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptSource {
    val location: URL?
    val text: String?

    data class Position(val line: Int, val col: Int, val absolutePos: Int? = null)
    data class Range(val start: Position, val end: Position)
    data class Location(val start: Position, val end: Position? = null)
}

data class ScriptSourceNamedFragment(val name: String?, val range: ScriptSource.Range)

enum class ScriptBodyTarget {
    Constructor,
    SAMFunction
}

data class ResolvingRestrictionRule(
    val action: Action,
    val pattern: String // FQN wildcard
) {
    enum class Action {
        Allow,
        Deny
    }
}

interface ScriptDependency {
    // Q: anything generic here?
}


interface ScriptCollectedDataKeys

class ScriptCollectedData(properties: Map<PropertiesCollection.Key<*>, Any>) : PropertiesCollection(properties) {

    companion object : ScriptCollectedDataKeys
}

val ScriptCollectedDataKeys.foundAnnotations by PropertiesCollection.key<List<Annotation>>()

val ScriptCollectedDataKeys.foundFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

class ScriptConfigurationRefinementContext(
    val source: ScriptSource,
    val compilationConfiguration: ScriptCompilationConfiguration,
    val collectedData: ScriptCollectedData? = null
)
