/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.js.backend.ast.JsExpression.JsExpressionHasArguments
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassBoxing
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassUnboxing
import org.jetbrains.kotlin.js.backend.ast.metadata.isJsCall
import org.jetbrains.kotlin.js.inline.util.isCallInvocation

// Replaces box(unbox(value)) and unbox(box(value)) with value
class BoxingUnboxingElimination(private val root: JsBlock) {
    private var changed = false

    fun apply(): Boolean {
        val visitor = object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsInvocation, ctx: JsContext<JsNode>) {
                super.endVisit(x, ctx)
                tryEliminate(x, ctx)
            }

            override fun endVisit(x: JsNew, ctx: JsContext<JsNode>) {
                super.endVisit(x, ctx)
                tryEliminate(x, ctx)
            }


            override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
                super.endVisit(x, ctx)
                tryEliminate(x, ctx)
            }

            override fun endVisit(x: JsArrayAccess, ctx: JsContext<*>) {
                super.endVisit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<JsNode>) = false

            private fun tryEliminate(expression: JsExpression, ctx: JsContext<JsNode>) {
                if (!expression.isInlineClassBoxing && !expression.isInlineClassUnboxing) return

                val firstArg = expression.arguments.first()

                if (!firstArg.isInlineClassBoxing && !firstArg.isInlineClassUnboxing) return

                if (firstArg.isInlineClassBoxing != expression.isInlineClassBoxing) {
                    ctx.replaceMe(firstArg.arguments.first())
                    changed = true
                }
            }

            private val JsExpression.arguments: List<JsExpression>
                get() = when (this) {
                    is JsExpressionHasArguments -> arguments
                    is JsNameRef -> listOfNotNull(qualifier)
                    else -> emptyList()
                }
        }

        visitor.accept(root)

        return changed
    }
}
