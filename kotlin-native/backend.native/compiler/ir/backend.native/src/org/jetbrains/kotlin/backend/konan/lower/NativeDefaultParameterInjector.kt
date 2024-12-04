/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.computePrimitiveBinaryTypeOrNull
import org.jetbrains.kotlin.backend.konan.getInlinedClassNative
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallWithSubstitutedType
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType

internal class NativeDefaultParameterInjector(context: KonanBackendContext) : DefaultParameterInjector<KonanBackendContext>(
        context = context,
        factory = NativeDefaultArgumentFunctionFactory(context),
        skipInline = false
) {

    override fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression {
        val symbols = context.ir.symbols

        // Actual scope for builder is the current function that we don't have access to. So we put a new symbol as scope here,
        // but it will not affect the result because we are not creating any declarations here.
        fun createIrBuilder() = context.irBuiltIns.createIrBuilder(IrSimpleFunctionSymbolImpl(), startOffset, endOffset)

        val nullConstOfEquivalentType = when (type.computePrimitiveBinaryTypeOrNull()) {
            null -> IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            PrimitiveBinaryType.BOOLEAN -> IrConstImpl.boolean(startOffset, endOffset, type, false)
            PrimitiveBinaryType.BYTE -> IrConstImpl.byte(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.SHORT -> when (type.getInlinedClassNative()) {
                context.irBuiltIns.charClass -> IrConstImpl.char(startOffset, endOffset, type, 0.toChar())
                else -> IrConstImpl.short(startOffset, endOffset, type, 0)
            }
            PrimitiveBinaryType.INT -> IrConstImpl.int(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.LONG -> IrConstImpl.long(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.FLOAT -> IrConstImpl.float(startOffset, endOffset, type, 0.0F)
            PrimitiveBinaryType.DOUBLE -> IrConstImpl.double(startOffset, endOffset, type, 0.0)
            PrimitiveBinaryType.POINTER -> with(createIrBuilder()) { irCall(symbols.getNativeNullPtr.owner) }
            PrimitiveBinaryType.VECTOR128 -> TODO()
        }

        return with(createIrBuilder()) {
            irCallWithSubstitutedType(symbols.reinterpret, listOf(nullConstOfEquivalentType.type, type)).apply {
                extensionReceiver = nullConstOfEquivalentType
            }
        }
    }
}
