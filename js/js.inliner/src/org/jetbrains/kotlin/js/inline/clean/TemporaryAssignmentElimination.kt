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
import org.jetbrains.kotlin.js.inline.util.collectFreeVariables
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

internal class TemporaryAssignmentElimination(private val root: JsBlock) {
    private val usageCount = mutableMapOf<JsName, Int>()
    private val assignmentCount = mutableMapOf<JsName, Int>()
    private val usages = mutableMapOf<JsName, Usage>()
    private val statementsToRemove = mutableSetOf<JsStatement>()
    private val mappedUsages = mutableMapOf<JsName, Usage>()
    private val syntheticNames = mutableSetOf<JsName>()
    private var hasChanges = false

    fun apply(): Boolean {
        analyze()
        process()
        generateDeclarations()
        return hasChanges
    }

    private fun analyze() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsReturn, ctx: JsContext<*>): Boolean {
                val returnExpr = x.expression
                if (returnExpr != null) {
                    tryRecord(returnExpr, Usage.Return(x))
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsExpressionStatement, ctx: JsContext<*>): Boolean {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, value) = assignment
                    assign(name)
                    val usage = Usage.Assignment(x, name)
                    if (x.synthetic) {
                        syntheticNames += name
                    }
                    tryRecord(value, usage)
                    accept(value)
                    return false
                }

                val mutation = JsAstUtils.decomposeAssignment(x.expression)
                if (mutation != null) {
                    val (target, value) = mutation
                    if (!target.canHaveSideEffect()) {
                        val usage = Usage.Mutation(x, target)
                        tryRecord(value, usage)
                        accept(value)
                        return false
                    }
                }

                return super.visit(x, ctx)
            }

            override fun visit(x: JsVars, ctx: JsContext<*>): Boolean {
                if (x.vars.size == 1) {
                    val declaration = x.vars[0]
                    val initExpression = declaration.initExpression
                    if (initExpression != null) {
                        tryRecord(initExpression, Usage.Declaration(x, declaration.name))
                    }
                    if (x.synthetic) {
                        syntheticNames += declaration.name
                    }
                }

                x.vars.forEach { v -> v?.initExpression?.let { accept(it) } }
                return false
            }

            override fun visit(x: JsNameRef, ctx: JsContext<*>): Boolean {
                val name = x.name
                if (name != null && x.qualifier == null) {
                    use(name)
                    return false
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
                x.collectFreeVariables().forEach { use(it); use(it) }
                return false
            }

            override fun visit(x: JsBreak, ctx: JsContext<*>) = false

            override fun visit(x: JsContinue, ctx: JsContext<*>) = false
        }.accept(root)

        usages.keys.retainAll(syntheticNames)
    }

    private fun getUsage(name: JsName): Usage? {
        return mappedUsages.getOrPut(name) {
            if (usageCount[name] ?: 0 != 1) return null

            val usage = usages[name]
            return when (usage) {
                is Usage.Assignment -> {
                    val result = getUsage(usage.target)
                    if (result != null) {
                        result.statements.addAll(usage.statements)
                        result
                    }
                    else {
                        usage
                    }
                }
                is Usage.Declaration -> {
                    val result = getUsage(usage.target)
                    if (result != null) {
                        result.statements.addAll(usage.statements)
                        result
                    }
                    else {
                        usage
                    }
                }
                else -> usage
            }
        }
    }

    private fun process() {
        usages.keys.forEach { getUsage(it) }

        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsExpressionStatement, ctx: JsContext<JsNode>): Boolean {
                if (x in statementsToRemove) {
                    hasChanges = true
                    ctx.removeMe()
                    return false
                }

                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, value) = assignment
                    val usage = getUsage(name)
                    if (usage != null) {
                        val replacement = when (usage) {
                            is Usage.Return -> JsReturn(value).source(x.expression.source)
                            is Usage.Assignment -> {
                                val expr = JsAstUtils.assignment(usage.target.makeRef(), value).source(x.expression.source)
                                val statement = JsExpressionStatement(expr)
                                statement.synthetic = usage.target in syntheticNames
                                statement
                            }
                            is Usage.Declaration -> {
                                val statement: JsStatement = if (assignmentCount[name] ?: 0 != 1) {
                                    usage.replaced = true
                                    val expr = JsAstUtils.assignment(usage.target.makeRef(), value).source(x.expression.source)
                                    val result = JsExpressionStatement(expr)
                                    result.synthetic = usage.target in syntheticNames
                                    result
                                } else {
                                    val declaration = JsAstUtils.newVar(usage.target, value)
                                    declaration.source(x.expression.source)
                                    declaration.synthetic = usage.target in syntheticNames
                                    declaration
                                }
                                statement
                            }
                            is Usage.Mutation -> {
                                JsExpressionStatement(JsAstUtils.assignment(usage.target, value).source(x.expression.source))
                            }
                        }
                        hasChanges = true
                        ctx.replaceMe(replacement)
                        statementsToRemove += usage.statements
                        return false;
                    }
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsReturn, ctx: JsContext<*>): Boolean {
                if (x in statementsToRemove) {
                    hasChanges = true
                    ctx.removeMe()
                    return false
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsVars, ctx: JsContext<*>): Boolean {
                if (x in statementsToRemove) {
                    hasChanges = true
                    ctx.removeMe()
                    return false
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<*>) = false
        }.accept(root)
    }

    private fun generateDeclarations() {
        var index = 0
        usages.values.asSequence()
                .filter { it is Usage.Declaration && it.replaced }
                .map { it as Usage.Declaration }
                .forEach { root.statements.add(index++, JsAstUtils.newVar(it.target, null)) }
    }

    private fun tryRecord(expr: JsExpression, usage: Usage): Boolean {
        if (expr !is JsNameRef) return false
        val name = expr.name ?: return false

        usages[name] = usage
        return true
    }

    private fun use(name: JsName) {
        usageCount[name] = 1 + (usageCount[name] ?: 0)
    }

    private fun assign(name: JsName) {
        assignmentCount[name] = 1 + (assignmentCount[name] ?: 0)
    }

    private sealed class Usage(statement: JsStatement) {
        val statements = mutableSetOf(statement)

        class Return(statement: JsStatement) : Usage(statement)

        class Assignment(statement: JsStatement, val target: JsName) : Usage(statement)

        class Declaration(statement: JsStatement, val target: JsName) : Usage(statement) {
            var replaced = false
        }

        class Mutation(statement: JsStatement, val target: JsExpression) : Usage(statement)
    }
}