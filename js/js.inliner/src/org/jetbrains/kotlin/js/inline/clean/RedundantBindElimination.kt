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

import com.google.dart.compiler.backend.js.ast.JsBlock
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.RecursiveJsVisitor

// TODO: this optimization is a little unfair. It tries to recognize pattern like this a.bind(b)(args) and
// replace it with b.a(args). However, we can't be completely sure that `a` is a Function.
// Using JS-independent AST should solve the issue (as well as many other issues).
class RedundantBindElimination(private val root: JsBlock) {
    private var changed = false

    fun apply(): Boolean {
        root.accept(object : RecursiveJsVisitor() {
            override fun visitInvocation(invocation: JsInvocation) {
                tryEliminate(invocation)
                super.visitInvocation(invocation)
            }

            private fun tryEliminate(invocation: JsInvocation) {
                val qualifier = invocation.qualifier as? JsInvocation ?: return

                val outerQualifier = qualifier.qualifier as? JsNameRef ?: return
                val name = outerQualifier.ident
                if (name != "bind") return

                val qualifierReplacement = outerQualifier.qualifier ?: return
                invocation.qualifier = qualifierReplacement
                changed = true
            }
        })

        return changed
    }
}
