/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.isBoxOrUnboxCall
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.*

/*
 * The algorithm tries to find and remove matching pairs of box/unbox calls.
 *
 * - The leaf points are either a call to a matching box/unbox function or a constant primitive with appropriate type:
 *   box<T>(unbox<T>(value)) --> value
 *   unbox<T>(const<Any>(value)) --> const<T>(value)
 *
 * - In case of a when clause or a returnable block, the algorithm tries to propagate the coercion to subvalues if possible:
 *   box<T>(when {
 *       predicate1 -> unbox<T>(value1)
 *       predicate2 -> value2
 *   }) --> when {
 *       predicate1 -> value1
 *       predicate2 -> box<T>(value2)
 *   }
 *   box<T>(block {
 *     ..
 *     return@block unbox<T>(value1)
 *     ..
 *     return@block value2
 *   }) --> block {
 *     ..
 *     return@block value1
 *     ..
 *     return@block box<T>(value2)
 *   }
 *
 *   - There might be also some casts in between matching box/unbox pair, the algorithm assumes that in the correct IR
 *     such casts are redundant and eliminates them.
 */
internal class RedundantCoercionsCleaner(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(transformer, null)
    }

    private class TransformerState(val coercion: IrCall) {
        var folded = false
        val casts = mutableListOf<IrTypeOperatorCall>()

        fun copy() = TransformerState(coercion).also {
            it.folded = folded
            it.casts.addAll(casts)
        }

        fun applyCoercion(expression: IrExpression): IrExpression {
            var result = expression
            for (i in casts.size - 1 downTo 0) {
                val cast = casts[i]
                result = with(cast) {
                    IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, typeOperand, result)
                }
            }
            return with(coercion) {
                IrCallImpl(
                        startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, origin
                ).apply {
                    putValueArgument(0, result)
                }
            }
        }
    }

    private fun IrFunction.getCoercedClass(): IrClass {
        if (name.asString().endsWith("-box>"))
            return valueParameters[0].type.classifierOrFail.owner as IrClass
        if (name.asString().endsWith("-unbox>"))
            return returnType.classifierOrFail.owner as IrClass
        error("Unexpected coercion: ${this.render()}")
    }

    private fun IrTypeOperator.isCast() =
            this == IrTypeOperator.CAST || this == IrTypeOperator.IMPLICIT_CAST || this == IrTypeOperator.SAFE_CAST

    private data class PossiblyFoldedExpression(val expression: IrExpression, val folded: Boolean)

    private val transformer = object : IrElementTransformer<TransformerState?> {
        override fun visitElement(element: IrElement, data: TransformerState?) = super.visitElement(element, null)
        override fun visitDeclaration(declaration: IrDeclarationBase, data: TransformerState?) = super.visitDeclaration(declaration, null)
        override fun visitExpression(expression: IrExpression, data: TransformerState?) = super.visitExpression(expression, null)
        override fun visitConstantValue(expression: IrConstantValue, data: TransformerState?) = super.visitConstantValue(expression, null)
        override fun visitBranch(branch: IrBranch, data: TransformerState?) = super.visitBranch(branch, null)
        override fun visitCatch(aCatch: IrCatch, data: TransformerState?) = super.visitCatch(aCatch, null)

        override fun visitCall(expression: IrCall, data: TransformerState?): IrElement {
            if (!expression.isBoxOrUnboxCall())
                return super.visitCall(expression, null)

            val argument = expression.getValueArgument(0)!!
            return if (expression.symbol.owner.getCoercedClass() == data?.coercion?.symbol?.owner?.getCoercedClass()) {
                data.folded = true
                argument.transform(this, null)
            } else {
                val state = TransformerState(expression)
                val result = argument.transform(this, state)
                if (state.folded)
                    result
                else
                    expression.also { it.putValueArgument(0, result) }
            }
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: TransformerState?): IrExpression {
            if (!expression.operator.isCast())
                return super.visitTypeOperator(expression, null)

            data?.casts?.push(expression)
            val argument = expression.argument.transform(this, data)
            data?.casts?.pop()
            return if (data?.folded == true)
                argument
            else expression.also { it.argument = argument }
        }

        override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: TransformerState?): IrConstantValue {
            if (expression.value.type == data?.coercion?.type) {
                data.folded = true
                expression.type = expression.value.type
            }
            return expression
        }

        override fun visitWhen(expression: IrWhen, data: TransformerState?): IrExpression {
            if (data == null)
                return super.visitWhen(expression, null)

            val branchResults = expression.branches.map { branch ->
                branch.condition = branch.condition.transform(this, null)
                val result = branch.result.transform(this, data)
                val folded = data.folded
                data.folded = false
                PossiblyFoldedExpression(result, folded)
            }
            if (branchResults.all { !it.folded }) {
                branchResults.forEachIndexed { index, branchResult ->
                    expression.branches[index].result = branchResult.expression
                }
            } else {
                branchResults.forEachIndexed { index, branchResult ->
                    expression.branches[index].result = if (branchResult.folded)
                        branchResult.expression
                    else
                        data.applyCoercion(branchResult.expression)
                }
                expression.type = data.coercion.type
                data.folded = true
            }
            return expression
        }

        val returnableBlockStates = mutableMapOf<IrReturnableBlock, TransformerState>()
        val foldedReturnableBlocks = mutableSetOf<IrReturnableBlock>()
        val foldedReturns = mutableSetOf<IrReturn>()

        override fun visitReturn(expression: IrReturn, data: TransformerState?): IrExpression {
            val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
            return if (returnableBlock == null)
                super.visitReturn(expression, data)
            else {
                val state = returnableBlockStates[returnableBlock]?.copy()
                expression.value = expression.value.transform(this, state)
                if (state?.folded == true) {
                    foldedReturnableBlocks.add(returnableBlock)
                    foldedReturns.add(expression)
                }
                expression
            }
        }

        override fun visitBlock(expression: IrBlock, data: TransformerState?): IrExpression {
            if (data == null)
                return super.visitBlock(expression, null)

            val returnableBlock = expression as? IrReturnableBlock
            if (returnableBlock != null)
                returnableBlockStates[returnableBlock] = data
            val statements = expression.statements
            for (i in statements.indices) {
                val state = data.takeIf { i == statements.lastIndex && returnableBlock == null }
                statements[i] = statements[i].transform(this, state) as IrStatement
            }
            if (returnableBlock in foldedReturnableBlocks) {
                expression.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        val value = expression.value.transform(this, null)
                        expression.value = if (expression.returnTargetSymbol.owner == returnableBlock && expression !in foldedReturns)
                            data.applyCoercion(value)
                        else value
                        return expression
                    }
                })
                data.folded = true
            }
            if (data.folded)
                expression.type = data.coercion.type
            return expression
        }
    }
}
