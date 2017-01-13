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

package org.jetbrains.kotlin.js.coroutine

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class CoroutineTransformer(private val program: JsProgram) : JsVisitorWithContextImpl() {
    private val additionalStatementsByNode = mutableMapOf<JsNode, List<JsStatement>>()

    override fun endVisit(x: JsExpressionStatement, ctx: JsContext<in JsStatement>) {
        additionalStatementsByNode.remove(x)?.forEach { ctx.addNext(it) }
        super.endVisit(x, ctx)
    }

    override fun endVisit(x: JsVars, ctx: JsContext<in JsStatement>) {
        for (v in x.vars) {
            additionalStatementsByNode.remove(v)?.forEach { ctx.addNext(it) }
        }
        super.endVisit(x, ctx)
    }

    override fun visit(x: JsExpressionStatement, ctx: JsContext<*>): Boolean {
        val expression = x.expression
        val assignment = JsAstUtils.decomposeAssignment(expression)
        if (assignment != null) {
            val (lhs, rhs) = assignment
            val function = rhs as? JsFunction ?: InlineMetadata.decompose(rhs)?.function
            if (function?.coroutineMetadata != null) {
                val name = ((lhs as? JsNameRef)?.name ?: function.name)?.ident
                additionalStatementsByNode[x] = CoroutineFunctionTransformer(program, function, name).transform()
                return false
            }
        }
        else if (expression is JsFunction) {
            if (expression.coroutineMetadata != null) {
                additionalStatementsByNode[x] = CoroutineFunctionTransformer(program, expression, expression.name?.ident).transform()
                return false
            }
        }
        return super.visit(x, ctx)
    }

    override fun visit(x: JsVars.JsVar, ctx: JsContext<*>): Boolean {
        val initExpression = x.initExpression
        if (initExpression != null) {
            val function = initExpression as? JsFunction ?: InlineMetadata.decompose(initExpression)?.function
            if (function?.coroutineMetadata != null) {
                val name = x.name.ident
                additionalStatementsByNode[x] = CoroutineFunctionTransformer(program, function, name).transform()
                return false
            }
        }
        return super.visit(x, ctx)
    }
}
