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

import org.jetbrains.kotlin.js.backend.ast.*
import java.util.*

class LabelNameRefreshingVisitor(val functionScope: JsFunctionScope) : JsVisitorWithContextImpl() {
    private val substitutions: MutableMap<JsName, ArrayDeque<JsName>> = mutableMapOf()

    override fun visit(x: JsFunction, ctx: JsContext<JsNode>): Boolean = false

    override fun endVisit(x: JsBreak, ctx: JsContext<JsNode>) {
        val label = x.label?.name
        if (label != null) {
            ctx.replaceMe(JsBreak(getSubstitution(label).makeRef()))
        }
        super.endVisit(x, ctx)
    }

    override fun endVisit(x: JsContinue, ctx: JsContext<JsNode>) {
        val label = x.label?.name
        if (label != null) {
            ctx.replaceMe(JsContinue(getSubstitution(label).makeRef()))
        }
        super.endVisit(x, ctx)
    }

    override fun visit(x: JsLabel, ctx: JsContext<JsNode>): Boolean {
        val labelName = x.name
        val freshName = functionScope.enterLabel(labelName.ident, labelName.ident)
        substitutions.getOrPut(labelName) { ArrayDeque() }.push(freshName)

        return super.visit(x, ctx)
    }

    override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
        val labelName = x.name
        val stack = substitutions[labelName]!!
        val replacementLabel = JsLabel(stack.pop(), x.statement).apply { copyMetadataFrom(x) }
        ctx.replaceMe(replacementLabel)
        functionScope.exitLabel()
        super.endVisit(x, ctx)
    }

    private fun getSubstitution(name: JsName) = substitutions[name]?.peek() ?: name
}
