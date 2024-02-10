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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class Node {
    private val _children = mutableListOf<Node>()
    val children: List<Node> get() = _children

    fun addChild(node: Node) {
        _children.add(node)
    }

    fun dump(): String = buildString {
        dump(this, 0)
    }

    private fun dump(builder: StringBuilder, indent: Int) {
        builder.append("  ".repeat(indent)).append(this).appendLine()
        for (child in children) {
            child.dump(builder, indent + 1)
        }
    }
}

class AndNode : Node() {
    override fun toString() = "AndNode"
}

class OrNode : Node() {
    override fun toString() = "OrNode"
}

class ExpressionNode : Node() {
    private val _expressions = mutableListOf<IrExpression>()
    val expressions: List<IrExpression> = _expressions

    fun add(expression: IrExpression) {
        _expressions.add(expression)
    }

    override fun toString() = "ExpressionNode(${_expressions.map { it.dumpKotlinLike() }})"
}

fun buildTree(expression: IrExpression): Node? {
    class RootNode : Node() {
        override fun toString() = "RootNode"
    }

    val tree = RootNode()
    expression.accept(
        object : IrElementVisitor<Unit, Node> {
            override fun visitElement(element: IrElement, data: Node) {
                element.acceptChildren(this, data)
            }

            override fun visitExpression(expression: IrExpression, data: Node) {
                if (expression is IrFunctionExpression) return // Do not transform lambda expressions, especially their body

                val node = data as? ExpressionNode ?: ExpressionNode().also { data.addChild(it) }
                node.add(expression)
                expression.acceptChildren(this, node)
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: Node) {
                if (expression.origin == IrStatementOrigin.SAFE_CALL) {
                    // Null safe expressions can be correctly navigated
                    super.visitContainerExpression(expression, data)
                } else {
                    // Everything else is considered unsafe and terminates the expression tree
                    val node = data as? ExpressionNode ?: ExpressionNode().also { data.addChild(it) }
                    node.add(expression)
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Node) {
                val node = data as? ExpressionNode ?: ExpressionNode().also { data.addChild(it) }
                if (expression.operator in setOf(IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF)) {
                    // Only include `is` and `!is` checks
                    node.add(expression)
                }

                expression.acceptChildren(this, node)
            }

            override fun visitCall(expression: IrCall, data: Node) {
                if (expression.symbol.owner.name.asString() == "EQEQ" && expression.origin == IrStatementOrigin.EXCLEQ) {
                    // Skip the EQEQ part of a EXCLEQ call
                    expression.acceptChildren(this, data)
                } else if (expression.origin == IrStatementOrigin.NOT_IN) {
                    // Exclude the wrapped "contains" call for `!in` operator expressions and only display the final result
                    val node = data as? ExpressionNode ?: ExpressionNode().also { data.addChild(it) }
                    node.add(expression)
                    expression.dispatchReceiver!!.acceptChildren(this, node)
                } else {
                    super.visitCall(expression, data)
                }
            }

            override fun visitVararg(expression: IrVararg, data: Node) {
                // Skip processing of vararg array
                expression.acceptChildren(this, data)
            }

            override fun visitConst(expression: IrConst<*>, data: Node) {
                // Do not include constants
            }

            override fun visitWhen(expression: IrWhen, data: Node) {
                when (expression.origin) {
                    IrStatementOrigin.ANDAND -> {
                        // flatten `&&` expressions to be at the same level
                        val node = data as? AndNode ?: AndNode().also { data.addChild(it) }

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
                        val node = data as? OrNode ?: OrNode().also { data.addChild(it) }

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
                        ExpressionNode().also { data.addChild(it) }
                    }
                }
            }
        },
        tree,
    )

    return tree.children.singleOrNull()
}
