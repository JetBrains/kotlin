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

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.collectFreeVariables

internal class RedundantVariableDeclarationElimination(private val root: JsStatement) {
    private val usages = mutableSetOf<JsName>()
    private var hasChanges = false

    fun apply(): Boolean {
        analyze()
        perform()
        return hasChanges
    }

    private fun analyze() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsNameRef, ctx: JsContext<*>): Boolean {
                val name = x.name
                if (name != null && x.qualifier == null) {
                    usages += name
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsBreak, ctx: JsContext<*>) = false

            override fun visit(x: JsContinue, ctx: JsContext<*>) = false

            override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
                usages += x.collectFreeVariables()
                return false
            }
        }.accept(root)
    }

    private fun perform() {
        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsVars, ctx: JsContext<*>) {
                if (x.synthetic) {
                    if (x.vars.removeAll { it.initExpression == null && it.name !in usages }) {
                        hasChanges = true
                    }
                    if (x.vars.isEmpty()) {
                        ctx.removeMe()
                    }
                }
                super.endVisit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<*>) = false
        }.accept(root)
    }
}
