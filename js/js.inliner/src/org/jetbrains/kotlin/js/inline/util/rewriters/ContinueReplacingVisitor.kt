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

import org.jetbrains.kotlin.js.backend.ast.*

class ContinueReplacingVisitor(val loopLabelName: JsName?, val guardLabelName: JsName) : JsVisitorWithContextImpl() {
    var loopNestingLevel = 0

    override fun visit(x: JsFunction, ctx: JsContext<JsNode>) = false

    override fun visit(x: JsContinue, ctx: JsContext<JsNode>): Boolean {
        val target = x.label?.name
        val shouldReplace = if (target == null) loopNestingLevel == 0 else target == loopLabelName
        assert(loopNestingLevel >= 0)
        if (shouldReplace) {
            ctx.replaceMe(JsBreak(guardLabelName.makeRef()))
        }

        return false
    }

    override fun visit(x: JsLoop, ctx: JsContext<JsNode>): Boolean {
        if (loopLabelName == null) return false

        loopNestingLevel++
        return super.visit(x, ctx)
    }

    override fun endVisit(x: JsLoop, ctx: JsContext<JsNode>) {
        super.endVisit(x, ctx)
        if (loopLabelName == null) return
        loopNestingLevel--
    }
}
