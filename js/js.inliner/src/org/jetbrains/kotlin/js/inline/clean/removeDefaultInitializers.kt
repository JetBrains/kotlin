/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.js.backend.ast.metadata.hasDefaultValue
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.inline.util.zipWithDefault
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.decomposeAssignmentToVariable
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.flattenStatement

/**
 * Removes initializers for default parameters with defined arguments given
 * Expands initializers for default parameters with undefined arguments given
 */
fun removeDefaultInitializers(arguments: List<JsExpression>, parameters: List<JsParameter>, body: JsBlock) {
    val toRemove = getDefaultParamsNames(arguments, parameters, initialized = true)
    val toExpand = getDefaultParamsNames(arguments, parameters, initialized = false)

    val statements = body.statements
    val newStatements = statements.flatMap {
        val name = getNameFromInitializer(it)
        if (name != null && !isNameInitialized(name, it)) {
            throw AssertionError("Unexpected initializer structure")
        }

        when {
            name != null && name in toRemove ->
                listOf<JsStatement>()
            name != null && name in toExpand -> {
                val thenStatement = (it as JsIf).thenStatement
                markAssignmentAsStaticRef(name, thenStatement)
                flattenStatement(thenStatement)
            }
            else ->
                listOf(it)
        }
    }

    statements.clear()
    statements.addAll(newStatements)
}

private fun markAssignmentAsStaticRef(name: JsName, node: JsNode) {
    node.accept(object : RecursiveJsVisitor() {
        override fun visitBinaryExpression(x: JsBinaryOperation) {
            decomposeAssignmentToVariable(x)?.let { (assignmentTarget, assignmentExpr) ->
                if (assignmentTarget == name) {
                    assignmentTarget.staticRef = assignmentExpr
                }
            }
            super.visitBinaryExpression(x)
        }
    })
}

private fun getNameFromInitializer(statement: JsStatement): JsName? {
    val ifStmt = (statement as? JsIf)
    val testExpr = ifStmt?.ifExpression

    val elseStmt = ifStmt?.elseStatement
    if (elseStmt == null && testExpr is JsBinaryOperation)
        return getNameFromInitializer(testExpr)

    return null
}

private fun getNameFromInitializer(isInitializedExpr: JsBinaryOperation): JsName? {
    val arg1 = isInitializedExpr.arg1
    val arg2 = isInitializedExpr.arg2
    val op = isInitializedExpr.operator

    if (arg1 == null || arg2 == null) {
        return null
    }

    if (op == JsBinaryOperator.REF_EQ && JsAstUtils.isUndefinedExpression(arg2)) {
        return (arg1 as? JsNameRef)?.name
    }

    return null
}

/**
 * Tests if the last statement of initializer
 * is name assignment.
 */
private fun isNameInitialized(
    name: JsName,
    initializer: JsStatement
): Boolean {
    val thenStmt = (initializer as JsIf).thenStatement
    val lastThenStmt = flattenStatement(thenStmt).last()

    val expr = (lastThenStmt as? JsExpressionStatement)?.expression
    if (expr !is JsBinaryOperation) return false

    val op = expr.operator
    if (!op.isAssignment) return false

    val arg1 = expr.arg1
    if (arg1 is HasName && arg1.name === name) return true

    return false
}

private fun getDefaultParamsNames(
    args: List<JsExpression>,
    params: List<JsParameter>,
    initialized: Boolean
): Set<JsName> {

    val argsParams = args.zipWithDefault(params, Namer.getUndefinedExpression())
    val relevantParams = argsParams.asSequence()
                                   .filter { it.second.hasDefaultValue }
                                   .filter { initialized == !JsAstUtils.isUndefinedExpression(it.first) }

    val names = relevantParams.map { it.second.name }
    return names.toSet()
}

