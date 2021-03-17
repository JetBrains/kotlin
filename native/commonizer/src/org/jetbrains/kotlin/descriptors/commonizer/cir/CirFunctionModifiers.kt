/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.Interner

interface CirFunctionModifiers {
    val isOperator: Boolean
    val isInfix: Boolean
    val isInline: Boolean
    val isTailrec: Boolean
    val isSuspend: Boolean
    val isExternal: Boolean

    companion object {
        fun createInterned(
            isOperator: Boolean,
            isInfix: Boolean,
            isInline: Boolean,
            isTailrec: Boolean,
            isSuspend: Boolean,
            isExternal: Boolean
        ): CirFunctionModifiers = interner.intern(
            CirFunctionModifiersInternedImpl(
                isOperator = isOperator,
                isInfix = isInfix,
                isInline = isInline,
                isTailrec = isTailrec,
                isSuspend = isSuspend,
                isExternal = isExternal
            )
        )

        private val interner = Interner<CirFunctionModifiersInternedImpl>()
    }
}

private data class CirFunctionModifiersInternedImpl(
    override val isOperator: Boolean,
    override val isInfix: Boolean,
    override val isInline: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean,
    override val isExternal: Boolean
) : CirFunctionModifiers
