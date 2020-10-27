/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
import org.jetbrains.kotlin.backend.konan.getInlinedClassNative
import org.jetbrains.kotlin.backend.konan.ir.isBoxOrUnboxCall
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class RedundantCoercionsCleaner(val context: Context) : FileLoweringPass, IrElementTransformerVoid() {

    private class PossiblyFoldedExpression(val expression: IrExpression, val folded: Boolean) {
        fun getFullExpression(coercion: IrCall, cast: IrTypeOperatorCall?): IrExpression {
            if (folded) return expression
            require (coercion.dispatchReceiver == null && coercion.extensionReceiver == null) {
                "Expected either <box> or <unbox> function without any receivers"
            }
            val castedExpression =
                    if (cast == null)
                        expression
                    else with (cast) {
                        IrTypeOperatorCallImpl(startOffset, endOffset, type, operator,
                                typeOperand, expression)
                    }
            with (coercion) {
                return IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, symbol.owner.valueParameters.size, origin).apply {
                    putValueArgument(0, castedExpression)
                }
            }
        }
    }

    private val returnableBlockValues = mutableMapOf<IrReturnableBlock, MutableList<IrExpression>>()

    private fun computeReturnableBlockValues(irFile: IrFile) {
        irFile.acceptChildrenVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitContainerExpression(expression: IrContainerExpression) {
                if (expression is IrReturnableBlock)
                    returnableBlockValues[expression] = mutableListOf()

                super.visitContainerExpression(expression)
            }

            override fun visitReturn(expression: IrReturn) {
                val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
                if (returnableBlock != null)
                    returnableBlockValues[returnableBlock]!!.add(expression.value)

                super.visitReturn(expression)
            }
        })
    }

    override fun lower(irFile: IrFile) {
        computeReturnableBlockValues(irFile)
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (!expression.isBoxOrUnboxCall())
            return super.visitCall(expression)

        val argument = expression.getArguments().single().second
        val foldedArgument = fold(
                expression           = argument,
                coercion             = expression,
                cast                 = null,
                transformRecursively = true)
        return foldedArgument.getFullExpression(expression, null)
    }

    private fun IrFunction.getCoercedClass(): IrClass {
        if (name.asString().endsWith("-box>"))
            return valueParameters[0].type.classifierOrFail.owner as IrClass
        if (name.asString().endsWith("-unbox>"))
            return returnType.classifierOrFail.owner as IrClass
        error("Unexpected coercion: ${this.dump()}")
    }

    private fun IrExpression.unwrapImplicitCasts(): IrExpression {
        var expression = this
        while (expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.IMPLICIT_CAST)
            expression = expression.argument
        return expression
    }

    /**
     * TODO: JVM inliner crashed on attempt inline this function from transform.kt with:
     *  j.l.IllegalStateException: Couldn't obtain compiled function body for
     *  public inline fun <reified T : org.jetbrains.kotlin.ir.IrElement> kotlin.collections.MutableList<T>.transform...
     */
    private inline fun <reified T : IrElement> MutableList<T>.transform(transformation: (T) -> IrElement) {
        forEachIndexed { i, item ->
            set(i, transformation(item) as T)
        }
    }

    private fun fold(expression: IrExpression, coercion: IrCall,
                     cast: IrTypeOperatorCall?, transformRecursively: Boolean): PossiblyFoldedExpression {

        val transformer = this

        fun IrExpression.transformIfAsked() =
                if (transformRecursively) this.transform(transformer, data = null) else this

        fun IrElement.transformIfAsked() =
                if (transformRecursively) this.transform(transformer, data = null) else this

        val coercionDeclaringClass = coercion.symbol.owner.getCoercedClass()
        expression.unwrapImplicitCasts().let {
            if (it.isBoxOrUnboxCall()) {
                val result =
                        if (coercionDeclaringClass == (it as IrCall).symbol.owner.getCoercedClass())
                            it.getArguments().single().second
                        else expression

                return PossiblyFoldedExpression(result.transformIfAsked(), result != expression)
            }
        }
        return when (expression) {
            is IrReturnableBlock -> {
                val foldedReturnableBlockValues = returnableBlockValues[expression]!!.associate {
                    it to fold(it, coercion, cast, false)
                }
                val someoneFolded = foldedReturnableBlockValues.any { it.value.folded }
                val transformedReturnableBlock =
                        if (!someoneFolded)
                            expression
                        else {
                            val oldSymbol = expression.symbol
                            val newSymbol = IrReturnableBlockSymbolImpl(expression.descriptor)
                            val transformedReturnableBlock = with(expression) {
                                IrReturnableBlockImpl(
                                        startOffset = startOffset,
                                        endOffset = endOffset,
                                        type = coercion.type,
                                        symbol = newSymbol,
                                        origin = origin,
                                        statements = statements,
                                        inlineFunctionSymbol = inlineFunctionSymbol)
                            }
                            transformedReturnableBlock.transformChildrenVoid(object: IrElementTransformerVoid() {
                                override fun visitExpression(expression: IrExpression): IrExpression {
                                    foldedReturnableBlockValues[expression]?.let {
                                        return it.getFullExpression(coercion, cast)
                                    }
                                    return super.visitExpression(expression)
                                }

                                override fun visitReturn(expression: IrReturn): IrExpression {
                                    expression.transformChildrenVoid(this)
                                    return if (expression.returnTargetSymbol != oldSymbol)
                                        expression
                                    else with(expression) {
                                        IrReturnImpl(
                                                startOffset = startOffset,
                                                endOffset = endOffset,
                                                type = context.irBuiltIns.nothingType,
                                                returnTargetSymbol = newSymbol,
                                                value = value)
                                    }
                                }
                            })
                            transformedReturnableBlock
                        }
                if (transformRecursively)
                    transformedReturnableBlock.transformChildrenVoid(this)
                PossiblyFoldedExpression(transformedReturnableBlock, someoneFolded)
            }

            is IrBlock -> {
                val statements = expression.statements
                if (statements.isEmpty())
                    PossiblyFoldedExpression(expression, false)
                else {
                    val lastStatement = statements.last() as IrExpression
                    val foldedLastStatement = fold(lastStatement, coercion, cast, transformRecursively)
                    statements.transform {
                        if (it == lastStatement)
                            foldedLastStatement.expression
                        else
                            it.transformIfAsked()
                    }
                    val transformedBlock =
                            if (!foldedLastStatement.folded)
                                expression
                            else with(expression) {
                                IrBlockImpl(
                                        startOffset = startOffset,
                                        endOffset = endOffset,
                                        type = coercion.type,
                                        origin = origin,
                                        statements = statements)
                            }
                    PossiblyFoldedExpression(transformedBlock, foldedLastStatement.folded)
                }
            }

            is IrWhen -> {
                val foldedBranches = expression.branches.map { fold(it.result, coercion, cast, transformRecursively) }
                val someoneFolded = foldedBranches.any { it.folded }
                val transformedWhen = with(expression) {
                    IrWhenImpl(startOffset, endOffset, if (someoneFolded) coercion.type else type, origin,
                            branches.asSequence().withIndex().map { (index, branch) ->
                                IrBranchImpl(
                                        startOffset = branch.startOffset,
                                        endOffset = branch.endOffset,
                                        condition = branch.condition.transformIfAsked(),
                                        result = if (someoneFolded)
                                            foldedBranches[index].getFullExpression(coercion, cast)
                                        else foldedBranches[index].expression)
                            }.toList())
                }
                return PossiblyFoldedExpression(transformedWhen, someoneFolded)
            }

            is IrTypeOperatorCall ->
                if (expression.operator != IrTypeOperator.CAST
                        && expression.operator != IrTypeOperator.IMPLICIT_CAST
                        && expression.operator != IrTypeOperator.SAFE_CAST)
                    PossiblyFoldedExpression(expression.transformIfAsked(), false)
                else {
                    if (expression.typeOperand.getInlinedClassNative() != coercionDeclaringClass)
                        PossiblyFoldedExpression(expression.transformIfAsked(), false)
                    else {
                        val foldedArgument = fold(expression.argument, coercion, expression, transformRecursively)
                        if (foldedArgument.folded)
                            foldedArgument
                        else
                            PossiblyFoldedExpression(expression.apply { argument = foldedArgument.expression }, false)
                    }
                }

            else -> PossiblyFoldedExpression(expression.transformIfAsked(), false)
        }
    }
}
