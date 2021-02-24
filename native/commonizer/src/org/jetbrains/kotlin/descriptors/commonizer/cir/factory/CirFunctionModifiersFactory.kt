/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionModifiers
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirFunctionModifiersImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner

object CirFunctionModifiersFactory {
    private val interner = Interner<CirFunctionModifiers>()

    fun create(source: SimpleFunctionDescriptor): CirFunctionModifiers = create(
        isOperator = source.isOperator,
        isInfix = source.isInfix,
        isInline = source.isInline,
        isTailrec = source.isTailrec,
        isSuspend = source.isSuspend,
        isExternal = source.isExternal
    )

    fun create(
        isOperator: Boolean,
        isInfix: Boolean,
        isInline: Boolean,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isExternal: Boolean
    ): CirFunctionModifiers {
        return interner.intern(
            CirFunctionModifiersImpl(
                isOperator = isOperator,
                isInfix = isInfix,
                isInline = isInline,
                isTailrec = isTailrec,
                isSuspend = isSuspend,
                isExternal = isExternal
            )
        )
    }
}
