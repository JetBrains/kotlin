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

package com.bnorm.power

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

abstract class PowerAssertGenerator {
  abstract fun IrBuilderWithScope.buildAssertThrow(subStack: List<IrStackVariable>): IrExpression

  fun buildAssert(
    builder: IrBuilderWithScope,
    root: Node
  ): IrExpression {
    return builder.irBlock {
      buildAssert(root, mutableListOf()) { subStack ->
        buildAssertThrow(subStack)
      }
    }
  }

  private fun IrStatementsBuilder<*>.buildAssert(
    node: Node,
    stack: MutableList<IrStackVariable>,
    thenPart: IrStatementsBuilder<*>.(stack: MutableList<IrStackVariable>) -> IrExpression
  ) {
    fun IrStatementsBuilder<*>.nest(children: List<Node>, index: Int, stack: MutableList<IrStackVariable>) {
      val child = children[index]
      buildAssert(child, stack) { subStack ->
        if (index + 1 == children.size) buildAssertThrow(subStack)
        else irBlock { nest(children, index + 1, subStack) }
      }
    }

    when (node) {
      is ExpressionNode -> {
        +irIfNotThan(stack, node, thenPart)
      }
      is AndNode -> {
        for (child in node.children) {
          buildAssert(child, stack, thenPart)
        }
      }
      is OrNode -> {
        nest(node.children, 0, stack)
      }
    }
  }

  private inline fun IrStatementsBuilder<*>.irIfNotThan(
    stack: MutableList<IrStackVariable>,
    node: ExpressionNode,
    thenPart: IrStatementsBuilder<*>.(subStack: MutableList<IrStackVariable>) -> IrExpression
  ): IrWhen {
    val expressions = node.getExpressionsCopy(this.parent)
    val stackTransformer = StackBuilder(this, stack, expressions)
    val transformed = expressions.first().transform(stackTransformer, null)
    return irIfThen(irNot(transformed), thenPart(stack.toMutableList()))
  }

  class StackBuilder(
    private val builder: IrStatementsBuilder<*>,
    private val stack: MutableList<IrStackVariable>,
    private val transform: List<IrExpression>
  ) : IrElementTransformerVoid() {

    override fun visitExpression(expression: IrExpression): IrExpression {
      return if (expression in transform) {
        with(builder) {
          val copy = expression.deepCopyWithSymbols(scope.getLocalDeclarationParent())
          val variable = irTemporary(super.visitExpression(expression))
          stack.add(IrStackVariable(variable, copy))
          irGet(variable)
        }
      } else {
        super.visitExpression(expression)
      }
    }
  }
}
