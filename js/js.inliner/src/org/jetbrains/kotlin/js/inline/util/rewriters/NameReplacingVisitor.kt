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
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata

class NameReplacingVisitor(private val replaceMap: Map<JsName, JsExpression>) : JsVisitorWithContextImpl() {

    override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
        if (x.qualifier != null) return
        val replacement = replaceMap[x.name] ?: return
        if (replacement is JsNameRef) {
            applyToNamedNode(x)
        }
        else {
            val replacementCopy = replacement.deepCopy()
            if (x.source != null) {
                replacementCopy.source = x.source
            }
            ctx.replaceMe(accept(replacementCopy))
        }
    }

    override fun endVisit(x: JsVars.JsVar, ctx: JsContext<*>) = applyToNamedNode(x)

    override fun endVisit(x: JsLabel, ctx: JsContext<*>) = applyToNamedNode(x)

    override fun endVisit(x: JsFunction, ctx: JsContext<*>) = applyToNamedNode(x)

    override fun endVisit(x: JsParameter, ctx: JsContext<*>) = applyToNamedNode(x)

    override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
        x.coroutineMetadata?.let { coroutineMetadata ->
            x.coroutineMetadata = coroutineMetadata.copy(
                baseClassRef = accept(coroutineMetadata.baseClassRef.deepCopy()),
                suspendObjectRef = accept(coroutineMetadata.suspendObjectRef.deepCopy())
            )
        }
        return super.visit(x, ctx)
    }

    private fun applyToNamedNode(x: HasName) {
        while (true) {
            val replacement = replaceMap[x.name]
            if (replacement is HasName) {
                x.name = replacement.name
            }
            else {
                break
            }
        }
    }
}
