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

package org.jetbrains.kotlin.js.dce

import org.jetbrains.kotlin.js.backend.ast.*

class Eliminator(private val analysisResult: AnalysisResult) : JsVisitorWithContextImpl() {
    override fun visit(x: JsVars.JsVar, ctx: JsContext<*>): Boolean = removeIfNecessary(x, ctx)

    override fun visit(x: JsExpressionStatement, ctx: JsContext<*>): Boolean = removeIfNecessary(x, ctx)

    override fun visit(x: JsReturn, ctx: JsContext<*>): Boolean = removeIfNecessary(x, ctx)

    private fun removeIfNecessary(x: JsNode, ctx: JsContext<*>): Boolean {
        if (x in analysisResult.astNodesToEliminate) {
            ctx.removeMe()
            return false
        }
        val node = analysisResult.nodeMap[x]?.original
        return if (!isUsed(node)) {
            ctx.removeMe()
            false
        }
        else {
            true
        }
    }

    override fun endVisit(x: JsVars, ctx: JsContext<*>) {
        if (x.vars.isEmpty()) {
            ctx.removeMe()
        }
    }

    private fun isUsed(node: Context.Node?): Boolean = node == null || node.declarationReachable
}