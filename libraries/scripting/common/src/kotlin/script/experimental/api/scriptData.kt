/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import java.net.URL
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface ScriptSource {
    val location: URL?
    val text: String?

    data class Position(val line: Int, val col: Int, val absolutePos: Int? = null)
    data class Range(val start: Position, val end: Position)
    data class Location(val start: Position, val end: Position? = null)
}

data class ScriptSourceNamedFragment(val name: String?, val range: ScriptSource.Range)

open class ScriptSourceFragments(
    val originalSource: ScriptSource,
    val fragments: List<ScriptSourceNamedFragment>?)

open class ProvidedDeclarations(
    val implicitReceivers: List<KType> = emptyList(), // previous scripts, etc.
    val contextVariables: Map<String, KType> = emptyMap() // external variables
    // Q: do we need context constants and/or types here, e.g.
    // val contextConstants: Map<String, Any?> // or with KType as well
    // val contextTypes: List<KType> // additional (to the classpath) types provided by the environment
    // alternatively:
    // val contextDeclarations: List<Tuple<DeclarationKind, String?, KType, Any?> // kind, name, type, value
    // OR: it should be a HeterogeneousMap too
) {
    object Empty : ProvidedDeclarations()
}

open class ScriptSignature(
    val scriptBase: KClass<*>,
    val providedDeclarations: ProvidedDeclarations
)

open class ResolvingRestrictions {
    data class Rule(
        val allow: Boolean,
        val pattern: String // FQN wildcard
    )

    val rules: Iterable<Rule> = arrayListOf()
}

interface ScriptDependency {
    // Q: anything generic here?
}
