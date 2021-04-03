/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.power.diagram

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

abstract class DiagramGenerator {
  abstract fun IrBuilderWithScope.buildCall(
    argument: IrExpression,
    variables: List<IrTemporaryVariable>
  ): IrExpression

  fun buildExpression(
    builder: IrBuilderWithScope,
    root: Node
  ): IrExpression {
    return builder.irBlock {
      buildExpression(root, listOf()) { argument, subStack ->
        buildCall(argument, subStack)
      }
    }
  }

  private fun IrStatementsBuilder<*>.buildExpression(
    node: Node,
    variables: List<IrTemporaryVariable>,
    call: IrStatementsBuilder<*>.(IrExpression, List<IrTemporaryVariable>) -> IrExpression
  ) {
    when (node) {
      is ExpressionNode -> add(node, variables, call)
      is AndNode -> nest(node, 0, variables, call)
      is OrNode -> nest(node, 0, variables, call)
      else -> TODO("Unknown node type=$node")
    }
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
  private fun IrStatementsBuilder<*>.add(
    node: ExpressionNode,
    variables: List<IrTemporaryVariable>,
    call: IrStatementsBuilder<*>.(IrExpression, List<IrTemporaryVariable>) -> IrExpression
  ) {
    val head = node.expressions.first().deepCopyWithSymbols(scope.getLocalDeclarationParent())
    val expressions = (buildTree(head) as ExpressionNode).expressions
    val transformer = IrTemporaryExtractionTransformer(this, expressions.toSet())
    val transformed = expressions.first().transform(transformer, null)

    +call(transformed, variables + transformer.variables)
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
  private fun IrStatementsBuilder<*>.nest(
    node: AndNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrStatementsBuilder<*>.(IrExpression, List<IrTemporaryVariable>) -> IrExpression
  ) {
    val children = node.children
    val child = children[index]
    buildExpression(child, variables) { argument, newVariables ->
      if (index + 1 == children.size) call(argument, newVariables) // last expression, result is false
      else irIfThenElse(
        context.irBuiltIns.anyType,
        argument,
        irBlock { nest(node, index + 1, newVariables, call) }, // more expressions, continue nesting
        call(irFalse(), newVariables), // short-circuit result to false
      )
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
  private fun IrStatementsBuilder<*>.nest(
    node: OrNode,
    index: Int,
    variables: List<IrTemporaryVariable>,
    call: IrStatementsBuilder<*>.(IrExpression, List<IrTemporaryVariable>) -> IrExpression
  ) {
    val children = node.children
    val child = children[index]
    buildExpression(child, variables) { argument, newVariables ->
      if (index + 1 == children.size) call(argument, newVariables) // last expression, result is false
      else irIfThenElse(
        context.irBuiltIns.anyType,
        argument,
        call(irTrue(), newVariables), // short-circuit result to true
        irBlock { nest(node, index + 1, newVariables, call) }, // more expressions, continue nesting
      )
    }
  }
}
