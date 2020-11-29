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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

sealed class Node {
  abstract val parent: Node?
  val mutableChildren: MutableList<Node> = mutableListOf()
  val children: List<Node> get() = mutableChildren

  protected fun dump(builder: StringBuilder, indent: Int) {
    builder.append("  ".repeat(indent)).append(this).appendLine()
    for (child in children) {
      child.dump(builder, indent + 1)
    }
  }
}

class AndNode(override val parent: Node) : Node() {
  init {
    parent.mutableChildren.add(this)
  }

  override fun toString() = "AndNode"
}

class OrNode(override val parent: Node) : Node() {
  init {
    parent.mutableChildren.add(this)
  }

  override fun toString() = "OrNode"
}

class ExpressionNode(
  override val parent: Node
) : Node() {
  init {
    parent.mutableChildren.add(this)
  }

  private val _expressions: MutableList<IrExpression> = mutableListOf()

  fun add(expression: IrExpression) {
    _expressions.add(expression)
  }

  fun getExpressionsCopy(initialParent: IrDeclarationParent?): List<IrExpression> {
    // Return a copy of all the expression by creating a deep copy of the head
    // expression and running back through the assertion tree builder
    val headCopy = _expressions.first().deepCopyWithSymbols(initialParent)
    return (buildAssertTree(headCopy).children.single() as ExpressionNode)._expressions
  }

  override fun toString() = "ExpressionNode($_expressions)"
}

class RootNode : Node() {
  override val parent: Node? = null

  fun dump(): String = buildString {
    dump(this, 0)
  }

  override fun toString() = "RootNode"
}

fun buildAssertTree(expression: IrExpression): RootNode {
  val tree = RootNode()
  expression.accept(object : IrElementVisitor<Unit, Node> {
    val INCREMENT_DECREMENT_OPERATORS = setOf(
      IrStatementOrigin.PREFIX_INCR,
      IrStatementOrigin.PREFIX_DECR,
      IrStatementOrigin.POSTFIX_INCR,
      IrStatementOrigin.POSTFIX_DECR
    )

    override fun visitElement(element: IrElement, data: Node) {
      element.acceptChildren(this, data)
    }

    override fun visitExpression(expression: IrExpression, data: Node) {
      val node = data as? ExpressionNode ?: ExpressionNode(data)
      node.add(expression)
      expression.acceptChildren(this, node)
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: Node) {
      if (expression.origin in INCREMENT_DECREMENT_OPERATORS) {
        val node = data as? ExpressionNode ?: ExpressionNode(data)
        node.add(expression)
        return // Skip the internals of increment/decrement operations
      }

      super.visitContainerExpression(expression, data)
    }

    override fun visitCall(expression: IrCall, data: Node) {
      if (expression.symbol.owner.name.asString() == "EQEQ" && expression.origin == IrStatementOrigin.EXCLEQ) {
        // Skip the EQEQ part of a EXCLEQ call
        expression.acceptChildren(this, data)
      } else {
        super.visitCall(expression, data)
      }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Node) {
      // Do not include constants
    }

    override fun visitWhen(expression: IrWhen, data: Node) {
      when (expression.origin) {
        IrStatementOrigin.ANDAND -> {
          // flatten `&&` expressions to be at the same level
          val node = data as? AndNode ?: AndNode(data)

          require(expression.branches.size == 2)
          val thenBranch = expression.branches[0]

          thenBranch.condition.accept(this, node)
          thenBranch.result.accept(this, node)

          val elseBranchCondition = expression.branches[1].condition
          val elseBranchResult = expression.branches[1].result

          if (elseBranchCondition !is IrConst<*> || elseBranchCondition.value != true) {
            elseBranchCondition.accept(this, node)
          }

          if (elseBranchResult !is IrConst<*> || elseBranchResult.value != false) {
            elseBranchResult.accept(this, node)
          }
        }
        IrStatementOrigin.OROR -> {
          // flatten `||` expressions to be at the same level
          val node = data as? OrNode ?: OrNode(data)

          require(expression.branches.size == 2)
          val thenBranchCondition = expression.branches[0].condition
          val thenBranchResult = expression.branches[0].result
          val elseBranchCondition = expression.branches[1].condition
          val elseBranchResult = expression.branches[1].result

          thenBranchCondition.accept(this, node)

          if (thenBranchResult !is IrConst<*> || thenBranchResult.value != true) {
            thenBranchResult.accept(this, node)
          }

          if (elseBranchCondition !is IrConst<*> || elseBranchCondition.value != true) {
            elseBranchCondition.accept(this, node)
          }

          if (elseBranchResult !is IrConst<*> || elseBranchResult.value != false) {
            elseBranchResult.accept(this, node)
          }
        }
        else -> {
          // Add as basic expression and terminate
          // TODO this has to be broken and not work in all cases...
          ExpressionNode(data)
        }
      }
    }
  }, tree)

  return tree
}
