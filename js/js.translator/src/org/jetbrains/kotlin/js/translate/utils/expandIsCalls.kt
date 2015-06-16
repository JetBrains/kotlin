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

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.TypeCheck
import com.google.dart.compiler.backend.js.ast.metadata.isCastExpression
import com.google.dart.compiler.backend.js.ast.metadata.typeCheck
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import java.util.*

fun expandIsCalls(node: JsNode, context: TranslationContext) {
    TypeCheckRewritingVisitor(context).accept(node)
}

private class TypeCheckRewritingVisitor(private val context: TranslationContext) : JsVisitorWithContextImpl() {

    private val scopes = Stack<JsScope>()
    private val localVars = Stack<MutableSet<JsName>>()

    override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
        scopes.push(x.scope)
        localVars.push(IdentitySet())
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

    override fun endVisit(x: JsConditional, ctx: JsContext<JsNode>) {
        val test = x.testExpression

        if (x.isCastExpression &&
            test is JsBinaryOperation &&
            test.operator == JsBinaryOperator.ASG
        ) {
            ctx.replaceMe(test.arg2)
        }
    }

    override fun visit(x: JsInvocation, ctx: JsContext<JsNode>): Boolean {
        // callee(calleeArgument)(argument)
        val callee = x.qualifier as? JsInvocation
        val calleeArgument = callee?.arguments?.firstOrNull()
        val argument = x.arguments.firstOrNull()

        if (callee != null && argument != null) {
            val replacement = getReplacement(callee, calleeArgument, argument)

            if (replacement != null) {
                ctx.replaceMe(accept(replacement))
                return false
            }
        }

        return true
    }

    private fun getReplacement(callee: JsInvocation, calleeArgument: JsExpression?, argument: JsExpression): JsExpression? {
        if (calleeArgument == null) {
            // Kotlin.isAny()(argument) -> argument != null
            if (callee.typeCheck == TypeCheck.IS_ANY) {
                return TranslationUtils.isNotNullCheck(argument)
            }

            return null
        }

        // Kotlin.isTypeOf(calleeArgument)(argument) -> typeOf argument === calleeArgument
        if (callee.typeCheck == TypeCheck.TYPEOF) {
            return typeOfIs(argument, calleeArgument as JsStringLiteral)
        }

        // Kotlin.isInstanceOf(calleeArgument)(argument) -> argument instanceof calleeArgument
        if (callee.typeCheck == TypeCheck.INSTANCEOF) {
            return context.namer().isInstanceOf(argument, calleeArgument)
        }

        // Kotlin.orNull(calleeArgument)(argument) -> (tmp = argument) == null || calleeArgument(tmp)
        if (callee.typeCheck == TypeCheck.OR_NULL) {
            if (calleeArgument is JsInvocation) {
                if (calleeArgument.typeCheck == TypeCheck.OR_NULL) return JsInvocation(calleeArgument, argument)

                if (calleeArgument.typeCheck == TypeCheck.IS_ANY) return argument
            }

            var nullCheckTarget = argument
            var nextCheckTarget = argument

            if (argument.isAssignmentToLocalVar) {
                // Kotlin.orNull(Kotlin.isInstance(SomeType))(localVar=someExpr) -> (localVar=someExpr) != null || Kotlin.isInstance(SomeType)(localVar)
                val localVar = (argument as JsBinaryOperation).getArg1()
                nextCheckTarget = localVar
            }
            else if (!argument.isLocalVar) {
                val currentScope = scopes.peek()
                val tmp = currentScope.declareTemporary()
                val statementContext = lastStatementLevelContext
                statementContext.addPrevious(newVar(tmp, null))
                nullCheckTarget = assignment(tmp.makeRef(), argument)
                nextCheckTarget = tmp.makeRef()
            }

            val isNull = TranslationUtils.isNullCheck(nullCheckTarget)
            return or(isNull, JsInvocation(calleeArgument, nextCheckTarget))
        }

        return null
    }

    private val JsExpression.isLocalVar: Boolean
        get() {
            if (localVars.empty() || this !is JsNameRef) return false

            val name = this.getName()
            return name != null && localVars.peek().contains(name)
        }

    private val JsExpression.isAssignmentToLocalVar: Boolean
        get() = this is JsBinaryOperation && getOperator() == JsBinaryOperator.ASG
}
