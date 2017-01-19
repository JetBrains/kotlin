/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

internal class EmptyStatementElimination(private val root: JsStatement) {
    private var hasChanges = false

    fun apply(): Boolean {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsFunction, ctx: JsContext<*>) = false

            override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
                if (x.synthetic) {
                    if (isEmpty(x.statement)) {
                        ctx.replaceMe(x.statement)
                        hasChanges = true
                    }
                }
            }

            override fun endVisit(x: JsBlock, ctx: JsContext<*>) {
                processStatements(x.statements)
            }

            override fun endVisit(x: JsIf, ctx: JsContext<JsNode>) {
                val thenEmpty = isEmpty(x.thenStatement)
                val elseEmpty = x.elseStatement?.let { isEmpty(it) } ?: true
                when {
                    thenEmpty && elseEmpty -> {
                        hasChanges = true
                        ctx.replaceMe(JsAstUtils.asSyntheticStatement(x.ifExpression))
                    }
                    elseEmpty -> {
                        if (x.elseStatement != null) {
                            hasChanges = true
                            x.elseStatement = null
                        }
                    }
                    thenEmpty -> {
                        hasChanges = true
                        x.thenStatement = x.elseStatement!!
                        x.elseStatement = null
                        x.ifExpression = JsAstUtils.notOptimized(x.ifExpression)
                    }
                }
            }

            override fun endVisit(x: JsTry, ctx: JsContext<JsNode>) {
                val finallyBlock = x.finallyBlock
                if (x.tryBlock.isEmpty) {
                    hasChanges = true
                    ctx.replaceMe(finallyBlock ?: JsEmpty)
                }
            }

            override fun endVisit(x: JsSwitch, ctx: JsContext<JsNode>) {
                for (case in x.cases) {
                    processStatements(case.statements)
                }
                if (x.cases.dropLast(1).none { it is JsDefault } && x.cases.dropLast(1).all { it.statements.isEmpty() }) {
                    hasChanges = true
                    val conditionStatement = JsAstUtils.asSyntheticStatement(x.expression)
                    ctx.replaceMe(JsBlock(listOf(conditionStatement) + x.cases.last().statements))
                }
            }

            private fun processStatements(statements: MutableList<JsStatement>) {
                for ((index, statement) in statements.withIndex().reversed()) {
                    if (statement is JsEmpty) {
                        statements.removeAt(index)
                        hasChanges = true
                    }
                    else if (statement is JsBlock) {
                        statements.removeAt(index)
                        statements.addAll(index, statement.statements)
                    }
                }
            }

            private fun isEmpty(statement: JsStatement) = statement is JsBlock && statement.isEmpty || statement is JsEmpty
        }.accept(root)
        return hasChanges
    }
}