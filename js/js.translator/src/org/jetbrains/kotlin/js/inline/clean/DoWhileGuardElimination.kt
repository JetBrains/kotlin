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

/**
 * During inlining we can sometimes get the following representation for do..while statement:
 *
 *     do {
 *         guard: {
 *             // some logic
 *             break guard;
 *             // some logic
 *         }
 *     }
 *     while (condition)
 *
 * A block labeled `guard` can be eliminated with `break guard` converted to `continue`.
 */
internal class DoWhileGuardElimination(private val root: JsStatement) {
    private val guardLabels = mutableSetOf<JsName>()
    private var hasChanges = false
    private val loopGuardMap = mutableMapOf<JsDoWhile, JsLabel>()
    private val guardToLoopLabel = mutableMapOf<JsName, JsName?>()

    fun apply(): Boolean {
        analyze()
        perform()
        return hasChanges
    }

    private fun analyze() {
        object : RecursiveJsVisitor() {
            override fun visitLabel(x: JsLabel) {
                val statement = x.statement
                if (statement is JsDoWhile) {
                    processDoWhile(statement, x.name)
                }
                else {
                    super.visitLabel(x)
                }
            }

            override fun visitDoWhile(x: JsDoWhile) = processDoWhile(x, null)

            private fun processDoWhile(x: JsDoWhile, label: JsName?) {
                val body = x.body
                val guard = when (body) {
                    is JsBlock -> {
                        val firstStatement = body.statements.firstOrNull()
                        if (firstStatement is JsLabel && body.statements.size == 1) {
                            firstStatement
                        }
                        else {
                            null
                        }
                    }
                    is JsLabel -> body
                    else -> null
                }

                if (guard != null && guard.statement !is JsLoop) {

                    // When do..while loop has no label and we encounter `break guard` from nested loop, we can't
                    // replace this break with continue. Example:
                    //
                    // do {
                    //    guard: {
                    //        for (;;) {
                    //            break guard;
                    //        }
                    //    }
                    // }
                    // while (condition)
                    //
                    // In this case we get simple `continue` that goes to beginning of `for`, not `do`.
                    // We can't specify label explicitly, since there's no label on `do`.
                    // See `js-optimizer/do-while-guard-elimination/innerBreakInLoopWithoutLabel.original.js`
                    //
                    // So we don't apply optimization if `do` statement does not have label on it and there's
                    // a nested loop which has `break guard` statement.

                    if (label != null || !findBreakInNestedLoop(guard, guard.name)) {
                        guardLabels += guard.name
                        loopGuardMap[x] = guard
                        guardToLoopLabel[guard.name] = label
                    }
                }

                body.accept(this)
            }

            override fun visitFunction(x: JsFunction) { }
        }.accept(root)
    }

    private fun findBreakInNestedLoop(statement: JsStatement, name: JsName): Boolean {
        var result = false
        statement.accept(object : RecursiveJsVisitor() {
            private var loopLevel = 0

            override fun visitBreak(x: JsBreak) {
                val guardLabel = x.label?.name ?: return
                if (guardLabel == name && isInLoop()) {
                    result = true
                }
            }

            private fun isInLoop() = loopLevel > 0

            override fun visitLoop(x: JsLoop) {
                loopLevel++
                super.visitLoop(x)
                loopLevel--
            }

            override fun visitFunction(x: JsFunction) { }

            override fun visitElement(node: JsNode) {
                if (!result) {
                    super.visitElement(node)
                }
            }
        })
        return result
    }

    private fun perform() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsDoWhile, ctx: JsContext<JsNode>): Boolean {
                loopGuardMap[x]?.let { guard ->
                    if (guard.name in guardLabels) {
                        x.body = accept(guard.statement)
                        hasChanges = true
                        return false
                    }
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsBreak, ctx: JsContext<JsNode>): Boolean {
                val name = x.label?.name
                if (name in guardLabels) {
                    val target = guardToLoopLabel[name]
                    ctx.replaceMe(JsContinue(target?.makeRef()))
                    hasChanges = true
                }
                return false
            }
        }.accept(root)
    }
}
