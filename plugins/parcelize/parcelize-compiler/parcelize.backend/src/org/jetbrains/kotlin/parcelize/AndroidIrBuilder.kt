/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType

// An IR builder with access to AndroidSymbols and convenience methods to build calls to some of these methods.
class AndroidIrBuilder internal constructor(
    val androidSymbols: AndroidSymbols,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
) : IrBuilderWithScope(IrGeneratorContextBase(androidSymbols.irBuiltIns), Scope(symbol), startOffset, endOffset) {
    fun parcelReadInt(receiver: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelReadInt).apply {
            dispatchReceiver = receiver
        }
    }

    fun parcelReadParcelable(receiver: IrExpression, loader: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelReadParcelable).apply {
            dispatchReceiver = receiver
            putValueArgument(0, loader)
        }
    }

    fun parcelReadString(receiver: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelReadString).apply {
            dispatchReceiver = receiver
        }
    }

    fun parcelWriteInt(receiver: IrExpression, value: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelWriteInt).apply {
            dispatchReceiver = receiver
            putValueArgument(0, value)
        }
    }

    fun parcelWriteParcelable(receiver: IrExpression, p: IrExpression, parcelableFlags: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelWriteParcelable).apply {
            dispatchReceiver = receiver
            putValueArgument(0, p)
            putValueArgument(1, parcelableFlags)
        }
    }

    fun parcelWriteString(receiver: IrExpression, value: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelWriteString).apply {
            dispatchReceiver = receiver
            putValueArgument(0, value)
        }
    }

    fun textUtilsWriteToParcel(cs: IrExpression, p: IrExpression, parcelableFlags: IrExpression): IrExpression {
        return irCall(androidSymbols.textUtilsWriteToParcel).apply {
            putValueArgument(0, cs)
            putValueArgument(1, p)
            putValueArgument(2, parcelableFlags)
        }
    }

    fun classGetClassLoader(receiver: IrExpression): IrExpression {
        return irCall(androidSymbols.classGetClassLoader).apply {
            dispatchReceiver = receiver
        }
    }

    fun getTextUtilsCharSequenceCreator(): IrExpression {
        return irGetField(null, androidSymbols.textUtilsCharSequenceCreator.owner)
    }

    fun unsafeCoerce(value: IrExpression, fromType: IrType, toType: IrType): IrExpression {
        return IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, toType, androidSymbols.unsafeCoerceIntrinsic).apply {
            putTypeArgument(0, fromType)
            putTypeArgument(1, toType)
            putValueArgument(0, value)
        }
    }
}
