/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.hasUsages
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

interface Condition {
    fun asExpression(reformat: Boolean): KtExpression
    fun asNegatedExpression(reformat: Boolean): KtExpression
    fun toAtomicConditions(): List<AtomicCondition>

    companion object {
        fun create(expression: KtExpression, negated: Boolean = false): Condition {
            if (negated) {
                if (expression is KtBinaryExpression && expression.operationToken == KtTokens.OROR) {
                    //TODO: check Boolean type for operands
                    val left = expression.left
                    val right = expression.right
                    if (left != null && right != null) {
                        val leftCondition = create(left, negated = true)
                        val rightCondition = create(right, negated = true)
                        return CompositeCondition.create(leftCondition.toAtomicConditions() + rightCondition.toAtomicConditions())
                    }
                }
            } else {
                if (expression is KtBinaryExpression && expression.operationToken == KtTokens.ANDAND) {
                    //TODO: check Boolean type for operands
                    val left = expression.left
                    val right = expression.right
                    if (left != null && right != null) {
                        val leftCondition = create(left)
                        val rightCondition = create(right)
                        return CompositeCondition.create(leftCondition.toAtomicConditions() + rightCondition.toAtomicConditions())
                    }
                }
            }
            return AtomicCondition(expression, negated)
        }
    }
}

class AtomicCondition(val expression: KtExpression, private val isNegated: Boolean = false) : Condition {
    init {
        assert(expression.isPhysical)
    }

    override fun asExpression(reformat: Boolean) = if (isNegated) expression.negate(reformat) else expression
    override fun asNegatedExpression(reformat: Boolean) = if (isNegated) expression else expression.negate(reformat)
    override fun toAtomicConditions() = listOf(this)

    fun negate() = AtomicCondition(expression, !isNegated)
}

class CompositeCondition private constructor(val conditions: List<AtomicCondition>) : Condition {
    override fun asExpression(reformat: Boolean): KtExpression {
        val factory = KtPsiFactory(conditions.first().expression)
        return factory.buildExpression(reformat = reformat) {
            for ((index, condition) in conditions.withIndex()) {
                if (index > 0) {
                    appendFixedText("&&")
                }
                appendExpression(condition.asExpression(reformat))
            }
        }
    }

    override fun asNegatedExpression(reformat: Boolean): KtExpression {
        return asExpression(reformat).negate()
    }

    override fun toAtomicConditions() = conditions

    companion object {
        fun create(conditions: List<AtomicCondition>): Condition {
            return conditions.singleOrNull() ?: CompositeCondition(conditions)
        }
    }
}

fun Condition.hasUsagesOf(variable: KtCallableDeclaration): Boolean {
    return toAtomicConditions().any { variable.hasUsages(it.expression) }
}

