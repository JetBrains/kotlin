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

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

abstract class DiagramGenerator {
  abstract fun IrBuilderWithScope.buildAssertThrow(variables: List<IrTemporaryVariable>): IrExpression

  fun buildAssert(
    builder: IrBuilderWithScope,
    root: Node
  ): IrExpression {
    return builder.irBlock {
      buildAssert(root, listOf()) { subStack ->
        buildAssertThrow(subStack)
      }
    }
  }

  private fun IrStatementsBuilder<*>.buildAssert(
    node: Node,
    variables: List<IrTemporaryVariable>,
    thenPart: IrStatementsBuilder<*>.(List<IrTemporaryVariable>) -> IrExpression
  ): List<IrTemporaryVariable> = when (node) {
    is ExpressionNode -> add(node, variables, thenPart)
    is AndNode -> chain(node, variables, thenPart)
    is OrNode -> nest(node, 0, variables)
    else -> TODO("Unknown node type=$node")
  }

  /**
   * TODO - this is how the following function should behave
   * ```
   * val result = call(1 + 2 + 3)
   * ```
   * Transformed to
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
    thenPart: IrStatementsBuilder<*>.(List<IrTemporaryVariable>) -> IrExpression
  ): List<IrTemporaryVariable> {
    val head = node.expressions.first().deepCopyWithSymbols(scope.getLocalDeclarationParent())
    val expressions = (buildTree(head) as ExpressionNode).expressions
    val transformer = IrTemporaryExtractionTransformer(this, expressions.toSet())
    val transformed = expressions.first().transform(transformer, null)

    if (transformed.type == context.irBuiltIns.booleanType) {
      // TODO irIfThenElse to support property nesting of ANDs & ORs
      +irIfThen(irNot(transformed), thenPart(variables + transformer.variables))
    } else {
      +thenPart(variables + transformer.variables)
    }

    return transformer.variables
  }

  /**
   * TODO - this is how the following function should behave
   * ```
   * val result = call(1 == 1 && 2 == 2)
   * ```
   * Transformed to
   * ```
   * val result = run {
   *   val tmp0 = 1 == 1
   *   if (tmp0) {
   *     val tmp1 = 2 == 2
   *     if (tmp1) call(true, <diagram>)
   *     else call(false, <diagram>)
   *   }
   *   else call(false, <diagram>)
   * }
   * ```
   */
  private fun IrStatementsBuilder<*>.chain(
    node: AndNode,
    variables: List<IrTemporaryVariable>,
    thenPart: IrStatementsBuilder<*>.(List<IrTemporaryVariable>) -> IrExpression
  ): List<IrTemporaryVariable> {
    val newVariables = mutableListOf<IrTemporaryVariable>()
    for (child in node.children) {
      newVariables.addAll(buildAssert(child, variables + newVariables, thenPart))
    }
    return newVariables
  }

  /**
   * TODO - this is how the following function should behave
   * ```
   * val result = call(1 == 1 || 2 == 2)
   * ```
   * Transformed to
   * ```
   * val result = run {
   *   val tmp0 = 1 == 1
   *   if (tmp0) call(true, <diagram>)
   *   else {
   *     val tmp1 = 2 == 2
   *     if (tmp1) call(true, <diagram>)
   *     else call(false, <diagram>)
   *   }
   * }
   * ```
   */
  private fun IrStatementsBuilder<*>.nest(
    node: OrNode,
    index: Int,
    variables: List<IrTemporaryVariable>
  ): List<IrTemporaryVariable> {
    val children = node.children
    val child = children[index]
    buildAssert(child, variables) { newVariables ->
      if (index + 1 == children.size) buildAssertThrow(newVariables)
      else irBlock { nest(node, index + 1, newVariables) }
    }
    return emptyList()
  }
}
