/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.irCatch
import org.jetbrains.kotlin.name.Name

internal fun makeEntryPoint(context: Context): IrFunction {
    val entryPointDescriptor = WrappedSimpleFunctionDescriptor()
    val actualMain = context.ir.symbols.entryPoint!!.owner
    val entryPoint = IrFunctionImpl(
            actualMain.startOffset,
            actualMain.startOffset,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(entryPointDescriptor),
            Name.identifier("Konan_start"),
            DescriptorVisibilities.PRIVATE,
            Modality.FINAL,
            context.irBuiltIns.intType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false
    ).also { function ->
        function.valueParameters = listOf(WrappedValueParameterDescriptor().let {
            IrValueParameterImpl(
                    actualMain.startOffset, actualMain.startOffset,
                    IrDeclarationOrigin.DEFINED,
                    IrValueParameterSymbolImpl(it),
                    Name.identifier("args"),
                    index = 0,
                    varargElementType = null,
                    isCrossinline = false,
                    type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType),
                    isNoinline = false,
                    isHidden = false,
                    isAssignable = false
            ).apply {
                it.bind(this)
                parent = function
            }
        })
    }
    entryPointDescriptor.bind(entryPoint)
    entryPoint.annotations += buildSimpleAnnotation(context.irBuiltIns,
            actualMain.startOffset, actualMain.startOffset,
            context.ir.symbols.exportForCppRuntime.owner, "Konan_start")

    val builder = context.createIrBuilder(entryPoint.symbol)
    entryPoint.body = builder.irBlockBody(entryPoint) {
        +IrTryImpl(
                startOffset = actualMain.startOffset,
                endOffset   = actualMain.startOffset,
                type        = context.irBuiltIns.nothingType
        ).apply {
            tryResult = irBlock {
                +irCall(actualMain).apply {
                    if (actualMain.valueParameters.size != 0)
                        putValueArgument(0, irGet(entryPoint.valueParameters[0]))
                }
                +irReturn(irInt(0))
            }
            catches += irCatch(context.irBuiltIns.throwableType).apply {
                result = irBlock {
                    +irCall(context.ir.symbols.onUnhandledException).apply {
                        putValueArgument(0, irGet(catchParameter))
                    }
                    +irReturn(irInt(1))
                }
            }
        }
    }

    return entryPoint
}
