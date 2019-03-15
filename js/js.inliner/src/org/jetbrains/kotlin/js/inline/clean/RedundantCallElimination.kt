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

import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.backend.ast.metadata.isJsCall
import org.jetbrains.kotlin.js.inline.util.isCallInvocation

// Replaces a.foo.call(a, b) with a.foo(b)
class RedundantCallElimination(private val root: JsBlock) {
    private var changed = false

    fun apply(): Boolean {
        root.accept(object : RecursiveJsVisitor() {
            override fun visitInvocation(invocation: JsInvocation) {
                tryEliminate(invocation)
                super.visitInvocation(invocation)
            }

            private fun tryEliminate(invocation: JsInvocation) {
                if (!isCallInvocation(invocation)) return

                val call = invocation.qualifier as? JsNameRef ?: return

                if (!call.isJsCall) return

                val qualifier = call.qualifier as? JsNameRef ?: return

                val receiver = qualifier.qualifier as? JsNameRef ?: return
                val firstArg = invocation.arguments.firstOrNull() as? JsNameRef ?: return

                if (receiver.qualifier == null && receiver.name != null && firstArg.qualifier == null && receiver.name == firstArg.name) {
                    invocation.arguments.removeAt(0)
                    invocation.qualifier = qualifier
                    changed = true
                }
            }
        })

        return changed
    }
}
