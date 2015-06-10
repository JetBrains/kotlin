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
import com.google.dart.compiler.backend.js.ast.metadata.typeCheck
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.or
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.typeOfIs
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.isNullCheck
import java.util.*

fun expandIsCalls(node: JsNode, context: TranslationContext) {
    TypeCheckRewritingVisitor(context).accept(node)
}

private class TypeCheckRewritingVisitor(private val context: TranslationContext) : JsVisitorWithContextImpl() {

    private val scopes = Stack<JsScope>()

    override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
        scopes.push(x.scope)
        return super.visit(x, ctx)
    }

    override fun endVisit(x: JsFunction, ctx: JsContext<*>) {
        scopes.pop()
        super.endVisit(x, ctx)
    }

    override fun visit(x: JsInvocation, ctx: JsContext<JsNode>): Boolean {
        // callee(calleeArgument)(argument)
        val callee = x.qualifier as? JsInvocation
        val calleeArgument = callee?.arguments?.firstOrNull()
        val argument = x.arguments.firstOrNull()

        if (callee != null && calleeArgument != null && argument != null) {
            val replacement = getReplacement(callee, calleeArgument, argument)

            if (replacement != null) {
                ctx.replaceMe(accept(replacement))
                return false
            }
        }

        return true
    }

    private fun getReplacement(callee: JsInvocation, calleeArgument: JsExpression, argument: JsExpression): JsExpression? {
        return when (callee.typeCheck) {
            // Kotlin.isTypeOf(calleeArgument)(argument) -> typeOf argument === calleeArgument
            TypeCheck.TYPEOF ->
                typeOfIs(argument, calleeArgument as JsStringLiteral)

            // Kotlin.isInstanceOf(calleeArgument)(argument) -> argument instanceof calleeArgument
            TypeCheck.INSTANCEOF ->
                context.namer().isInstanceOf(argument, calleeArgument)

            // Kotlin.orNull(calleeArgument)(argument) -> argument === null || calleeArgument(argument)
            TypeCheck.OR_NULL ->
                or(isNullCheck(argument), JsInvocation(calleeArgument, argument))

            else ->
                null
        }
    }
}
