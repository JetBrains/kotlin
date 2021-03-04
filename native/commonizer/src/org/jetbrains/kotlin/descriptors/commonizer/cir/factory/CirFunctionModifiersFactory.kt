/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmFunction
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionModifiers

object CirFunctionModifiersFactory {
    fun create(source: KmFunction) = CirFunctionModifiers.createInterned(
        isOperator = Flag.Function.IS_OPERATOR(source.flags),
        isInfix = Flag.Function.IS_INFIX(source.flags),
        isInline = Flag.Function.IS_INLINE(source.flags),
        isTailrec = Flag.Function.IS_TAILREC(source.flags),
        isSuspend = Flag.Function.IS_SUSPEND(source.flags),
        isExternal = Flag.Function.IS_EXTERNAL(source.flags)
    )
}
