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

package org.jetbrains.k2js.inline

import com.google.dart.compiler.backend.js.ast.*

import java.util.IdentityHashMap
import java.util.Collections

/**
 * Removes initializers for default parameters with non-void0 arguments given
 * Expands initializers for default parameters with void0 arguments given
 *
 * @see isInitializer
 */
fun removeRedundantDefaultInitializers(arguments: List<JsExpression>, parameters: List<JsParameter>, body: JsBlock) {
    val toRemove = getInitializedParametersNames(arguments, parameters)
    val toExpand = getUnInitializedParametersNames(arguments, parameters)

    val statements = body.getStatements()

    val newStatements = statements.flatMap {
        when {
            !isInitializer(it) -> listOf(it)
            else -> {
                val name = getNameFromInitializer(it)
                when {
                    name in toRemove.keySet() -> listOf<JsStatement>()
                    name in toExpand.keySet() -> expand(it)
                    else -> listOf(it)
                }
            }
        }
    }.filterNotNull()

    statements.clear()
    statements.addAll(newStatements)
}

/**
 * Tests if statement is an initializer for parameter with default value.
 *
 * This check assumes that default parameter initializer is generated like:
 * if (defaultParam === void 0) {
 *     ...
 * }
 */
private fun isInitializer(statement: JsStatement): Boolean {
    return getNameFromInitializer(statement) != null
}

private fun getNameFromInitializer(statement: JsStatement): JsName? {
    val jsIf = (statement as? JsIf)
    val ifExpr = jsIf?.getIfExpression() as? JsBinaryOperation

    return when {
        jsIf?.getElseStatement() != null -> null
        else -> getNameFromInitializer(ifExpr)
    }
}

private fun getNameFromInitializer(initializerTestExpression: JsExpression?): JsName? {
    val binOp = initializerTestExpression as? JsBinaryOperation
    val arg1 = binOp?.getArg1()
    val arg2 = binOp?.getArg2()
    val operator = binOp?.getOperator()

    return when {
        operator != JsBinaryOperator.REF_EQ -> null
        !isVoid0(arg2) -> null
        else -> (arg1 as? JsNameRef)?.getName()
    }
}

private fun isVoid0(expr: JsExpression?): Boolean {
    val op = (expr as? JsUnaryOperation)?.getOperator()
    return op == JsUnaryOperator.VOID
}

private fun getInitializedParametersNames(args: List<JsExpression>,
                                          params: List<JsParameter>): Map<JsName, Boolean> {

    val names = IdentityHashMap<JsName, Boolean>()
    val argParams = (args zip params).stream()

    val initialized = argParams.filter { it.second.hasDefaultValue() }
                               .filter { !isVoid0(it.first) }
                               .map { it.second }

    initialized.map { it.getName() }
               .forEach { names.put(it, true) }

    return names
}

private fun getUnInitializedParametersNames(args: List<JsExpression>,
                                            params: List<JsParameter>): Map<JsName, Boolean> {

    val names = IdentityHashMap<JsName, Boolean>()
    val argParams = (args zip params).stream()

    val void0Params = argParams.filter { it.second.hasDefaultValue() }
                               .filter { isVoid0(it.first) }
                               .map { it.second }

    val noArgsParams = params.drop(args.size).stream()
    val uninitialized = void0Params.plus(noArgsParams)

    uninitialized.map { it.getName() }
                 .forEach { names.put(it, true) }

    return names
}

/**
 * Changes initializer check:
 *  if (arg === void 0) { arg = ... }
 * To list of statement(s):
 *  arg = ...
 */
private fun expand(initializer: JsStatement): Iterable<JsStatement> {
    val then = (initializer as? JsIf)?.getThenStatement()

    return when {
        then is JsBlock -> then.getStatements()
        then != null -> listOf(then)
        else -> listOf()
    }
}