/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType


typealias TemplateGroup<TBuilder> = () -> Sequence<SourceTemplate<TBuilder>>

abstract class TemplateGroupBase<TBuilder> : TemplateGroup<TBuilder> {

    override fun invoke(): Sequence<SourceTemplate<TBuilder>> = sequence {
        with(this@TemplateGroupBase) {
            this::class.members.filter { it.name.startsWith("f_") }.forEach {
                require(it.parameters.size == 1) { "Template $it violates naming convention" }
                @Suppress("UNCHECKED_CAST")
                when {
                    it.returnType.isSubtypeOf(typeSourceTemplate) ->
                        yield(it.call(this) as SourceTemplate<TBuilder>)
                    it.returnType.isSubtypeOf(typeIterableOfSourceTemplates) ->
                        yieldAll(it.call(this) as Iterable<SourceTemplate<TBuilder>>)
                    it.returnType.isSubtypeOf(typeSequenceOfSourceTemplates) ->
                        yieldAll(it.call(this) as Sequence<SourceTemplate<TBuilder>>)
                    else ->
                        error("Template $it violates naming convention")
                }
            }
        }
    }.run {
        if (defaultActions.isEmpty()) this else onEach { t -> defaultActions.forEach(t::builder) }
    }

    private val defaultActions = mutableListOf<Action<TBuilder>>()

    fun defaultBuilder(builderAction: Action<TBuilder>) {
        defaultActions += builderAction
    }

    companion object {
        private val typeSourceTemplate = SourceTemplate::class.starProjectedType
        private val typeIterableOfSourceTemplates = Iterable::class.createType(arguments = listOf(KTypeProjection.invariant(typeSourceTemplate)))
        private val typeSequenceOfSourceTemplates = Sequence::class.createType(arguments = listOf(KTypeProjection.invariant(typeSourceTemplate)))
    }

}

typealias MemberTemplateGroupBase = TemplateGroupBase<MemberBuilder>
typealias TestTemplateGroupBase = TemplateGroupBase<TestBuilder>