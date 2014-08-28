/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline.clean

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.hasDefaultValue

import org.jetbrains.k2js.inline.util.toIdentitySet
import org.jetbrains.k2js.inline.util.zipWithDefault
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.context.Namer.isUndefined
import org.jetbrains.k2js.translate.utils.JsAstUtils.flattenStatement

import java.util.IdentityHashMap
import java.util.Collections
import org.jetbrains.k2js.translate.utils.JsAstUtils

/**
 * Removes initializers for default parameters with defined arguments given
 * Expands initializers for default parameters with undefined arguments given
 *
 * @see isInitializer
 */
public fun removeDefaultInitializers(arguments: List<JsExpression>, parameters: List<JsParameter>, body: JsBlock) {
    val toRemove = getDefaultParamsNames(arguments, parameters, initialized = true)
    val toExpand = getDefaultParamsNames(arguments, parameters, initialized = false)

    val statements = body.getStatements()
    val newStatements = statements.flatMap {
        val name = getNameFromInitializer(it)
        if (name != null && !isNameInitialized(name, it)) {
            throw AssertionError("Unexpected initializer structure")
        }

        when {
            name in toRemove ->
                listOf<JsStatement>()
            name in toExpand ->
                flattenStatement((it as JsIf).getThenStatement()!!)
            else ->
                listOf(it)
        }
    }

    statements.clear()
    statements.addAll(newStatements)
}

private fun getNameFromInitializer(statement: JsStatement): JsName? {
    val ifStmt = (statement as? JsIf)
    val testExpr = ifStmt?.getIfExpression()

    val elseStmt = ifStmt?.getElseStatement()
    if (elseStmt == null && testExpr is JsBinaryOperation)
        return getNameFromInitializer(testExpr)

    return null
}

private fun getNameFromInitializer(isInitializedExpr: JsBinaryOperation): JsName? {
    val arg1 = isInitializedExpr.getArg1()
    val arg2 = isInitializedExpr.getArg2()
    val op = isInitializedExpr.getOperator()

    if (arg1 == null || arg2 == null || op == null) {
        return null
    }

    if (op == JsBinaryOperator.REF_EQ && isUndefined(arg2)) {
        return (arg1 as? JsNameRef)?.getName()
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
    val thenStmt = (initializer as JsIf).getThenStatement()!!
    val lastThenStmt = flattenStatement(thenStmt).last

    val expr = (lastThenStmt as? JsExpressionStatement)?.getExpression()
    if (expr !is JsBinaryOperation) return false

    val op = expr.getOperator()!!
    if (!op.isAssignment()) return false

    val arg1 = expr.getArg1()
    if (arg1 is HasName && arg1.getName() identityEquals name) return true

    return false
}

private fun getDefaultParamsNames(
    args: List<JsExpression>,
    params: List<JsParameter>,
    initialized: Boolean
): Set<JsName> {

    val argsParams = args.zipWithDefault(params, Namer.UNDEFINED_EXPRESSION)
    val relevantParams = argsParams.stream()
                                   .filter { it.second.hasDefaultValue }
                                   .filter { initialized == !isUndefined(it.first) }

    val names = relevantParams.map { it.second.getName() }
    return names.toIdentitySet()
}

