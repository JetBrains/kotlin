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

package org.jetbrains.kotlin.js.inline.util.rewriters

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.functionDescriptor
import com.google.dart.compiler.backend.js.ast.metadata.returnTarget
import com.google.dart.compiler.backend.js.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.canHaveSideEffect
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class ReturnReplacingVisitor(
        private val resultRef: JsNameRef?,
        private val breakLabel: JsNameRef?,
        private val function: JsFunction
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
        if (returnExpression != null) {
            if (resultRef != null) {
                return JsAstUtils.assignment(resultRef, returnExpression).apply { synthetic = true }
            }

            if (returnExpression.canHaveSideEffect()) return returnExpression
        }

        return null
    }
}