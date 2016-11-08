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

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.isFakeSuspend
import com.google.dart.compiler.backend.js.ast.metadata.isPreSuspend
import com.google.dart.compiler.backend.js.ast.metadata.isSuspend
import com.google.dart.compiler.backend.js.ast.metadata.synthetic
import org.jetbrains.kotlin.js.translate.context.Namer

fun <T : JsNode> T.removeFakeSuspend(): T {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsInvocation, ctx: JsContext<in JsNode>) {
            if (x.isFakeSuspend) {
                ctx.replaceMe(x.arguments.getOrElse(0) { Namer.getUndefinedExpression() })
            }
            else {
                x.isSuspend = false
                x.isPreSuspend = false
            }
            super.endVisit(x, ctx)
        }

        override fun visit(x: JsExpressionStatement, ctx: JsContext<*>): Boolean {
            val expression = x.expression
            if (expression is JsInvocation && expression.isFakeSuspend && expression.arguments.isEmpty()) {
                x.synthetic = true
            }
            return super.visit(x, ctx)
        }
    }
    return visitor.accept(this)
}