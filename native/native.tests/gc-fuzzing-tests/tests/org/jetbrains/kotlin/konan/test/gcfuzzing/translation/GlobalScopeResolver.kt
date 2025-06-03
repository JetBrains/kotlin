/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Definition
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.EntityId
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Program
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.TargetLanguage

class GlobalScopeResolver private constructor(
    private val definitions: List<Definition>,
) {
    constructor(program: Program) : this(program.definitions)

    private fun computeEntityId(definition: Definition): EntityId {
        val index = definitions.indexOfFirst {
            it === definition
        }
        check(index != -1) { "Definition of $definition is not found" }
        return index
    }

    fun computeName(definition: Definition.Function): String = "fun${computeEntityId(definition)}"
    fun computeName(definition: Definition.Class): String = "Class${computeEntityId(definition)}"
    fun computeName(definition: Definition.Global): String = "g${computeEntityId(definition)}"

    fun isExported(definition: Definition): Boolean = when (definition) {
        is Definition.Function -> true
        is Definition.Class -> true
        is Definition.Global -> false
    }

    fun isAvailable(definition: Definition, contextLanguage: TargetLanguage): Boolean =
        isExported(definition) || definition.targetLanguage == contextLanguage

    private inline fun <reified T : Definition> resolveDefinition(id: EntityId, contextLanguage: TargetLanguage): T? =
        definitions.filterIsInstance<T>().filter {
            isAvailable(it, contextLanguage)
        }.findEntity(id)

    fun resolveFunction(id: EntityId, contextLanguage: TargetLanguage): Definition.Function? =
        resolveDefinition<Definition.Function>(id, contextLanguage)

    fun resolveClass(id: EntityId, contextLanguage: TargetLanguage): Definition.Class? =
        resolveDefinition<Definition.Class>(id, contextLanguage)

    fun resolveGlobal(id: EntityId, contextLanguage: TargetLanguage): Definition.Global? =
        resolveDefinition<Definition.Global>(id, contextLanguage)
}