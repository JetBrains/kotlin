/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
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
            arguments[0] = receiver
            arguments[1] = loader
        }
    }

    fun parcelReadString(receiver: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelReadString).apply {
            arguments[0] = receiver
        }
    }

    fun parcelWriteInt(receiver: IrExpression, value: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelWriteInt).apply {
            arguments[0] = receiver
            arguments[1] = value
        }
    }

    fun parcelWriteParcelable(receiver: IrExpression, p: IrExpression, parcelableFlags: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelWriteParcelable).apply {
            arguments[0] = receiver
            arguments[1] = p
            arguments[2] = parcelableFlags
        }
    }

    fun parcelWriteString(receiver: IrExpression, value: IrExpression): IrExpression {
        return irCall(androidSymbols.parcelWriteString).apply {
            arguments[0] = receiver
            arguments[1] = value
        }
    }

    fun textUtilsWriteToParcel(cs: IrExpression, p: IrExpression, parcelableFlags: IrExpression): IrExpression {
        return irCall(androidSymbols.textUtilsWriteToParcel).apply {
            arguments[0] = cs
            arguments[1] = p
            arguments[2] = parcelableFlags
        }
    }

    fun classGetClassLoader(receiver: IrExpression): IrExpression {
        return irCall(androidSymbols.classGetClassLoader).apply {
            arguments[0] = receiver
        }
    }

    fun getTextUtilsCharSequenceCreator(): IrExpression {
        return irGetField(null, androidSymbols.textUtilsCharSequenceCreator.owner)
    }

    fun unsafeCoerce(value: IrExpression, fromType: IrType, toType: IrType): IrExpression {
        return IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, toType, androidSymbols.unsafeCoerceIntrinsic).apply {
            typeArguments[0] = fromType
            typeArguments[1] = toType
            arguments[0] = value
        }
    }
}
