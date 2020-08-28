/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionModifiers

data class CirFunctionModifiersImpl(
    override val isOperator: Boolean,
    override val isInfix: Boolean,
    override val isInline: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean,
    override val isExternal: Boolean
) : CirFunctionModifiers
