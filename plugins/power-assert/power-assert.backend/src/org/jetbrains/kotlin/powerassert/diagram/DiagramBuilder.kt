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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

fun IrBuilderWithScope.buildDiagramNesting(
    root: Node,
    variables: List<IrTemporaryVariable> = emptyList(),
    call: IrBuilderWithScope.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    return buildExpression(root, variables) { argument, subStack ->
        call(argument, subStack)
    }
}

fun IrBuilderWithScope.buildDiagramNestingNullable(
    root: Node?,
    variables: List<IrTemporaryVariable> = emptyList(),
    call: IrBuilderWithScope.(IrExpression?, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    return if (root != null) buildDiagramNesting(root, variables, call) else call(null, variables)
}

private fun IrBuilderWithScope.buildExpression(
    node: Node,
    variables: List<IrTemporaryVariable>,
    call: IrBuilderWithScope.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression = when (node) {
    is ExpressionNode -> add(node, variables, call)
    is AndNode -> nest(node, 0, variables, call)
    is OrNode -> nest(node, 0, variables, call)
    else -> TODO("Unknown node type=$node")
}

/**
 * ```
 * val result = call(1 + 2 + 3)
 * ```
 * Transforms to
 * ```
 * val result = run {
 *   val tmp0 = 1 + 2
 *   val tmp1 = tmp0 + 3
 *   call(tmp1, <diagram>)
 * }
 * ```
 */
private fun IrBuilderWithScope.add(
    node: ExpressionNode,
    variables: List<IrTemporaryVariable>,
    call: IrBuilderWithScope.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    return irBlock {
        val head = node.expressions.first().deepCopyWithSymbols(scope.getLocalDeclarationParent())
        val expressions = (buildTree(head) as ExpressionNode).expressions
        val transformer = IrTemporaryExtractionTransformer(this@irBlock, expressions.toSet())
        val transformed = expressions.first().transform(transformer, null)
        +call(transformed, variables + transformer.variables)
    }
}

/**
 * ```
 * val result = call(1 == 1 && 2 == 2)
 * ```
 * Transforms to
 * ```
 * val result = run {
 *   val tmp0 = 1 == 1
 *   if (tmp0) {
 *     val tmp1 = 2 == 2
 *     call(tmp1, <diagram>)
 *   }
 *   else call(false, <diagram>)
 * }
 * ```
 */
private fun IrBuilderWithScope.nest(
    node: AndNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrBuilderWithScope.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    val children = node.children
    val child = children[index]
    return buildExpression(child, variables) { argument, newVariables ->
        if (index + 1 == children.size) {
            call(argument, newVariables) // last expression, result is false
        } else {
            irIfThenElse(
                context.irBuiltIns.anyType,
                argument,
                nest(node, index + 1, newVariables, call), // more expressions, continue nesting
                call(irFalse(), newVariables), // short-circuit result to false
            )
        }
    }
}

/**
 * ```
 * val result = call(1 == 1 || 2 == 2)
 * ```
 * Transforms to
 * ```
 * val result = run {
 *   val tmp0 = 1 == 1
 *   if (tmp0) call(true, <diagram>)
 *   else {
 *     val tmp1 = 2 == 2
 *     call(tmp1, <diagram>)
 *   }
 * }
 * ```
 */
private fun IrBuilderWithScope.nest(
    node: OrNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrBuilderWithScope.(IrExpression, List<IrTemporaryVariable>) -> IrExpression,
): IrExpression {
    val children = node.children
    val child = children[index]
    return buildExpression(child, variables) { argument, newVariables ->
        if (index + 1 == children.size) {
            call(argument, newVariables) // last expression, result is false
        } else {
            irIfThenElse(
                context.irBuiltIns.anyType,
                argument,
                call(irTrue(), newVariables), // short-circuit result to true
                nest(node, index + 1, newVariables, call), // more expressions, continue nesting
            )
        }
    }
}
