/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

/**
 * Builder for a `TypeEmbedding`.
 *
 * We split most of the work into `PretypeBuilder`, which builds the type modulo nullability
 * (and potentially other flags).  As a result, the builder does not contain the full building
 * state at any point, though a `TypeBuilder`, `PretypeBuilder` pair does.
 */
class TypeBuilder {
    var isNullable = false

    fun complete(init: TypeBuilder.() -> PretypeBuilder): TypeEmbedding = completeWithPretypeBuilder(init())

    private fun completeWithPretypeBuilder(subBuilder: PretypeBuilder): TypeEmbedding {
        val subResult = subBuilder.complete()
        return if (isNullable) NullableTypeEmbedding(subResult) else subResult
    }

    fun unit() = UnitPretypeBuilder
    fun nothing() = NothingPretypeBuilder
    fun any() = AnyPretypeBuilder
    fun int() = IntPretypeBuilder
    fun boolean() = BooleanPretypeBuilder
    fun function(init: FunctionPretypeBuilder.() -> Unit) = FunctionPretypeBuilder().also { it.init() }
    fun klass(init: ClassPretypeBuilder.() -> Unit) = ClassPretypeBuilder().also { it.init() }
}

fun TypeBuilder.nullableAny(): AnyPretypeBuilder {
    isNullable = true
    return any()
}

fun buildType(init: TypeBuilder.() -> PretypeBuilder): TypeEmbedding = TypeBuilder().complete(init)

