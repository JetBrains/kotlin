/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert.diagram

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun IrBuilderWithScope.buildDiagramNesting(
    sourceFile: SourceFile,
    root: Node,
    variables: List<IrTemporaryVariable> = emptyList(),
    call: IrBuilderWithScope.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    return irBlock {
        +buildExpression(sourceFile, root, variables) { argument, subStack ->
            call(argument, subStack)
        }
    }
}

fun IrBuilderWithScope.buildDiagramNestingNullable(
    sourceFile: SourceFile,
    root: Node?,
    variables: List<IrTemporaryVariable> = emptyList(),
    call: IrBuilderWithScope.(IrExpression?, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    return if (root != null) buildDiagramNesting(sourceFile, root, variables, call) else call(null, variables)
}

private fun IrBlockBuilder.buildExpression(
    sourceFile: SourceFile,
    node: Node,
    variables: List<IrTemporaryVariable>,
    call: IrBlockBuilder.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression = when (node) {
    is ConstantNode -> add(node, variables, call)
    is ExpressionNode -> add(sourceFile, node, variables, call)
    is ChainNode -> nest(sourceFile, node, 0, variables, call)
    is WhenNode -> nest(sourceFile, node, 0, variables, call)
    is ElvisNode -> nest(sourceFile, node, 0, variables, call)
    else -> TODO("Unknown node type=$node")
}

/**
 * ```
 * val result = call(a)
 * ```
 *
 * Should be transformed into:
 *
 * ```
 * val result = call(a)
 * ```
 */
private fun IrBlockBuilder.add(
    node: ConstantNode,
    variables: List<IrTemporaryVariable>,
    call: IrBlockBuilder.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    val expression = node.expression
    val transformer = IrTemporaryExtractionTransformer(this@add, variables)
    val copy = expression.deepCopyWithSymbols(scope.getLocalDeclarationParent()).transform(transformer, null)
    return call(copy, variables)
}

/**
 * ```
 * val result = call(a)
 * ```
 *
 * Should be transformed into:
 *
 * ```
 * val tmp0 = a
 * val result = call(tmp0, <diagram of tmp0>)
 * ```
 */
private fun IrBlockBuilder.add(
    sourceFile: SourceFile,
    node: ExpressionNode,
    variables: List<IrTemporaryVariable>,
    call: IrBlockBuilder.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    val expression = node.expression
    val sourceRangeInfo = sourceFile.getSourceRangeInfo(expression)
    val text = sourceFile.getText(sourceRangeInfo)

    val transformer = IrTemporaryExtractionTransformer(this@add, variables)
    val copy = expression.deepCopyWithSymbols(scope.getLocalDeclarationParent()).transform(transformer, null)

    val variable = irTemporary(copy, nameHint = "PowerAssertSynthesized")
    val newVariables = variables + IrTemporaryVariable(variable, expression, sourceRangeInfo, text)
    return call(irGet(variable), newVariables)
}

/**
 * ```
 * val result = call(a.b.c)
 * ```
 *
 * Should be transformed into:
 *
 * ```
 * val result = run {
 *   val tmp0 = a
 *   val tmp1 = tmp0.b
 *   val tmp2 = tmp1.c
 *   call(tmp2, <diagram of tmp0 + tmp1 + tmp2>)
 * }
 * ```
 */
private fun IrBlockBuilder.nest(
    sourceFile: SourceFile,
    node: ChainNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrBlockBuilder.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    val children = node.children
    val child = children[index]
    return buildExpression(sourceFile, child, variables) { argument, argumentVariables ->
        if (index + 1 == children.size) {
            call(argument, argumentVariables)
        } else {
            nest(sourceFile, node, index + 1, argumentVariables, call)
        }
    }
}

/**
 * ```
 * val result = call(if (condition1) result1 else if (condition2) result2 else result3)
 * ```
 *
 * Should be transformed into:
 *
 * ```
 * val result = run {
 *   val tmp0 = condition1
 *   if (tmp0) {
 *     val tmp1 = result1
 *     call(tmp1, <diagram of tmp0 + tmp1>)
 *   } else {
 *     val tmp2 = condition2
 *     if (tmp2) {
 *       val tmp3 = result2
 *       call(tmp3, <diagram of tmp0 + tmp2 + tmp3>)
 *     } else {
 *       val tmp4 = result3
 *       call(tmp4, <diagram of tmp0 + tmp2 + tmp4>)
 *     }
 *   }
 * }
 * ```
 */
private fun IrBlockBuilder.nest(
    sourceFile: SourceFile,
    node: WhenNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrBlockBuilder.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    class BranchOptimizer(private val branchIndex: Int) : IrElementTransformerVoid() {
        override fun visitWhen(expression: IrWhen): IrExpression {
            if (expression.attributeOwnerId == node.expression) {
                val branches = expression.branches
                if (branches.size <= branchIndex) return expression

                /**
                 * Transforming a call with a nested when-expression into a when-expression with nested calls will leave copies of the
                 * original nested when-expression which have a known result based on the surrounding when-expression. These copies can be
                 * optimized by replacing them with the result of the known branch.
                 *
                 * ```
                 * val tmp1: Boolean = ...
                 * when {
                 *   tmp1 -> { // BLOCK
                 *     val tmp2: Int = value1
                 *
                 *     // Replace this when with just 'tmp2'
                 *     when {
                 *       tmp1 -> tmp2
                 *       else -> ...
                 *     }
                 *
                 *     ...
                 * ```
                 */

                return branches[branchIndex].result
            }
            return super.visitWhen(expression)
        }
    }

    val children = node.children
    val conditionNode = children[index]
    val resultNode = children[index + 1]
    return buildExpression(sourceFile, conditionNode, variables) { condition, conditionVariables ->
        if (index + 2 == children.size) {
            buildExpression(sourceFile, resultNode, conditionVariables) { result, resultVariables ->
                call(result, resultVariables)
            }
        } else {
            irIfThenElse(
                context.irBuiltIns.anyType,
                condition,
                irBlock {
                    +buildExpression(sourceFile, resultNode, conditionVariables) { result, resultVariables ->
                        call(result, resultVariables)
                    }
                }.transform(BranchOptimizer(index / 2), null),
                irBlock {
                    +nest(sourceFile, node, index + 2, conditionVariables, call)
                }.transform(BranchOptimizer(index / 2 + 1), null),
            )
        }
    }
}

/**
 * ```
 * val result = call(a?.b ?: c)
 * ```
 *
 * Should be transformed into:
 *
 * ```
 * val result = run {
 *   val tmp0 = a
 *   val tmp1 = tmp0?.b
 *   if (tmp1 == null) {
 *     val tmp2 = c
 *     call(tmp2, <diagram of tmp0 + tmp1 + tmp2>)
 *   } else {
 *     call(tmp1, <diagram of tmp0 + tmp1>)
 *   }
 * }
 * ```
 */
private fun IrBlockBuilder.nest(
    sourceFile: SourceFile,
    node: ElvisNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrBlockBuilder.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    class ElvisOptimizer : IrElementTransformerVoid() {
        private val transformer = IrTemporaryExtractionTransformer(this@nest, variables)
        private val initializer = node.variable.initializer?.transform(transformer, null)

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            if (initializer != null && expression.symbol == node.variable.symbol) {
                /**
                 * When the when-expression of an elvis-expression is converted, the branch condition will still reference the temporary
                 * variable of the original elvis-expression. This must be replaced with the temporary variable generated by power-assert
                 * from the initializer of the elvis-expression.
                 */
                return initializer.deepCopyWithSymbols(scope.getLocalDeclarationParent())
            }

            return super.visitGetValue(expression)
        }

        override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
            if (expression.attributeOwnerId == node.expression) {
                val statements = expression.statements
                if (statements.size != 2) return expression
                val variable = statements[0] as? IrVariable ?: return expression
                val conditional = statements[1] as? IrGetValue ?: return expression

                return if (conditional.symbol == variable.symbol) {
                    /**
                     * Elvis-expressions which look like the following, can be replaced by the variable initializer.
                     * ```
                     * { // BLOCK
                     *   val tmp = value1
                     *   return@BLOCK tmp
                     * }
                     * ```
                     */
                    variable.initializer!!
                } else {
                    /**
                     * Elvis-expressions which look like the following, can be replaced by the get value expression.
                     * ```
                     * { // BLOCK
                     *   val tmp = value1
                     *   return@BLOCK value2
                     * }
                     * ```
                     */
                    conditional
                }
            }
            return super.visitContainerExpression(expression)
        }
    }

    val children = node.children
    val child = children[index]
    val result = buildExpression(sourceFile, child, variables) { argument, argumentVariables ->
        if (index + 1 == children.size) {
            call(argument, argumentVariables)
        } else {
            nest(sourceFile, node, index + 1, argumentVariables, call)
        }
    }

    if (index == 0) {
        result.transform(ElvisOptimizer(), null)
    }

    return result
}
