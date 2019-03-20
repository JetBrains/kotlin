/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.legacy

import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDefinitionsSource
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.scripting.shared.definitions.LazyScriptDefinitionProvider
import kotlin.concurrent.write

open class CliScriptDefinitionProvider : LazyScriptDefinitionProvider() {
    private val definitionsFromSources: MutableList<Sequence<KotlinScriptDefinition>> = arrayListOf()
    private val definitions: MutableList<KotlinScriptDefinition> = arrayListOf()
    private var hasStandardDefinition = true

    override val currentDefinitions: Sequence<KotlinScriptDefinition>
        get() {
            val base = definitions.asSequence() + definitionsFromSources.asSequence().flatMap { it }
            return if (hasStandardDefinition) base + getDefaultScriptDefinition() else base
        }

    override fun getDefaultScriptDefinition(): KotlinScriptDefinition {
        return StandardScriptDefinition
    }

    fun setScriptDefinitions(newDefinitions: List<KotlinScriptDefinition>) {
        lock.write {
            definitions.clear()
            val (withoutStdDef, stdDef) = newDefinitions.partition { it != getDefaultScriptDefinition() }
            definitions.addAll(withoutStdDef)
            hasStandardDefinition = stdDef.isNotEmpty()
        }
    }

    fun setScriptDefinitionsSources(newSources: List<ScriptDefinitionsSource>) {
        lock.write {
            definitionsFromSources.clear()
            for (it in newSources) {
                definitionsFromSources.add(it.definitions.constrainOnce())
            }
        }
    }
}
