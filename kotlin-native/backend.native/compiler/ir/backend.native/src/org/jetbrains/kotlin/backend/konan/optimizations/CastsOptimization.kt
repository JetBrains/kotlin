/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.lower.optimizations.AbstractCastsOptimization
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.getInlinedClassNative
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.erasedUpperBound

internal class CastsOptimization(private val generationState: NativeGenerationState) : AbstractCastsOptimization(generationState) {
    override fun IrType.getInlinedClassOrNull(): IrClass? = getInlinedClassNative()

    override fun IrBuilderWithScope.irThrowClassCastException(argument: IrExpression, typeOperand: IrType): IrExpression =
            irCall(generationState.symbols.throwClassCastException).apply {
                val typeOperandClass = typeOperand.erasedUpperBound
                arguments[0] = argument
                arguments[1] = IrClassReferenceImpl(
                        startOffset, endOffset,
                        generationState.symbols.nativePtrType,
                        typeOperandClass.symbol,
                        typeOperandClass.defaultType
                )
            }

    override fun isTrivialValGetter(function: IrSimpleFunction): Boolean =
            function.correspondingPropertySymbol?.owner?.isVar == false && generationState.context.isTrivialGetter(function)
}
