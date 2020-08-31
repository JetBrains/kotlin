/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.internal

import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.impl.CompoundGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor

internal abstract class InternalGeneratedCode : GeneratedCode {
    open val isOverridable: Boolean
        get() = false

    override fun GeneratedCode.Builder.body() {
        throw IllegalArgumentException(
            "Unexpected call to `body` to decompose a Leaf in the Generated Code Tree. Do not call `GeneratedCode.body()` directly."
        )
    }

    abstract fun GeneratedCodeVisitor.visit(indent: Int)
}

// - Decomposition to instances of Internal Generated Code. A.K.A: The leaves in a Tree.

internal fun GeneratedCode.leaves(): List<InternalGeneratedCode> {
    return mutableListOf<InternalGeneratedCode>().apply { addAllLeaves(this@leaves) }.toList()
}

internal fun MutableList<InternalGeneratedCode>.addAllLeaves(code: GeneratedCode) {
    when (code) {
        is GeneratedCode.Empty -> Unit
        is CompoundGeneratedCode -> {
            for (element in code.codeList) {
                add(element)
            }
        }
        is InternalGeneratedCode -> {
            add(code)
        }
        else -> {
            val body = code.body()
            require(body != code) {
                "Generated code returns itself in the body. This is not allowed"
            }
            addAllLeaves(body)
        }
    }
}

