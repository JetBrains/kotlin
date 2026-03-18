/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.parameter

import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.powerassert.PowerAssertBuiltIns
import org.jetbrains.kotlin.powerassert.diagram.IrDiagramVariable
import org.jetbrains.kotlin.powerassert.diagram.toDisplayOffset
import org.jetbrains.kotlin.powerassert.sourceRange

class ExplanationFactory(
    private val builtIns: PowerAssertBuiltIns,
) {
    private fun IrBuilderWithScope.irListOf(type: IrType, list: List<IrExpression>): IrExpression {
        return irCall(builtIns.listOfFunction).apply {
            typeArguments[0] = type
            arguments[0] = irVararg(elementType = type, values = list)
        }
    }

    fun IrBuilderWithScope.irCallExplanation(
        offset: Int,
        source: IrExpression,
        arguments: List<IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.callExplanationConstructor).apply {
            this.arguments[0] = irInt(offset)
            this.arguments[1] = source
            this.arguments[2] = irListOf(builtIns.argumentType, arguments)
        }
    }

    fun IrBuilderWithScope.irArgument(
        startOffset: Int,
        endOffset: Int,
        kind: IrParameterKind,
        expressions: List<IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.argumentConstructor).apply {
            arguments[0] = irInt(startOffset)
            arguments[1] = irInt(endOffset)
            arguments[2] = irKind(kind)
            arguments[3] = irListOf(builtIns.expressionType, expressions)
        }
    }

    private fun IrBuilderWithScope.irKind(
        kind: IrParameterKind,
    ): IrGetEnumValue {
        val entries = builtIns.argumentKindClass.owner.declarations
            .filterIsInstance<IrEnumEntry>()
        val entry = when (kind) {
            IrParameterKind.DispatchReceiver -> entries.single { it.name.toString() == "DISPATCH" }
            IrParameterKind.Context -> entries.single { it.name.toString() == "CONTEXT" }
            IrParameterKind.ExtensionReceiver -> entries.single { it.name.toString() == "EXTENSION" }
            IrParameterKind.Regular -> entries.single { it.name.toString() == "VALUE" }
        }
        return IrGetEnumValueImpl(startOffset, endOffset, builtIns.argumentKindType, entry.symbol)
    }

    fun IrBuilderWithScope.irValueExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.valueExpressionConstructor).apply {
            arguments[0] = irInt(startOffset)
            arguments[1] = irInt(endOffset)
            arguments[2] = irInt(displayOffset)
            arguments[3] = value
        }
    }

    fun IrBuilderWithScope.irLiteralExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.literalExpressionConstructor).apply {
            arguments[0] = irInt(startOffset)
            arguments[1] = irInt(endOffset)
            arguments[2] = irInt(displayOffset)
            arguments[3] = value
        }
    }

    fun IrBuilderWithScope.irEqualityExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
        lhs: IrExpression,
        rhs: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.equalityExpressionConstructor).apply {
            arguments[0] = irInt(startOffset)
            arguments[1] = irInt(endOffset)
            arguments[2] = irInt(displayOffset)
            arguments[3] = value
            arguments[4] = lhs
            arguments[5] = rhs
        }
    }

    fun IrBuilderWithScope.irDefaultMessage(callDiagram: IrExpression): IrCall {
        return irCall(builtIns.toDefaultMessageFunction).apply {
            arguments[0] = callDiagram
        }
    }

    fun IrBuilderWithScope.irExpression(
        variable: IrDiagramVariable.Displayable,
        startOffset: Int,
    ): IrConstructorCall {
        val displayOffset = variable.toDisplayOffset()
        val sourceRange = variable.original.sourceRange
        val initializer = variable.temporary.initializer

        return when {
            variable.literal -> {
                irLiteralExpression(
                    startOffset = sourceRange.start - startOffset,
                    endOffset = sourceRange.endInclusive - startOffset,
                    displayOffset = displayOffset - startOffset,
                    value = irGet(variable.temporary),
                )
            }
            initializer is IrCall &&
                    initializer.symbol.owner.name.asString() == BuiltInOperatorNames.EQEQ &&
                    initializer.origin == IrStatementOrigin.EQEQ
                -> {
                val lhs = initializer.arguments[0]!!
                val rhs = initializer.arguments[1]!!
                irEqualityExpression(
                    startOffset = sourceRange.start - startOffset,
                    endOffset = sourceRange.endInclusive - startOffset,
                    displayOffset = displayOffset - startOffset,
                    value = irGet(variable.temporary),
                    lhs = lhs.deepCopyWithSymbols(),
                    rhs = rhs.deepCopyWithSymbols(),
                )
            }
            else -> {
                irValueExpression(
                    startOffset = sourceRange.start - startOffset,
                    endOffset = sourceRange.endInclusive - startOffset,
                    displayOffset = displayOffset - startOffset,
                    value = irGet(variable.temporary),
                )
            }
        }
    }
}
