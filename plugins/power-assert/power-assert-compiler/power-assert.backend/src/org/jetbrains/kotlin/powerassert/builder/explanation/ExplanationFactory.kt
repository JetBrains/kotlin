/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.explanation

import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.powerassert.PowerAssertBuiltIns
import org.jetbrains.kotlin.powerassert.diagram.IrTemporaryVariable
import org.jetbrains.kotlin.powerassert.sourceRange

class ExplanationFactory(
    private val builtIns: PowerAssertBuiltIns,
) {
    private fun IrBuilderWithScope.irPair(first: IrExpression, second: IrExpression): IrExpression {
        return irCall(builtIns.pairConstructor).apply {
            putValueArgument(0, first)
            putValueArgument(1, second)
        }
    }

    private fun IrBuilderWithScope.irMapOf(map: Map<String, IrExpression>): IrExpression {
        return irCall(builtIns.mapOfFunction).apply {
            val entries = map.entries.map { (key, value) -> irPair(irString(key), value) }
            val elementType = builtIns.pairType(builtIns.stringType, builtIns.valueArgumentType)
            putValueArgument(0, irVararg(elementType = elementType, values = entries))
        }
    }

    private fun IrBuilderWithScope.irListOf(list: List<IrExpression>): IrExpression {
        return irCall(builtIns.listOfFunction).apply {
            putValueArgument(0, irVararg(elementType = builtIns.expressionType, values = list))
        }
    }

    fun IrBuilderWithScope.irVariableDiagram(
        offset: Int,
        source: IrExpression,
        name: String,
        assignment: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.variableExplanationConstructor).apply {
            putValueArgument(0, irInt(offset))
            putValueArgument(1, source)
            putValueArgument(2, irString(name))
            putValueArgument(3, assignment)
        }
    }

    fun IrBuilderWithScope.irAssignment(
        startOffset: Int,
        endOffset: Int,
        expressions: List<IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.initializerConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irCallDiagram(
        offset: Int,
        source: IrExpression,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueParameters: Map<String, IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.callExplanationConstructor).apply {
            putValueArgument(0, irInt(offset))
            putValueArgument(1, source)
            putValueArgument(2, dispatchReceiver ?: irNull())
            putValueArgument(3, extensionReceiver ?: irNull())
            putValueArgument(4, irMapOf(valueParameters))
        }
    }

    fun IrBuilderWithScope.irValueParameter(
        startOffset: Int,
        endOffset: Int,
        expressions: List<IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.valueArgumentConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irReceiver(
        startOffset: Int,
        endOffset: Int,
        expressions: List<IrExpression>,
    ): IrConstructorCall {
        return irCall(builtIns.receiverConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irListOf(expressions))
        }
    }

    fun IrBuilderWithScope.irValueExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.valueExpressionConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
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
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
            putValueArgument(4, lhs)
            putValueArgument(5, rhs)
        }
    }

    fun IrBuilderWithScope.irVariableAccessExpression(
        startOffset: Int,
        endOffset: Int,
        displayOffset: Int,
        value: IrExpression,
        diagram: IrExpression,
    ): IrConstructorCall {
        return irCall(builtIns.variableAccessExpressionConstructor).apply {
            putValueArgument(0, irInt(startOffset))
            putValueArgument(1, irInt(endOffset))
            putValueArgument(2, irInt(displayOffset))
            putValueArgument(3, value)
            putValueArgument(4, diagram)
        }
    }

    fun IrBuilderWithScope.irDefaultMessage(callDiagram: IrExpression): IrCall {
        return irCall(builtIns.toDefaultMessageFunction).apply {
            extensionReceiver = callDiagram
        }
    }

    fun IrBuilderWithScope.irExpression(
        variable: IrTemporaryVariable,
        startOffset: Int,
        variableDiagrams: Map<IrVariable, IrValueDeclaration>,
    ): IrConstructorCall {
        val displayOffset = variable.toDisplayOffset()
        val sourceRange = variable.original.sourceRange
        val initializer = variable.temporary.initializer

        return when {
            variable.temporary in variableDiagrams -> {
                irVariableAccessExpression(
                    startOffset = sourceRange.start - startOffset,
                    endOffset = sourceRange.endInclusive - startOffset,
                    displayOffset = displayOffset - startOffset,
                    value = irGet(variable.temporary),
                    diagram = irGet(variableDiagrams.getValue(variable.temporary)),
                )
            }
            initializer is IrCall &&
                    initializer.symbol.owner.name.asString() == BuiltInOperatorNames.EQEQ &&
                    initializer.origin == IrStatementOrigin.EQEQ
                -> {
                val lhs = initializer.getValueArgument(0)!!
                val rhs = initializer.getValueArgument(1)!!
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
