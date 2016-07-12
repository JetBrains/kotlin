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
import org.jetbrains.kotlin.js.inline.util.collectDefinedNames
import org.jetbrains.kotlin.js.inline.util.collectFreeVariables
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

internal class TemporaryAssignmentElimination(private val root: JsBlock) {
    private val usageCount = mutableMapOf<JsName, Int>()
    private val usages = mutableMapOf<JsName, Usage>()
    private val statementsToRemove = mutableSetOf<JsStatement>()
    private val mappedUsages = mutableMapOf<JsName, UsageHolder?>()
    private val syntheticNames = mutableSetOf<JsName>()
    private var hasChanges = false
    private val namesToProcess = mutableSetOf<JsName>()

    fun apply(): Boolean {
        analyze()
        calculateDeclarations()
        process()
        generateDeclarations()
        return hasChanges
    }

    private fun analyze() {
        namesToProcess.addAll(collectDefinedNames(root))

        object : RecursiveJsVisitor() {
            override fun visitReturn(x: JsReturn) {
                val returnExpr = x.expression
                if (returnExpr != null) {
                    tryRecord(returnExpr, Usage.Return(x))
                }
                super.visitReturn(x)
            }

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val variableAssignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (variableAssignment != null) {
                    val (name, value) = variableAssignment
                    val usage = Usage.VariableAssignment(x, name)
                    if (x.synthetic) {
                        syntheticNames += name
                    }
                    tryRecord(value, usage)
                    accept(value)
                    // Don't visit LHS, since it's already treated as a temporary variable
                    return
                }

                val propertyMutation = JsAstUtils.decomposeAssignment(x.expression)
                if (propertyMutation != null) {
                    val (target, value) = propertyMutation
                    if (!target.canHaveSideEffect(namesToProcess)) {
                        val usage = Usage.PropertyMutation(x, target)
                        tryRecord(value, usage)
                        accept(value)
                        return
                    }
                }

                return super.visitExpressionStatement(x)
            }

            override fun visitVars(x: JsVars) {
                // TODO: generalize for multiple declarations per one statement
                if (x.vars.size == 1) {
                    val declaration = x.vars[0]
                    val initExpression = declaration.initExpression
                    if (initExpression != null) {
                        tryRecord(initExpression, Usage.VariableDeclaration(x, declaration.name))
                    }
                    // TODO: generalize for case when single JsVar is synthetic
                    if (x.synthetic) {
                        syntheticNames += declaration.name
                    }
                }

                x.vars.forEach { v -> v?.initExpression?.let { accept(it) } }
            }

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                if (name != null && nameRef.qualifier == null) {
                    use(name)
                    return
                }
                return super.visitNameRef(nameRef)
            }

            override fun visitFunction(x: JsFunction) {
                // Don't visit function body, but mark its free variables as used two times, so that further stages won't treat
                // these variables as temporary.
                x.collectFreeVariables().forEach { use(it); use(it) }
            }

            override fun visitBreak(x: JsBreak) { }

            override fun visitContinue(x: JsContinue) { }
        }.accept(root)

        usages.keys.retainAll(syntheticNames)
    }

    private fun getUsage(name: JsName): UsageHolder? {
        return mappedUsages.getOrPut(name) {
            if (usageCount[name] != 1) return null

            val usage = usages[name]
            val mappedUsage: UsageHolder? = when (usage) {
                is Usage.VariableAssignment -> {
                    val result = getUsage(usage.target)
                    if (result != null) {
                        UsageHolder(result.value, usage.statement, result)
                    }
                    else {
                        UsageHolder(usage, usage.statement, null)
                    }
                }
                is Usage.VariableDeclaration -> {
                    val result = getUsage(usage.target)
                    if (result != null) {
                        UsageHolder(result.value, usage.statement, result)
                    }
                    else {
                        UsageHolder(usage, usage.statement, null)
                    }
                }
                null -> null
                else -> UsageHolder(usage, usage.statement, null)
            }

            mappedUsage
        }
    }

    private fun calculateDeclarations() {
        usages.keys.forEach { getUsage(it) }

        object : RecursiveJsVisitor() {
            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val usage = getUsage(assignment.first)?.value
                    if (usage is Usage.VariableDeclaration) {
                        usage.count++
                    }
                }
                super.visitExpressionStatement(x)
            }
        }.accept(root)
    }

    private fun process() {
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
                    val usageHolder = getUsage(name)
                    if (usageHolder != null) {
                        val usage = usageHolder.value
                        val replacement = when (usage) {
                            is Usage.Return -> JsReturn(value).apply { source(x.expression.source) }
                            is Usage.VariableAssignment -> {
                                val expr = JsAstUtils.assignment(usage.target.makeRef(), value).source(x.expression.source)
                                val statement = JsExpressionStatement(expr)
                                statement.synthetic = usage.target in syntheticNames
                                statement
                            }
                            is Usage.VariableDeclaration -> {
                                val statement: JsStatement = if (usage.count > 1) {
                                    val expr = JsAstUtils.assignment(usage.target.makeRef(), value).source(x.expression.source)
                                    val result = JsExpressionStatement(expr)
                                    result.synthetic = usage.target in syntheticNames
                                    result
                                }
                                else {
                                    val declaration = JsAstUtils.newVar(usage.target, value)
                                    declaration.source(x.expression.source)
                                    declaration.synthetic = usage.target in syntheticNames
                                    declaration
                                }
                                statement
                            }
                            is Usage.PropertyMutation -> {
                                JsExpressionStatement(JsAstUtils.assignment(usage.target, value).source(x.expression.source))
                            }
                        }
                        hasChanges = true
                        ctx.replaceMe(replacement)
                        statementsToRemove += usageHolder.collectStatements()
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
                .filter { it is Usage.VariableDeclaration && it.count > 1 }
                .map { it as Usage.VariableDeclaration }
                .forEach {
                    val statement = JsAstUtils.newVar(it.target, null)
                    statement.synthetic = it.target in syntheticNames
                    root.statements.add(index++, statement)
                }
    }

    private fun tryRecord(expr: JsExpression, usage: Usage): Boolean {
        if (expr !is JsNameRef) return false
        val name = expr.name ?: return false
        if (name !in namesToProcess) return false

        usages[name] = usage
        return true
    }

    private fun use(name: JsName) {
        usageCount[name] = 1 + (usageCount[name] ?: 0)
    }

    private sealed class Usage(val statement: JsStatement) {
        class Return(statement: JsStatement) : Usage(statement)

        class VariableAssignment(statement: JsStatement, val target: JsName) : Usage(statement)

        class VariableDeclaration(statement: JsStatement, val target: JsName) : Usage(statement) {
            var count = 0
        }

        class PropertyMutation(statement: JsStatement, val target: JsExpression) : Usage(statement)
    }

    private class UsageHolder(val value: Usage, val statement: JsStatement, val next: UsageHolder?) {
        fun collectStatements() = generateSequence (this) { it.next }.map { it.statement }
    }
}