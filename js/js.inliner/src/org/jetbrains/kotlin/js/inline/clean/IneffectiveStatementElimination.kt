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

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.sideEffects
import com.google.dart.compiler.backend.js.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.collectLocalVariables
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class IneffectiveStatementElimination(private val root: JsFunction) {
    private val localVars = mutableSetOf<JsName>()
    private var hasChanges = false

    fun apply(): Boolean {
        analyze()
        process()
        return hasChanges
    }

    private fun analyze() {
        localVars += root.collectLocalVariables()
    }

    private fun process() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsExpressionStatement, ctx: JsContext<JsNode>): Boolean {
                if (x.synthetic) {
                    val replacement = replace(x.expression)
                    if (replacement.size != 1 || replacement[0] != x.expression) {
                        hasChanges = true
                        ctx.addPrevious(replacement.map { JsExpressionStatement(it).apply { synthetic = true } }.toList())
                        ctx.removeMe()
                    }
                }
                return super.visit(x, ctx)
            }
        }.accept(root.body)
    }

    private fun replace(expression: JsExpression): List<JsExpression> {
        return when (expression) {
            is JsNameRef -> {
                val qualifier = expression.qualifier
                if (qualifier == null && expression.name in localVars) {
                    listOf()
                }
                else if (!expression.sideEffects) {
                    if (qualifier != null) replace(qualifier) else listOf()
                }
                else {
                    listOf(expression)
                }
            }

            is JsUnaryOperation -> {
                when (expression.operator) {
                    JsUnaryOperator.DEC, JsUnaryOperator.INC, JsUnaryOperator.DELETE -> listOf(expression)
                    else -> replace(expression.arg)
                }
            }

            is JsBinaryOperation -> {
                if (expression.sideEffects) {
                    when (expression.operator) {
                        JsBinaryOperator.AND, JsBinaryOperator.OR -> {
                            val right = replace(expression.arg2)
                            if (right.isEmpty()) replace(expression.arg1) else listOf(expression)
                        }
                        JsBinaryOperator.INOP, JsBinaryOperator.INSTANCEOF -> listOf(expression)
                        else -> {
                            if (!expression.operator.isAssignment) {
                                replace(expression.arg1) + replace(expression.arg2)
                            }
                            else {
                                listOf(expression)
                            }
                        }
                    }
                }
                else {
                    listOf(expression)
                }
            }

            is JsInvocation -> {
                if (!expression.sideEffects) {
                    replace(expression.qualifier) + replaceMany(expression.arguments)
                }
                else {
                    listOf(expression)
                }
            }

            is JsNew -> {
                if (!expression.sideEffects) {
                    replace(expression.constructorExpression) + replaceMany(expression.arguments)
                }
                else {
                    listOf(expression)
                }
            }

            is JsConditional -> {
                val thenExpr = replace(expression.thenExpression)
                val elseExpr = replace(expression.elseExpression)
                when {
                    thenExpr.isEmpty() && elseExpr.isEmpty() -> replace(expression.testExpression)
                    thenExpr.isEmpty() -> listOf(JsAstUtils.or(expression.testExpression, expression.elseExpression))
                    elseExpr.isEmpty() -> listOf(JsAstUtils.and(expression.testExpression, expression.thenExpression))
                    else -> listOf(expression)
                }
            }

            // Although it can be suspicious case, sometimes it really helps.
            // Consider the following case: `Kotlin.modules['foo'].bar()`, where `foo` is inlineable. Expression decomposer produces
            //   var $tmp = Kotlin.modules['foo'];
            //   $tmp.bar();
            // Then, inlined body of `bar` never uses `$tmp`, therefore we can eliminate it, so `Kotlin.modules['foo']` remains.
            // It's good to eliminate such useless expression.
            is JsArrayAccess -> {
                if (!expression.sideEffects) {
                    replace(expression.arrayExpression) + replace(expression.indexExpression)
                }
                else {
                    listOf(expression)
                }
            }

            is JsLiteral.JsValueLiteral -> listOf()

            is JsArrayLiteral -> replaceMany(expression.expressions)

            is JsObjectLiteral -> expression.propertyInitializers.map { replace(it.labelExpr) + replace(it.valueExpr) }.flatten()

            is JsFunction -> if (expression.name == null) listOf() else listOf(expression)

            else -> listOf(expression)
        }
    }

    private fun replaceMany(expressions: List<JsExpression>) = expressions.map { replace(it) }.flatten()
}
