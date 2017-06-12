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

package org.jetbrains.kotlin.js.inline.util.rewriters

import org.jetbrains.kotlin.js.backend.ast.*

class NameReplacingVisitor(private val replaceMap: Map<JsName, JsExpression>) : JsVisitorWithContextImpl() {

    override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
        val replacement = replaceMap[x.name] ?: return
        ctx.replaceMe(replacement.deepCopy().source(x.source))
    }

    override fun endVisit(x: JsVars.JsVar, ctx: JsContext<JsNode>) {
        val replacement = replaceMap[x.name]
        if (replacement is HasName) {
            val replacementVar = JsVars.JsVar(replacement.name, x.initExpression)
            ctx.replaceMe(replacementVar.source(x.source))
        }
    }

    override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
        val replacement = replaceMap[x.name]
        if (replacement is HasName) {
            val replacementLabel = JsLabel(replacement.name, x.statement)
            ctx.replaceMe(replacementLabel)
        }
    }
}
