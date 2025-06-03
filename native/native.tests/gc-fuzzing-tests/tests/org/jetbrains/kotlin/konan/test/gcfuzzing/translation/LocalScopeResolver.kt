/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Definition
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.EntityId
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Parameter
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.TargetLanguage

class DefinitionLocal(val id: EntityId, val mutable: Boolean)

class LocalScopeResolver(
    private val globalScopeResolver: GlobalScopeResolver,
    private val scopeLanguage: TargetLanguage,
    initialScope: List<Parameter>,
) {
    private val locals = mutableListOf<DefinitionLocal>().apply {
        initialScope.mapIndexedTo(this) { index, _ ->
            DefinitionLocal(index, false)
        }
    }

    fun computeName(definition: Definition.Function): String = globalScopeResolver.computeName(definition)
    fun computeName(definition: Definition.Class): String = globalScopeResolver.computeName(definition)
    fun computeName(definition: Definition.Global): String = globalScopeResolver.computeName(definition)
    fun computeName(definition: DefinitionLocal): String = "l${definition.id}"

    fun isExported(definition: Definition): Boolean = globalScopeResolver.isExported(definition)
    fun isAvailable(definition: Definition): Boolean = globalScopeResolver.isAvailable(definition, scopeLanguage)

    fun resolveFunction(id: EntityId): Definition.Function? = globalScopeResolver.resolveFunction(id, scopeLanguage)
    fun resolveClass(id: EntityId): Definition.Class? = globalScopeResolver.resolveClass(id, scopeLanguage)
    fun resolveGlobal(id: EntityId): Definition.Global? = globalScopeResolver.resolveGlobal(id, scopeLanguage)
    fun resolveLocal(id: EntityId, onlyMutable: Boolean = false): DefinitionLocal? =
        locals.filter { if (onlyMutable) it.mutable else true }.findEntity(id)

    /**
     * Allocate a new local and run [block] with its name.
     *
     * The local will be added to the scope only after [block] has executed.
     */
    fun allocateLocal(block: (String) -> Unit) {
        val local = DefinitionLocal(locals.size, mutable = true)
        block(computeName(local))
        locals.add(local)
    }

    val localsCount: Int
        get() = locals.size
}