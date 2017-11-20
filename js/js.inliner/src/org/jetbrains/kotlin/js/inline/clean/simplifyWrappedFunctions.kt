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
import org.jetbrains.kotlin.js.inline.util.extractFunction

fun simplifyWrappedFunctions(root: JsNode) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsInvocation, ctx: JsContext<in JsNode>) {
            extractFunction(x)?.let { (function, wrapper) ->
                if (wrapper != null && wrapper.statements.size == 1) {
                    ctx.replaceMe(function)
                }
            }
        }

        override fun endVisit(x: JsVars, ctx: JsContext<in JsStatement>) {
            x.vars.singleOrNull()?.let { jsVar ->
                (jsVar.initExpression as? JsFunction)?.let { function ->
                    if (function.name == null) {
                        function.name = jsVar.name
                        ctx.replaceMe(function.makeStmt())
                    }
                }
            }
        }
    }
    visitor.accept(root)
}