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

import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dump
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

class ConstantNode(
    val expression: IrExpression,
) : Node() {
    override fun toString() = "ConstantNode(${expression.dumpKotlinLike()})"
}

class ExpressionNode(
    val expression: IrExpression,
) : Node() {
    override fun toString() = "ExpressionNode(${expression.dumpKotlinLike()})"
}

class ChainNode : Node() {
    override fun toString() = "ChainNode"
}

class WhenNode(
    val expression: IrExpression,
) : Node() {
    override fun toString() = "WhenNode(${expression.dumpKotlinLike()})"
}

class ElvisNode(
    val expression: IrExpression,
    val variable: IrVariable,
) : Node() {
    override fun toString() = "ElvisNode(${expression.dumpKotlinLike()})"
}

fun buildTree(expression: IrExpression): Node? {
    class RootNode : Node() {
        override fun toString() = "RootNode"
    }

    val tree = RootNode()
    expression.accept(
        object : IrElementVisitor<Unit, Node> {
            private var currentCall: IrCall? = null

            private fun IrExpression.isImplicitReceiverOf(irCall: IrCall): Boolean {
                val otherReceiver = when (this) {
                    irCall.dispatchReceiver -> irCall.extensionReceiver
                    irCall.extensionReceiver -> irCall.dispatchReceiver
                    else -> return false // Not a receiver of the call
                }

                // In K1, an implicit receiver will either have a zero-width offset,
                // or have the same start and end offsets as the call.
                //
                // In K2, the end offsets of the implicit receiver and the call will match,
                // but the implicit receiver may start at the beginning of an explicit receiver,
                // while the call starts at a later offset.
                //
                // The following is a generalization all of these conditions into a single check.
                return startOffset == endOffset ||
                        endOffset == irCall.endOffset && (startOffset == irCall.startOffset || otherReceiver?.startOffset == startOffset)
            }

            override fun visitElement(element: IrElement, data: Node) {
                element.acceptChildren(this, data)
            }

            override fun visitExpression(expression: IrExpression, data: Node) {
                if (expression is IrFunctionExpression) return // Do not transform lambda expressions, especially their body

                val call = currentCall
                if (call != null && expression.isImplicitReceiverOf(call)) {
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    chainNode.addChild(ConstantNode(expression)) // Do not diagram implicit receivers
                } else {
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    expression.acceptChildren(this, chainNode)
                    chainNode.addChild(ExpressionNode(expression))
                }
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: Node) {
                val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                when (expression.origin) {
                    IrStatementOrigin.SAFE_CALL -> {
                        // Safe call operators only have their temporary variable processed
                        val statements = expression.statements
                        require(statements.size == 2) {
                            "Expected the safe call expression to consist of exactly two statements.\n${expression.dump()}"
                        }
                        val variable = statements[0] as? IrVariable
                            ?: error("Expected the first statement of the safe call expression to be a variable.\n${expression.dump()}")

                        variable.acceptChildren(this, chainNode)

                        chainNode.addChild(ExpressionNode(expression))
                    }
                    IrStatementOrigin.ELVIS -> {
                        // Elvis operators are handled with a special node
                        val statements = expression.statements
                        require(statements.size == 2) {
                            "Expected the elvis expression to consist of exactly two statements.\n${expression.dump()}"
                        }
                        val variable = statements[0] as? IrVariable
                            ?: error("Expected the first statement of the elvis expression to be a variable.\n${expression.dump()}")
                        val conditional = statements[1] as? IrWhen
                            ?: error("Expected the second statement of the elvis expression to be a when.\n${expression.dump()}")

                        variable.acceptChildren(this, chainNode)

                        val elvisNode = ElvisNode(expression, variable)
                        chainNode.addChild(elvisNode)

                        // Elvis operators need special handing for fallback value,
                        // as all other when-expression values should be treated as constants
                        val branches = conditional.branches
                        require(branches.size == 2) {
                            "Expected the when of the elvis expression to consist of exactly two branches.\n${expression.dump()}"
                        }
                        val nullBranch = branches[0]
                        val notNullBranch = branches[1]

                        // Make sure each branch results in 2 child nodes: condition and result.
                        val whenNode = WhenNode(conditional).also { elvisNode.addChild(it) }
                        whenNode.addChild(ConstantNode(nullBranch.condition))
                        nullBranch.result.accept(this, whenNode)
                        whenNode.addChild(ConstantNode(notNullBranch.condition))
                        whenNode.addChild(ConstantNode(notNullBranch.result))
                    }
                    else -> {
                        // Everything else is considered unsafe and terminates the expression tree
                        chainNode.addChild(ExpressionNode(expression))
                    }
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Node) {
                val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }

                expression.acceptChildren(this, chainNode)

                when (expression.operator) {
                    // Only include `is` and `!is` checks and `as?` casts in the diagram.
                    IrTypeOperator.INSTANCEOF,
                    IrTypeOperator.NOT_INSTANCEOF,
                    IrTypeOperator.SAFE_CAST,
                        -> chainNode.addChild(ExpressionNode(expression))

                    // Do not diagram other type operations.
                    else -> chainNode.addChild(ConstantNode(expression))
                }
            }

            override fun visitCall(expression: IrCall, data: Node) {
                val isExcleq = expression.symbol.owner.name.asString() == BuiltInOperatorNames.EQEQ
                        && expression.origin == IrStatementOrigin.EXCLEQ
                val isExcleqeq = expression.symbol.owner.name.asString() == BuiltInOperatorNames.EQEQEQ
                        && expression.origin == IrStatementOrigin.EXCLEQEQ
                if (isExcleq || isExcleqeq) {
                    // Skip the EQEQ/EQEQEQ part of a EXCLEQ/EXCLEQEQ call
                    expression.acceptChildren(this, data)
                } else if (expression.origin == IrStatementOrigin.NOT_IN) {
                    // Exclude the wrapped "contains" call for `!in` operator expressions and only display the final result
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    expression.dispatchReceiver!!.acceptChildren(this, chainNode)
                    chainNode.addChild(ExpressionNode(expression))
                } else {
                    val previousCall = currentCall
                    currentCall = expression
                    super.visitCall(expression, data)
                    currentCall = previousCall
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
                val whenNode = WhenNode(expression).also { data.addChild(it) }

                for (branch in expression.branches) {
                    // Make sure each branch results in 2 child nodes: condition and result.
                    when (val child = branch.condition) {
                        is IrConst<*> -> whenNode.addChild(ConstantNode(child))
                        else -> child.accept(this, whenNode)
                    }
                    when (val child = branch.result) {
                        is IrConst<*> -> whenNode.addChild(ConstantNode(child))
                        else -> child.accept(this, whenNode)
                    }
                }
            }
        },
        tree,
    )

    return tree.children.singleOrNull()
}
