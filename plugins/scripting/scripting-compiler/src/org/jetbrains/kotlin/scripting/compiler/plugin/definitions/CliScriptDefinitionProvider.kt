/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import org.jetbrains.kotlin.scripting.definitions.LazyScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class CliScriptDefinitionProvider : LazyScriptDefinitionProvider() {
    private val definitionsLock = ReentrantLock()
    private val definitionsFromSources: MutableList<Sequence<ScriptDefinition>> = arrayListOf()
    private val definitions: MutableList<ScriptDefinition> = arrayListOf()
    private var defaultDefinition: ScriptDefinition? = null

    override val currentDefinitions: Sequence<ScriptDefinition>
        get() {
            val base = definitions.asSequence() + definitionsFromSources.asSequence().flatMap { it }
            return base + getDefaultDefinition()
        }

    fun setScriptDefinitions(newDefinitions: List<ScriptDefinition>) {
        definitionsLock.withLock {
            definitions.clear()
            val (withoutStdDef, stdDef) = newDefinitions.partition { !it.isDefault }
            definitions.addAll(withoutStdDef)
            // TODO: consider reporting an error when several default definitions are supplied
            defaultDefinition = stdDef.firstOrNull()
            clearCache()
        }
    }

    fun setScriptDefinitionsSources(newSources: List<ScriptDefinitionsSource>) {
        definitionsLock.withLock {
            definitionsFromSources.clear()
            for (it in newSources) {
                definitionsFromSources.add(it.definitions.constrainOnce())
            }
            clearCache()
        }
    }

    override fun getDefaultDefinition(): ScriptDefinition = defaultDefinition ?: super.getDefaultDefinition()
}
