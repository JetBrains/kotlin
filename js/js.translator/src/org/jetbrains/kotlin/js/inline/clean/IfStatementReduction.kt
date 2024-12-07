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
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class IfStatementReduction(private val root: JsStatement) {
    private var hasChanges = false

    fun apply(): Boolean {
        visitor.accept(root)

        return hasChanges
    }

    val visitor = object : JsVisitorWithContextImpl() {
        override fun visit(x: JsIf, ctx: JsContext<JsNode>): Boolean {
            val thenStatementRaw = x.thenStatement
            val elseStatementRaw = x.elseStatement
            if (x.synthetic && elseStatementRaw != null) {
                val thenStatement = extractSingleStatement(thenStatementRaw)
                val elseStatement = extractSingleStatement(elseStatementRaw)

                if (thenStatement is JsExpressionStatement && elseStatement is JsExpressionStatement) {
                    val thenAssignment = JsAstUtils.decomposeAssignment(thenStatement.expression)
                    val elseAssignment = JsAstUtils.decomposeAssignment(elseStatement.expression)
                    if (thenAssignment != null && elseAssignment != null) {
                        val (thenTarget, thenValue) = thenAssignment
                        val (elseTarget, elseValue) = elseAssignment
                        if (lhsEqual(thenTarget, elseTarget)) {
                            hasChanges = true
                            val ternary = JsConditional(x.ifExpression, thenValue, elseValue)
                            val replacement = JsExpressionStatement(JsAstUtils.assignment(thenTarget, ternary))
                            replacement.synthetic = thenStatement.synthetic && elseStatement.synthetic
                            ctx.replaceMe(replacement)
                            accept(replacement)
                            return false
                        }
                    }
                }
                else if (thenStatement is JsVars && elseStatement is JsVars) {
                    if (thenStatement.vars.size == 1 && elseStatement.vars.size == 1) {
                        val thenVar = thenStatement.vars[0]
                        val elseVar = elseStatement.vars[0]
                        val thenValue = thenVar.initExpression
                        val elseValue = elseVar.initExpression
                        if (thenVar.name == elseVar.name && thenValue != null && elseValue != null) {
                            hasChanges = true
                            val ternary = JsConditional(x.ifExpression, thenValue, elseValue)
                            val replacement = JsAstUtils.newVar(thenVar.name, ternary)
                            replacement.synthetic = thenStatement.synthetic && elseStatement.synthetic
                            ctx.replaceMe(replacement)
                            accept(replacement)
                            return false
                        }
                    }
                }
                else if (thenStatement is JsReturn && elseStatement is JsReturn) {
                    val thenValue = thenStatement.expression
                    val elseValue = elseStatement.expression
                    if (thenValue != null && elseValue != null) {
                        hasChanges = true
                        val ternary = JsConditional(x.ifExpression, thenValue, elseValue)
                        val replacement = JsReturn(ternary)
                        accept(replacement)
                        ctx.replaceMe(replacement)
                        return false
                    }
                }
            }
            return super.visit(x, ctx)
        }
    }

    private fun extractSingleStatement(statement: JsStatement): JsStatement {
        var result = statement
        while (result is JsBlock && result.statements.size == 1) {
            result = result.statements[0]
        }

        return result
    }

    private fun lhsEqual(a: JsExpression?, b: JsExpression?): Boolean = when {
        a == null && b == null -> true
        a is JsNameRef && b is JsNameRef -> a.name == b.name && lhsEqual(a.qualifier, b.qualifier)
        a is JsArrayAccess && b is JsArrayAccess -> lhsEqual(a.arrayExpression, b.arrayExpression) &&
                                                    lhsEqual(a.indexExpression, b.indexExpression)
        else -> false
    }
}