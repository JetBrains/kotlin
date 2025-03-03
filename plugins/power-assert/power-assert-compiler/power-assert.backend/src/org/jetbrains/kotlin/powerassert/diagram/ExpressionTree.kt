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

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.evaluatedConstTrackerKey
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.powerassert.isEqualOperator
import org.jetbrains.kotlin.powerassert.isImplicitArgument
import org.jetbrains.kotlin.powerassert.isInnerOfComparisonOperator
import org.jetbrains.kotlin.powerassert.isInnerOfNotEqualOperator

sealed class Node {
    val children: List<Node>
        field = mutableListOf<Node>()

    fun addChild(node: Node) {
        children.add(node)
    }

    abstract fun isVisible(): Boolean

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

class RootNode : Node() {
    override fun isVisible(): Boolean = children.any { it.isVisible() }
    override fun toString() = "RootNode"
}

class ConstantNode(
    val expression: IrExpression,
) : Node() {
    override fun isVisible(): Boolean = false
    override fun toString() = "ConstantNode(${expression.dumpKotlinLike()})"
}

class HiddenNode(
    val expression: IrExpression,
) : Node() {
    override fun isVisible(): Boolean = false
    override fun toString() = "HiddenNode(${expression.dumpKotlinLike()})"
}

class ExpressionNode(
    val expression: IrExpression,
) : Node() {
    override fun isVisible(): Boolean = true
    override fun toString() = "ExpressionNode(${expression.dumpKotlinLike()})"
}

class ChainNode : Node() {
    override fun isVisible(): Boolean = children.any { it.isVisible() }
    override fun toString() = "ChainNode"
}

class WhenNode(
    val expression: IrWhen,
    val subject: IrVariable?,
) : Node() {
    override fun isVisible(): Boolean = true
    override fun toString() = "WhenNode(${expression.dumpKotlinLike()})"
}

class ElvisNode(
    val expression: IrExpression,
    val variable: IrVariable,
) : Node() {
    override fun isVisible(): Boolean = true
    override fun toString() = "ElvisNode(${expression.dumpKotlinLike()})"
}

fun buildTree(
    constTracker: EvaluatedConstTracker?,
    sourceFile: SourceFile,
    expression: IrExpression,
): Node? {
    fun IrConst.isEvaluatedConst(): Boolean =
        constTracker?.load(startOffset, endOffset, sourceFile.irFile.evaluatedConstTrackerKey) != null

    val tree = RootNode()
    expression.accept(
        object : IrVisitor<Unit, Node>() {
            private val calls = mutableListOf<IrCall>()
            private val whenSubjects = mutableListOf<IrVariableSymbol>()

            private fun IrFunctionExpression.isInlineParameter(): Boolean {
                val call = calls.lastOrNull() ?: return false
                val index = call.arguments.indexOf(this)
                val parameter = call.symbol.owner.parameters.getOrNull(index) ?: return false
                return parameter.isInlineParameter()
            }

            private fun IrExpression.isWhenSubjectAccess(): Boolean = this is IrGetValue && this.symbol in whenSubjects

            override fun visitElement(element: IrElement, data: Node) {
                element.acceptChildren(this, data)
            }

            override fun visitExpression(expression: IrExpression, data: Node) {
                if (expression is IrGetObjectValue) {
                    // Do not transform object access.
                    data.addChild(HiddenNode(expression))
                } else if (expression is IrFunctionExpression) {
                    if (expression.isInlineParameter()) {
                        // Completely skip this argument so it can still be inlined.
                        // This may result in the function call arguments being reordered,
                        // but this is safe for inline lambdas as they do not capture
                        // and reordering is inherent to the inlining process regardless.
                        return
                    }

                    // Do not transform lambda expressions, especially their body.
                    // But extract to a local variable so capturing behaves correctly.
                    data.addChild(HiddenNode(expression))
                } else if (expression.isImplicitArgument()) {
                    // Do not diagram implicit arguments.
                    data.addChild(HiddenNode(expression))
                } else if (expression.isWhenSubjectAccess()) {
                    // Do not diagram implicit when-subjects.
                    data.addChild(HiddenNode(expression))
                } else {
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    expression.acceptChildren(this, chainNode)
                    chainNode.addChild(ExpressionNode(expression))
                }
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: Node) {
                when (expression.origin) {
                    IrStatementOrigin.SAFE_CALL -> {
                        // Safe call operators only have their temporary variable processed
                        val statements = expression.statements
                        check(statements.size == 2) {
                            "Expected the safe call expression to consist of exactly two statements.\n${expression.dump()}"
                        }
                        val variable = statements[0] as? IrVariable
                            ?: error("Expected the first statement of the safe call expression to be a variable.\n${expression.dump()}")

                        val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                        variable.acceptChildren(this, chainNode)
                        chainNode.addChild(ExpressionNode(expression))
                    }
                    IrStatementOrigin.ELVIS -> {
                        // Elvis operators are handled with a special node
                        val statements = expression.statements
                        check(statements.size == 2) {
                            "Expected the elvis expression to consist of exactly two statements.\n${expression.dump()}"
                        }
                        val variable = statements[0] as? IrVariable
                            ?: error("Expected the first statement of the elvis expression to be a variable.\n${expression.dump()}")
                        val conditional = statements[1] as? IrWhen
                            ?: error("Expected the second statement of the elvis expression to be a when.\n${expression.dump()}")

                        val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                        variable.acceptChildren(this, chainNode)

                        val elvisNode = ElvisNode(expression, variable)
                        chainNode.addChild(elvisNode)

                        // Elvis operators need special handing for fallback value,
                        // as all other when-expression values are synthetic and either
                        // constants or repeats of expressions already processed.
                        val branches = conditional.branches
                        check(branches.size == 2) {
                            "Expected the when of the elvis expression to consist of exactly two branches.\n${expression.dump()}"
                        }
                        val nullBranch = branches[0]
                        val notNullBranch = branches[1]

                        // Make sure each branch results in 2 child nodes: condition and result.
                        val whenNode = WhenNode(conditional, null).also { elvisNode.addChild(it) }
                        whenNode.addChild(ConstantNode(nullBranch.condition)) // Constant node for the synthetic nullable condition.
                        nullBranch.result.accept(this, whenNode)
                        whenNode.addChild(ConstantNode(notNullBranch.condition)) // Constant node for the synthetic non-null condition.
                        whenNode.addChild(ConstantNode(notNullBranch.result)) // Constant node for the synthetic non-null result.

                        // Make sure elvis resulted in 4 child nodes.
                        check(whenNode.children.size == 4) {
                            "Expected the when of the elvis expression to consist of exactly two branches.\n${expression.dump()}"
                        }
                    }
                    IrStatementOrigin.WHEN -> {
                        // When-with-subject expressions are handled with a special node.
                        val statements = expression.statements
                        check(statements.size == 2) {
                            "Expected the when-with-subject expression to consist of exactly two statements.\n${expression.dump()}"
                        }
                        val variable = statements[0] as? IrVariable
                            ?: error("Expected the first statement of the when-with-subject expression to be a variable.\n${expression.dump()}")
                        val conditional = statements[1] as? IrWhen
                            ?: error("Expected the second statement of the when-with-subject expression to be a when.\n${expression.dump()}")

                        val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                        variable.acceptChildren(this, chainNode)
                        processWhen(conditional, chainNode, variable)
                    }
                    else -> {
                        // Everything else is considered unsafe and terminates the expression tree
                        data.addChild(ExpressionNode(expression))
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
                    else -> chainNode.addChild(HiddenNode(expression))
                }
            }

            override fun visitCall(expression: IrCall, data: Node) {
                if (expression.isInnerOfNotEqualOperator()) {
                    // Skip the EQEQ/EQEQEQ part of a EXCLEQ/EXCLEQEQ call
                    expression.acceptChildren(this, data)
                } else if (expression.isInnerOfComparisonOperator()) {
                    // Skip the `compareTo` part of any compareTo operator call
                    expression.acceptChildren(this, data)
                } else if (expression.origin == IrStatementOrigin.NOT_IN) {
                    // Exclude the wrapped "contains" call for `!in` operator expressions and only display the final result
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    expression.dispatchReceiver!!.acceptChildren(this, chainNode)
                    chainNode.addChild(ExpressionNode(expression))
                } else if (expression.origin == IrStatementOrigin.EXCLEXCL) {
                    // Skip displaying results of '!!' operator.
                    expression.acceptChildren(this, data)
                } else if (expression.isEqualOperator() && expression.arguments.getOrNull(0)?.isWhenSubjectAccess() == true) {
                    // Exclude equality call for when-with-subject branches.
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    expression.acceptChildren(this, chainNode)
                    chainNode.addChild(HiddenNode(expression))
                } else {
                    calls.add(expression)
                    super.visitCall(expression, data)
                    calls.removeLast()
                }
            }

            override fun visitVararg(expression: IrVararg, data: Node) {
                // Skip processing of vararg array
                expression.acceptChildren(this, data)
            }

            override fun visitConst(expression: IrConst, data: Node) {
                if (expression.isEvaluatedConst()) {
                    // Constants evaluated by the compiler should be shown as they are not explicit in the source code.
                    val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                    chainNode.addChild(ExpressionNode(expression))
                } else {
                    data.addChild(ConstantNode(expression))
                }
            }

            override fun visitTry(aTry: IrTry, data: Node) {
                // Transform try-expressions as singular expressions.
                val chainNode = data as? ChainNode ?: ChainNode().also { data.addChild(it) }
                chainNode.addChild(ExpressionNode(aTry))
            }

            override fun visitWhen(expression: IrWhen, data: Node) {
                processWhen(expression, data)
            }

            private fun processWhen(expression: IrWhen, data: Node, subject: IrVariable? = null) {
                val whenNode = WhenNode(expression, subject).also { data.addChild(it) }

                if (subject != null) whenSubjects.add(subject.symbol)
                for (branch in expression.branches) {
                    // Each branch should result in 2 child nodes: a condition and a result.
                    branch.condition.accept(this, whenNode)
                    branch.result.accept(this, whenNode)
                }
                if (subject != null) whenSubjects.removeLast()

                // Make sure each branch resulted in 2 child nodes: condition and result.
                check(whenNode.children.size == 2 * expression.branches.size) {
                    "Expected the when of the elvis expression to consist of exactly two branches.\n${expression.dump()}"
                }
            }
        },
        tree,
    )

    return tree.children.singleOrNull()?.takeIf { it !is ConstantNode }
}
