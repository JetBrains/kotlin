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
import org.jetbrains.kotlin.js.inline.util.canHaveSideEffect
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

internal class TemporaryVariableElimination(private val root: JsStatement) {
    private val definitions = mutableMapOf<JsName, Int>()
    private val usages = mutableMapOf<JsName, Int>()
    private val definedValues = mutableMapOf<JsName, JsExpression>()
    private val temporary = mutableSetOf<JsName>()

    fun apply() {
        analyze()
        perform()
    }

    private fun analyze() {
        object : JsVisitorWithContextImpl() {
            val lastAssignedVars = mutableListOf<JsName>()

            override fun visit(x: JsNameRef, ctx: JsContext<*>): Boolean {
                val name = x.name
                if (name != null && x.qualifier == null) {
                    val expr = definedValues[name]
                    useVariable(name)
                    if (lastAssignedVars.lastOrNull() == name) {
                        lastAssignedVars.removeAt(lastAssignedVars.lastIndex)
                    }
                    else if (expr == null || expr.canHaveSideEffect()) {
                        temporary -= name
                    }
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsTry, ctx: JsContext<*>): Boolean {
                lastAssignedVars.clear()
                return super.visit(x, ctx)
            }

            override fun visit(x: JsWhile, ctx: JsContext<*>): Boolean {
                lastAssignedVars.clear()
                return super.visit(x, ctx)
            }

            override fun visit(x: JsInvocation, ctx: JsContext<*>): Boolean {
                x.arguments.asReversed().forEach { accept(it) }
                accept(x.qualifier)
                return false
            }

            override fun visit(x: JsBreak, ctx: JsContext<*>) = false

            override fun visit(x: JsContinue, ctx: JsContext<*>) = false

            override fun visit(x: JsExpressionStatement, ctx: JsContext<JsNode>): Boolean {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, value) = assignment
                    assignVariable(name, value)
                    accept(value)
                    if (x.synthetic) {
                        temporary += name
                    }
                    else if (value.canHaveSideEffect()) {
                        lastAssignedVars.clear()
                    }
                    lastAssignedVars += name
                    return false
                }

                var statement = x
                val result = accept(x.expression)
                if (result != x.expression) {
                    statement = JsExpressionStatement(result)
                    ctx.replaceMe(statement)
                }

                if (statement.expression.canHaveSideEffect()) {
                    lastAssignedVars.clear()
                }
                return false
            }

            override fun visit(x: JsBinaryOperation, ctx: JsContext<*>): Boolean {
                accept(x.arg2)
                accept(x.arg1)
                return false
            }

            override fun visit(x: JsVars, ctx: JsContext<*>): Boolean {
                for (v in x.vars) {
                    val name = v.name
                    val value = v.initExpression
                    if (value != null) {
                        assignVariable(name, value)
                        accept(value)
                        if (x.synthetic) {
                            temporary += name
                        }
                        lastAssignedVars += name
                    }
                }
                return false
            }
        }.accept(root)
    }

    private fun perform() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsExpressionStatement, ctx: JsContext<*>): Boolean {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    if (shouldConsiderTemporary(assignment.first)) {
                        ctx.removeMe()
                        return false
                    }
                }

                return super.visit(x, ctx)
            }

            override fun visit(x: JsVars, ctx: JsContext<*>): Boolean {
                val filteredVars = x.vars.filter { it.initExpression == null || !shouldConsiderTemporary(it.name) }
                x.vars.clear()
                x.vars.addAll(filteredVars)
                if (x.vars.isEmpty()) {
                    ctx.removeMe()
                }

                return super.visit(x, ctx)
            }

            override fun visit(x: JsNameRef, ctx: JsContext<JsNode>): Boolean {
                val name = x.name
                if (x.qualifier == null && name != null && shouldConsiderTemporary(name)) {
                    val newExpr = definedValues[name]!!
                    ctx.replaceMe(accept(newExpr))
                    usages[name] = usages[name]!! - 1
                    return false
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsBreak, ctx: JsContext<*>) = false

            override fun visit(x: JsContinue, ctx: JsContext<*>) = false
        }.accept(root)
    }

    private fun assignVariable(name: JsName, value: JsExpression) {
        definitions[name] = (definitions[name] ?: 0) + 1
        definedValues[name] = value
    }

    private fun useVariable(name: JsName) {
        usages[name] = (usages[name] ?: 0) + 1
    }

    private fun shouldConsiderTemporary(name: JsName): Boolean {
        if (definitions[name] ?: 0 != 1 || name !in temporary) return false

        val expr = definedValues[name]
        return (expr != null && isTrivial(expr)) || usages[name] ?: 0 == 1
    }

    private fun isTrivial(expr: JsExpression) = when (expr) {
        is JsNameRef -> expr.qualifier == null
        is JsLiteral.JsValueLiteral -> true
        else -> false
    }
}