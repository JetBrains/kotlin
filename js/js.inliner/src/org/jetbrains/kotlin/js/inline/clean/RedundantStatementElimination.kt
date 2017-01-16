/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.collectLocalVariables
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class RedundantStatementElimination(private val root: JsFunction) {
    private val localVars = root.collectLocalVariables()
    private var hasChanges = false

    fun apply(): Boolean {
        process()
        return hasChanges
    }

    private fun process() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsExpressionStatement, ctx: JsContext<JsNode>): Boolean {
                if (x.synthetic || x.expression.synthetic) {
                    val replacement = replace(x.expression)
                    if (replacement.size != 1 || replacement[0] != x.expression) {
                        hasChanges = true
                        ctx.addPrevious(replacement.map { JsExpressionStatement(it).apply { synthetic = true } })
                        ctx.removeMe()
                    }
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsBinaryOperation, ctx: JsContext<JsNode>): Boolean {
                if (x.operator == JsBinaryOperator.COMMA) {
                    val expressions = replace(x.arg1)
                    val replacement = if (expressions.isEmpty()) {
                        x.arg2
                    }
                    else {
                        JsAstUtils.newSequence(expressions + x.arg2)
                    }
                    ctx.replaceMe(replacement)
                }
                return super.visit(x, ctx)
            }
        }.accept(root.body)
    }

    private fun replace(expression: JsExpression): List<JsExpression> {
        return when (expression) {
            is JsNameRef -> {
                if (expression.name in localVars) {
                    listOf()
                }
                else if (expression.sideEffects != SideEffectKind.AFFECTS_STATE) {
                    val qualifier = expression.qualifier
                    if (qualifier != null) replace(qualifier) else listOf()
                }
                else {
                    listOf(expression)
                }
            }

            is JsUnaryOperation -> {
                when (expression.operator!!) {
                    JsUnaryOperator.BIT_NOT,
                    JsUnaryOperator.NOT,
                    JsUnaryOperator.TYPEOF,
                    JsUnaryOperator.VOID,
                    JsUnaryOperator.POS,
                    JsUnaryOperator.NEG -> replace(expression.arg)

                    JsUnaryOperator.DEC,
                    JsUnaryOperator.INC,
                    JsUnaryOperator.DELETE -> listOf(expression)
                }
            }

            is JsBinaryOperation -> {
                when (expression.operator) {
                    JsBinaryOperator.BIT_AND,
                    JsBinaryOperator.BIT_OR,
                    JsBinaryOperator.BIT_XOR,
                    JsBinaryOperator.COMMA,
                    JsBinaryOperator.ADD,
                    JsBinaryOperator.SUB,
                    JsBinaryOperator.MUL,
                    JsBinaryOperator.DIV,
                    JsBinaryOperator.MOD,
                    JsBinaryOperator.EQ,
                    JsBinaryOperator.NEQ,
                    JsBinaryOperator.REF_EQ,
                    JsBinaryOperator.REF_NEQ,
                    JsBinaryOperator.GT,
                    JsBinaryOperator.GTE,
                    JsBinaryOperator.LT,
                    JsBinaryOperator.LTE,
                    JsBinaryOperator.SHL,
                    JsBinaryOperator.SHR,
                    JsBinaryOperator.SHRU -> replace(expression.arg1) + replace(expression.arg2)

                    JsBinaryOperator.AND,
                    JsBinaryOperator.OR -> {
                        val right = replace(expression.arg2)
                        if (right.isEmpty()) replace(expression.arg1) else listOf(expression)
                    }

                    JsBinaryOperator.INOP,
                    JsBinaryOperator.INSTANCEOF -> listOf(expression)

                    JsBinaryOperator.ASG,
                    JsBinaryOperator.ASG_ADD,
                    JsBinaryOperator.ASG_SUB,
                    JsBinaryOperator.ASG_MUL,
                    JsBinaryOperator.ASG_DIV,
                    JsBinaryOperator.ASG_MOD,
                    JsBinaryOperator.ASG_BIT_AND,
                    JsBinaryOperator.ASG_BIT_OR,
                    JsBinaryOperator.ASG_BIT_XOR,
                    JsBinaryOperator.ASG_SHL,
                    JsBinaryOperator.ASG_SHR,
                    JsBinaryOperator.ASG_SHRU -> listOf(expression)
                }
            }

            is JsInvocation -> {
                if (expression.sideEffects != SideEffectKind.AFFECTS_STATE) {
                    replace(expression.qualifier) + replaceMany(expression.arguments)
                }
                else {
                    listOf(expression)
                }
            }

            is JsNew -> {
                if (expression.sideEffects != SideEffectKind.AFFECTS_STATE) {
                    replace(expression.constructorExpression) + replaceMany(expression.arguments)
                }
                else {
                    listOf(expression)
                }
            }

            is JsConditional -> {
                val thenExpr = replace(expression.thenExpression)
                val elseExpr = replace(expression.elseExpression)
                // TODO: consider case like this one: cond ? se() + 1 : se() + 2
                when {
                    thenExpr.isEmpty() && elseExpr.isEmpty() -> replace(expression.testExpression)
                    thenExpr.isEmpty() -> listOf(JsAstUtils.or(expression.testExpression, expression.elseExpression))
                    elseExpr.isEmpty() -> listOf(JsAstUtils.and(expression.testExpression, expression.thenExpression))
                    else -> listOf(expression)
                }
            }

            // Although it can be suspicious case, sometimes it really helps.
            // Consider the following case: `Kotlin.modules['foo'].bar()`, where `bar` is inlineable. Expression decomposer produces
            //   var $tmp = Kotlin.modules['foo'];
            //   $tmp.bar();
            // Then, inlined body of `bar` never uses `$tmp`, therefore we can eliminate it, so `Kotlin.modules['foo']` remains.
            // It's good to eliminate such useless expression.
            is JsArrayAccess -> {
                if (expression.sideEffects != SideEffectKind.AFFECTS_STATE) {
                    replace(expression.arrayExpression) + replace(expression.indexExpression)
                }
                else {
                    listOf(expression)
                }
            }

            is JsLiteral.JsValueLiteral -> listOf()

            is JsArrayLiteral -> replaceMany(expression.expressions)

            is JsObjectLiteral -> expression.propertyInitializers.flatMap { replace(it.labelExpr) + replace(it.valueExpr) }

            is JsFunction -> if (expression.name == null) listOf() else listOf(expression)

            else -> listOf(expression)
        }
    }

    private fun replaceMany(expressions: List<JsExpression>) = expressions.flatMap { replace(it) }
}
