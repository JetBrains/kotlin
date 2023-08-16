/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.name.Name

fun IrBuilderWithScope.irCatch() =
        IrCatchImpl(
                startOffset, endOffset,
                buildVariable(
                        this@irCatch.parent,
                        startOffset,
                        endOffset,
                        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                        Name.identifier("e"),
                        context.irBuiltIns.throwableType,
                        isVar = false,
                        isConst = false,
                        isLateinit = false
                )
        )
