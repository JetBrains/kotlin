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

package org.jetbrains.kotlin.js.translate.utils

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.TypeCheck
import org.jetbrains.kotlin.js.backend.ast.metadata.typeCheck
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import java.util.*

fun expandIsCalls(fragments: Iterable<JsProgramFragment>) {
    val visitor = TypeCheckRewritingVisitor()
    for (fragment in fragments) {
        visitor.accept(fragment.declarationBlock)
        visitor.accept(fragment.initializerBlock)
    }
}

private class TypeCheckRewritingVisitor : JsVisitorWithContextImpl() {

    private val scopes = Stack<JsScope>()
    private val localVars = Stack<MutableSet<JsName>>().apply { push(mutableSetOf()) }

    override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
        scopes.push(x.scope)
        localVars.push(IdentitySet<JsName>().apply { this += x.parameters.map { it.name } })
        return super.visit(x, ctx)
    }

    override fun visit(x: JsVars.JsVar, ctx: JsContext<*>): Boolean {
        localVars.peek().add(x.name)
        return super.visit(x, ctx)
    }

    override fun endVisit(x: JsFunction, ctx: JsContext<*>) {
        scopes.pop()
        localVars.pop()
        super.endVisit(x, ctx)
    }

    override fun visit(x: JsInvocation, ctx: JsContext<JsNode>): Boolean {
        // callee(calleeArgument)(argument)
        val callee = x.qualifier as? JsInvocation
        val calleeArguments = callee?.arguments
        val argument = x.arguments.firstOrNull()

        if (callee != null && argument != null && calleeArguments != null) {
            val replacement = getReplacement(callee, calleeArguments, argument)

            if (replacement != null) {
                ctx.replaceMe(accept(replacement).source(x.source))
                return false
            }
        }

        return true
    }

    private fun getReplacement(callee: JsInvocation, calleeArguments: List<JsExpression>, argument: JsExpression): JsExpression? {
        val typeCheck = callee.typeCheck
        return when (typeCheck) {
            TypeCheck.TYPEOF -> {
                // `Kotlin.isTypeOf(calleeArgument)(argument)` -> `typeOf argument === calleeArgument`
                if (calleeArguments.size == 1) typeOfIs(argument, calleeArguments[0] as JsStringLiteral) else null
            }

            TypeCheck.INSTANCEOF -> {
                // `Kotlin.isInstanceOf(calleeArgument)(argument)` -> `argument instanceof calleeArgument`
                if (calleeArguments.size == 1) Namer.isInstanceOf(argument, calleeArguments[0]) else null
            }

            TypeCheck.OR_NULL -> {
                // `Kotlin.orNull(calleeArgument)(argument)` -> `(tmp = argument) == null || calleeArgument(tmp)`
                if (calleeArguments.size == 1) getReplacementForOrNull(argument, calleeArguments[0]) else null
            }

            TypeCheck.AND_PREDICATE -> {
                // `Kotlin.andPredicate(p1, p2)(argument)` -> `p1(tmp = argument) && p2(tmp)`
                if (calleeArguments.size == 2) {
                    getReplacementForAndPredicate(argument, calleeArguments[0], calleeArguments[1])
                }
                else {
                    null
                }
            }

            null -> null
        }
    }

    private fun getReplacementForOrNull(argument: JsExpression, calleeArgument: JsExpression): JsExpression {
        if (calleeArgument is JsInvocation && calleeArgument.typeCheck == TypeCheck.OR_NULL) {
            return JsInvocation(calleeArgument, argument)
        }

        val (nullCheckTarget, nextCheckTarget) = expandArgumentForTwoInvocations(argument)
        val isNull = TranslationUtils.isNullCheck(nullCheckTarget)
        return or(isNull, JsInvocation(calleeArgument, nextCheckTarget))
    }

    private fun getReplacementForAndPredicate(argument: JsExpression, p1: JsExpression, p2: JsExpression): JsExpression {
        val (arg1, arg2) = expandArgumentForTwoInvocations(argument)
        val first = accept(JsInvocation(p1, arg1) as JsExpression)
        val second = accept(JsInvocation(p2, arg2) as JsExpression)
        return JsAstUtils.and(first, second)
    }

    private fun expandArgumentForTwoInvocations(argument: JsExpression) = when {
        // `(P * Q)(localVar=someExpr)` -> `P(localVar=someExpr), Q(localVar)`
        // Where P, Q - predicate, * - function composition
        argument.isAssignmentToLocalVar -> Pair(argument, (argument as JsBinaryOperation).arg1)

        // `(P * Q)(expression)` -> `P(tmp = expression), Q(tmp)`
        argument.needsAlias -> generateAlias(argument)

        // `(P * Q)(primitive)` -> `P(primitive), Q(primitive)`
        else -> Pair(argument, argument)
    }

    private fun generateAlias(argument: JsExpression): Pair<JsExpression, JsExpression> {
        val tmp = JsScope.declareTemporary()
        val statementContext = lastStatementLevelContext
        statementContext.addPrevious(newVar(tmp, null))
        return Pair(assignment(tmp.makeRef(), argument), tmp.makeRef())
    }

    private val JsExpression.needsAlias: Boolean
        get() = when (this) {
            is JsLiteral.JsValueLiteral -> false
            else -> !isLocalVar
        }

    private val JsExpression.isLocalVar: Boolean
        get() = localVars.isNotEmpty() && this is JsNameRef && name.let { it != null && it in localVars.peek() }

    private val JsExpression.isAssignmentToLocalVar: Boolean
        get() = localVars.isNotEmpty() &&
                JsAstUtils.decomposeAssignmentToVariable(this).let { it != null && it.first in localVars.peek() }
}
