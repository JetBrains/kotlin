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

object LabeledBlockToDoWhileTransformation {
    fun apply(fragments: List<JsProgramFragment>) {
        for (fragment in fragments) {
            apply(fragment.declarationBlock)
            apply(fragment.initializerBlock)
        }
    }

    fun apply(root: JsNode) {
        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsLabel, ctx: JsContext<JsNode>) {
                if (x.statement is JsBlock) {
                    x.statement = JsDoWhile(JsLiteral.FALSE, x.statement)
                }

                super.endVisit(x, ctx)
            }
        }.accept(root)
    }
}
