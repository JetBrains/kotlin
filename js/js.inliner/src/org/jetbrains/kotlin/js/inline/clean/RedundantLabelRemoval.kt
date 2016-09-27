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
import com.google.dart.compiler.backend.js.ast.metadata.synthetic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

internal class RedundantLabelRemoval(private val root: JsStatement) {
    private val labelUsages = mutableMapOf<JsName, Int>()
    private var hasChanges = false

    fun apply(): Boolean {
        analyze()
        perform()
        return hasChanges
    }

    private fun analyze() {
        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsBreak, ctx: JsContext<*>) {
                super.endVisit(x, ctx)
                x.label?.let { useLabel(it.name!!) }
            }

            override fun endVisit(x: JsContinue, ctx: JsContext<*>) {
                super.endVisit(x, ctx)
                x.label?.let { useLabel(it.name!!) }
            }
        }.accept(root)
    }

    private fun perform() {
        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
                if (x.synthetic) {
                    val statementReplacement = perform(x.statement, x.name)
                    if (statementReplacement == null) {
                        hasChanges = true
                        ctx.removeMe()
                    }
                    else if (labelUsages[x.name] ?: 0 == 0) {
                        val replacement = statementReplacement
                        if (replacement is JsBlock) {
                            hasChanges = true
                            ctx.addPrevious(replacement.statements)
                            ctx.removeMe()
                        }
                        else {
                            if (replacement != ctx.currentNode) {
                                hasChanges = true
                                ctx.replaceMe(replacement)
                            }
                        }
                    }
                    else {
                        x.statement = statementReplacement
                    }
                }

                super.endVisit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<*>) = false
        }.accept(root)
    }

    private fun perform(statement: JsStatement, name: JsName): JsStatement? = when (statement) {
        is JsBreak ->
            if (name == statement.label?.name) {
                unuseLabel(name)
                null
            }
            else {
                statement
            }
        is JsLabel -> {
            perform(statement.statement, name)?.let { statement }
        }
        is JsBlock ->
            if (perform(statement.statements, name).isEmpty()) {
                null
            }
            else {
                statement
            }
        is JsIf -> {
            val thenRemoved = perform(statement.thenStatement, name) == null
            val elseStatement = statement.elseStatement
            val elseRemoved = elseStatement?.let { perform(it, name) == null } ?: false
            when {
                thenRemoved && (elseRemoved || elseStatement == null) -> {
                    hasChanges = true
                    JsAstUtils.asSyntheticStatement(statement.ifExpression)
                }
                elseRemoved -> {
                    hasChanges = true
                    statement.elseStatement = null
                    statement
                }
                thenRemoved -> {
                    hasChanges = true
                    statement.thenStatement = elseStatement ?: JsEmpty
                    statement.elseStatement = null
                    statement.ifExpression = JsAstUtils.not(statement.ifExpression)
                    statement
                }
                else -> statement
            }
        }
        is JsTry -> {
            // TODO: optimize finally and catch blocks
            val finallyBlock = statement.finallyBlock
            val result = perform(statement.tryBlock, name)
            if (result != null) {
                statement
            }
            else if (finallyBlock != null && !finallyBlock.isEmpty) {
                finallyBlock
            }
            else {
                null
            }
        }
        else -> statement
    }

    private fun perform(statements: MutableList<JsStatement>, name: JsName): MutableList<JsStatement> {
        val last = statements.lastOrNull()
        val lastOptimized = last?.let { perform(it, name) }
        if (lastOptimized != last) {
            if (lastOptimized == null) {
                hasChanges = true
                statements.removeAt(statements.lastIndex)
            }
            else if (lastOptimized is JsBlock) {
                hasChanges = true
                statements.removeAt(statements.lastIndex)
                statements.addAll(lastOptimized.statements)
            }
            else {
                if (statements[statements.lastIndex] != lastOptimized) {
                    hasChanges = true
                    statements[statements.lastIndex] = lastOptimized
                }
            }
        }
        return statements
    }

    private fun useLabel(name: JsName) {
        labelUsages[name] = (labelUsages[name] ?: 0) + 1
    }

    private fun unuseLabel(name: JsName) {
        labelUsages[name] = labelUsages[name]!! - 1
    }
}
