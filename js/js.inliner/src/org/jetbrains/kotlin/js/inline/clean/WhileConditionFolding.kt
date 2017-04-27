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
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class WhileConditionFolding(val body: JsBlock) {
    private var changed = false

    fun apply(): Boolean {
        body.accept(object : RecursiveJsVisitor() {
            override fun visitLabel(x: JsLabel) {
                val innerStatement = x.statement
                when (innerStatement) {
                    is JsWhile -> process(innerStatement, x.name)
                    is JsDoWhile -> process(innerStatement, x.name)
                    else -> super.visitLabel(x)
                }
            }

            override fun visitWhile(x: JsWhile) {
                process(x, null)
            }

            override fun visitDoWhile(x: JsDoWhile) {
                process(x, null)
            }

            private fun process(statement: JsWhile, name: JsName?) {
                process(statement, name, { first(it) }, { removeFirst(it) }, { a, b -> JsAstUtils.and(a, b) })
            }

            private fun process(statement: JsDoWhile, name: JsName?) {
                if (!hasContinue(statement.body, name)) {
                    process(statement, name, { last(it) }, { removeLast(it) }, { a, b -> JsAstUtils.and(b, a) })
                }
            }

            private fun process(
                    statement: JsWhile,
                    name: JsName?,
                    find: (JsStatement) -> JsStatement,
                    remove: (JsStatement) -> JsStatement,
                    combine: (JsExpression, JsExpression) -> JsExpression
            ) {
                statement.body.accept(this)
                do {
                    var optimized = false
                    val first = find(statement.body)
                    val condition = extractCondition(first, name)
                    if (condition != null) {
                        statement.body = remove(statement.body)
                        val existingCondition = statement.condition
                        statement.condition = when (existingCondition) {
                            JsLiteral.TRUE -> condition
                            else -> combine(existingCondition, condition)
                        }
                        changed = true
                        optimized = true
                    }
                } while (optimized)
            }

            /**
             * Gets condition from while or do..while body and convert it into while/do..while condition
             *
             * For example,
             *
             *     while (true) {
             *         if (!A) break;
             *         B()
             *     }
             *
             * This function should return `A`, (negated condition of if statement).
             */
            private fun extractCondition(statement: JsStatement, label: JsName?): JsExpression? = when (statement) {
                // Code like this
                //
                //     while (A) {
                //         break;
                //         B();
                //     }
                //
                // can be rewritten as
                //
                //     while (A && false) {
                //         B();
                //     }
                //
                // therefore for single `break` we should return `false`.
                is JsBreak -> {
                    val target = statement.label?.name
                    if (label == target) JsLiteral.FALSE else null
                }

                // Code like this
                //
                //     while (A) {
                //         if (!B)
                //             X;
                //         D();
                //     }
                //
                // where X is a statement, and we can extract condition `C` from it, can be rewritten as
                //
                //     while (A && (B || C)) {
                //         D()
                //     }
                // therefore we return B || C
                //
                // an example is
                //
                //     while (A) {
                //         if (!B)
                //             if (!C)
                //                 break;
                //         D()
                //     }
                //
                // applying this rule repeatedly we get while (A && (B || C)), which is correct

                is JsIf -> {
                    val then = statement.thenStatement
                    if (statement.elseStatement == null) {
                        val nextCondition = extractCondition(then, label)
                        val result: JsExpression? = when (nextCondition) {
                            // Just a little optimization. When inner statement is a single `break`, `nextCondition` would be false.
                            // However, `A || false` can be rewritten as simply `A`
                            JsLiteral.FALSE -> JsAstUtils.notOptimized(statement.ifExpression)
                            null -> null
                            else -> JsAstUtils.or(JsAstUtils.notOptimized(statement.ifExpression), nextCondition)
                        }
                        result
                    }
                    else {
                        null
                    }
                }

                // A single block containing a statement with extractable condition
                is JsBlock -> {
                    if (statement.statements.size == 1) extractCondition(statement.statements[0], label) else null
                }
                else -> null
            }

            private fun first(statement: JsStatement) = when (statement) {
                is JsBlock -> statement.statements.firstOrNull() ?: statement
                else -> statement
            }

            private fun removeFirst(statement: JsStatement) = when (statement) {
                is JsBlock -> {
                    val statements = statement.statements
                    if (statements.isNotEmpty()) {
                        statements.removeAt(0)
                    }
                    statement
                }
                else -> JsBlock()
            }

            private fun last(statement: JsStatement) = when (statement) {
                is JsBlock -> statement.statements.lastOrNull() ?: statement
                else -> statement
            }

            private fun removeLast(statement: JsStatement) = when (statement) {
                is JsBlock -> {
                    val statements = statement.statements
                    if (statements.isNotEmpty()) {
                        statements.removeAt(statements.lastIndex)
                    }
                    statement
                }
                else -> JsBlock()
            }

            override fun visitFunction(x: JsFunction) { }
        })

        return changed
    }

    private fun hasContinue(statement: JsStatement, label: JsName?): Boolean {
        var found = false
        statement.accept(object : RecursiveJsVisitor() {
            private var level = 0

            override fun visitContinue(x: JsContinue) {
                val name = x.label?.name
                if (name == null) {
                    if (level == 0) {
                        found = true
                    }
                }
                else if (name == label) {
                    found = true
                }
            }

            override fun visitFor(x: JsFor) {
                level++
                super.visitFor(x)
                level--
            }

            override fun visitWhile(x: JsWhile) {
                level++
                super.visitWhile(x)
                level--
            }

            override fun visitDoWhile(x: JsDoWhile) {
                level++
                super.visitDoWhile(x)
                level--
            }

            override fun visitForIn(x: JsForIn) {
                level++
                super.visitForIn(x)
                level--
            }

            override fun visitFunction(x: JsFunction) { }

            override fun visitElement(node: JsNode) {
                if (!found) {
                    super.visitElement(node)
                }
            }
        })
        return found
    }
}