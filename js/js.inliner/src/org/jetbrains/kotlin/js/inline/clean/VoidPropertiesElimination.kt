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
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassBoxing
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassUnboxing

// Replaces { a: 2, b: VOID, c: VOID } with { a: 2 }
class VoidPropertiesElimination(private val root: JsBlock, private val voidName: JsName) {
    private var changed = false

    fun apply(): Boolean {
        val visitor = object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsPropertyInitializer, ctx: JsContext<JsNode>) {
                super.endVisit(x, ctx)
                if ((x.valueExpr as? JsNameRef)?.name === voidName) {
                    ctx.removeMe()
                    changed = true
                }
            }
        }

        visitor.accept(root)

        return changed
    }
}
