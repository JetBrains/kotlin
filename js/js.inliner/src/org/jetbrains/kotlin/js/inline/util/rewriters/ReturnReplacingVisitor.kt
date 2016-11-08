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

package org.jetbrains.kotlin.js.inline.util.rewriters

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.*
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class ReturnReplacingVisitor(
        private val resultRef: JsNameRef?,
        private val breakLabel: JsNameRef?,
        private val function: JsFunction,
        private val isSuspend: Boolean
) : JsVisitorWithContextImpl() {

    /**
     * Prevents replacing returns in object literal
     */
    override fun visit(x: JsObjectLiteral, ctx: JsContext<JsNode>): Boolean = false

    /**
     * Prevents replacing returns in inner function
     */
    override fun visit(x: JsFunction, ctx: JsContext<JsNode>): Boolean = false

    override fun endVisit(x: JsReturn, ctx: JsContext<JsNode>) {
        if (x.returnTarget != null && function.functionDescriptor != x.returnTarget) return

        ctx.removeMe()

        val returnReplacement = getReturnReplacement(x.expression)
        if (returnReplacement != null) {
            ctx.addNext(JsExpressionStatement(returnReplacement).apply { synthetic = true })
        }

        if (breakLabel != null) {
            ctx.addNext(JsBreak(breakLabel))
        }
    }

    private fun getReturnReplacement(returnExpression: JsExpression?): JsExpression? {
        return if (returnExpression != null) {
            val assignment = resultRef?.let { lhs ->
                val rhs = makeFakeSuspendCall(returnExpression)!!
                JsAstUtils.assignment(lhs, rhs).apply { synthetic = true }
            }
            assignment ?: makeFakeSuspendCall(returnExpression)
        }
        else {
            makeFakeSuspendCall(null)
        }
    }

    fun makeFakeSuspendCall(expression: JsExpression?): JsExpression? {
        if (!isSuspend) return expression

        val fakeSuspendCall = JsInvocation(JsAstUtils.pureFqn("fakeSuspend", JsAstUtils.pureFqn("Kotlin", null)))
        fakeSuspendCall.isSuspend = true
        fakeSuspendCall.isFakeSuspend = true
        if (expression != null) {
            fakeSuspendCall.arguments += expression
        }
        return fakeSuspendCall
    }
}