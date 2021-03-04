/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionModifiers

class FunctionModifiersCommonizer : AbstractStandardCommonizer<CirFunctionModifiers, CirFunctionModifiers>() {
    private class MutableModifiers(
        var isOperator: Boolean,
        var isInfix: Boolean,
        var isInline: Boolean,
        var isTailrec: Boolean,
        var isSuspend: Boolean,
        var isExternal: Boolean
    ) {
        constructor(immutable: CirFunctionModifiers) : this(
            isOperator = immutable.isOperator,
            isInfix = immutable.isInfix,
            isInline = immutable.isInline,
            isTailrec = immutable.isTailrec,
            isSuspend = immutable.isSuspend,
            isExternal = immutable.isExternal
        )

        fun toImmutableModifiers() = CirFunctionModifiers.createInterned(
            isOperator = isOperator,
            isInfix = isInfix,
            isInline = isInline,
            isTailrec = isTailrec,
            isSuspend = isSuspend,
            isExternal = isExternal
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
        modifiers.isTailrec = modifiers.isTailrec && next.isTailrec
        modifiers.isExternal = modifiers.isExternal && next.isExternal

        return true
    }
}
