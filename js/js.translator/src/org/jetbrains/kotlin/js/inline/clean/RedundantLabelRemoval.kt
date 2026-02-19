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

internal class RedundantLabelRemoval(private val root: JsStatement) {
    private val labelUsages = mutableMapOf<JsName, Int>()
    private var hasChanges = false

    fun apply(): Boolean {
        analyze()
        perform()
        return hasChanges
    }

    private fun analyze() {
        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsBreak, ctx: JsContext<*>) {
                super.endVisit(x, ctx)
                x.label?.let { useLabel(it.name!!) }
            }

            override fun endVisit(x: JsContinue, ctx: JsContext<*>) {
                super.endVisit(x, ctx)
                x.label?.let { useLabel(it.name!!) }
            }
        }.accept(root)
    }

    private fun perform() {
        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
                if (x.synthetic) {
                    x.statement = perform(x.statement, x.name)
                    if (labelUsages[x.name] ?: 0 == 0) {
                        hasChanges = true
                        ctx.replaceMe(x.statement)
                    }
                }

                super.endVisit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<*>) = false
        }.accept(root)
    }

    private fun perform(statement: JsStatement, name: JsName): JsStatement = when (statement) {
        is JsBreak ->
            if (name == statement.label?.name) {
                unuseLabel(name)
                hasChanges = true
                JsEmpty
            }
            else {
                statement
            }
        is JsLabel -> {
            perform(statement.statement, name)
            statement
        }
        is JsBlock -> {
            perform(statement.statements, name)
            statement
        }
        is JsIf -> {
            statement.thenStatement = perform(statement.thenStatement, name)
            statement.elseStatement = statement.elseStatement?.let { perform(it, name) }
            statement
        }
        is JsTry -> {
            perform(statement.tryBlock, name)
            statement
        }
        else -> statement
    }

    private fun perform(statements: MutableList<JsStatement>, name: JsName) {
        statements.lastOrNull()?.let { statements[statements.lastIndex] = perform(it, name) }
    }

    private fun useLabel(name: JsName) {
        labelUsages[name] = (labelUsages[name] ?: 0) + 1
    }

    private fun unuseLabel(name: JsName) {
        labelUsages[name] = labelUsages[name]!! - 1
    }
}
