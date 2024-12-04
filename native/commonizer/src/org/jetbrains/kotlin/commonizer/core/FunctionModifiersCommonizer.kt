/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirFunctionModifiers

class FunctionModifiersCommonizer : AbstractStandardCommonizer<CirFunctionModifiers, CirFunctionModifiers>() {
    private class MutableModifiers(
        var isOperator: Boolean,
        var isInfix: Boolean,
        var isInline: Boolean,
        var isSuspend: Boolean,
    ) {
        constructor(immutable: CirFunctionModifiers) : this(
            isOperator = immutable.isOperator,
            isInfix = immutable.isInfix,
            isInline = immutable.isInline,
            isSuspend = immutable.isSuspend,
        )

        fun toImmutableModifiers() = CirFunctionModifiers.createInterned(
            isOperator = isOperator,
            isInfix = isInfix,
            isInline = isInline,
            isSuspend = isSuspend,
        )
    }

    private lateinit var modifiers: MutableModifiers

    override fun commonizationResult() = modifiers.toImmutableModifiers()

    override fun initialize(first: CirFunctionModifiers) {
        modifiers = MutableModifiers(first)
    }

    override fun doCommonizeWith(next: CirFunctionModifiers): Boolean {
        if (modifiers.isSuspend != next.isSuspend)
            return false

        modifiers.isOperator = modifiers.isOperator && next.isOperator
        modifiers.isInfix = modifiers.isInfix && next.isInfix
        modifiers.isInline = modifiers.isInline && next.isInline

        return true
    }
}
