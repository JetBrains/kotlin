/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirFunctionModifiers

class FunctionModifiersCommonizer : Commonizer<CirFunctionModifiers, CirFunctionModifiers> {
    private var modifiers: CirFunctionModifiersImpl? = null
    private var error = false

    override val result: CirFunctionModifiers
        get() = modifiers?.takeIf { !error } ?: throw IllegalCommonizerStateException()

    override fun commonizeWith(next: CirFunctionModifiers): Boolean {
        if (error)
            return false

        val modifiers = modifiers
        if (modifiers == null)
            this.modifiers = CirFunctionModifiersImpl(next)
        else {
            if (modifiers.isSuspend != next.isSuspend)
                error = true
            else {
                modifiers.isOperator = modifiers.isOperator && next.isOperator
                modifiers.isInfix = modifiers.isInfix && next.isInfix
                modifiers.isInline = modifiers.isInline && next.isInline
                modifiers.isTailrec = modifiers.isTailrec && next.isTailrec
                modifiers.isExternal = modifiers.isExternal && next.isExternal
            }
        }

        return !error
    }

    private data class CirFunctionModifiersImpl(
        override var isOperator: Boolean,
        override var isInfix: Boolean,
        override var isInline: Boolean,
        override var isTailrec: Boolean,
        override var isSuspend: Boolean,
        override var isExternal: Boolean
    ) : CirFunctionModifiers {
        constructor(function: CirFunctionModifiers) : this(
            function.isOperator,
            function.isInfix,
            function.isInline,
            function.isTailrec,
            function.isSuspend,
            function.isExternal
        )
    }
}
