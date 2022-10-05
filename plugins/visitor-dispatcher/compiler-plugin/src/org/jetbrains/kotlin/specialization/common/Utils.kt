/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.common

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.substitute

fun <T> List<List<T>>.cartesianProduct(): Sequence<List<T>> {
    return sequence {
        if (isEmpty()) {
            yield(emptyList())
            return@sequence
        }

        val head = get(0)
        for (x in head) {
            for (tail in drop(1).cartesianProduct()) {
                yield(listOf(x) + tail)
            }
        }
    }
}

fun IrFunction.isMonomorphic(): Boolean {
    return typeParameters.any {
        it.hasAnnotation(FqnUtils.MONOMORPHIC_ANNOTATION_FQN)
    }
}

fun IrCall.shallowCopy(newSymbol: IrSimpleFunctionSymbol? = null, transformValueArgument: (IrExpression) -> IrExpression): IrCall {
    return IrCallImpl(
        startOffset, endOffset,
        type,
        newSymbol ?: symbol,
        typeArgumentsCount,
        valueArgumentsCount,
        origin,
        superQualifierSymbol
    ).also { copy ->
        for (i in 0 until typeArgumentsCount) {
            copy.putTypeArgument(i, getTypeArgument(i))
        }
        for (i in 0 until valueArgumentsCount) {
            copy.putValueArgument(i, getValueArgument(i)?.let { transformValueArgument(it) })
        }
        copy.attributeOwnerId = attributeOwnerId
        copy.dispatchReceiver = dispatchReceiver
        copy.extensionReceiver = extensionReceiver
    }
}

fun IrCall.shallowCopy(newSymbol: IrSimpleFunctionSymbol?) = shallowCopy(newSymbol) { it }