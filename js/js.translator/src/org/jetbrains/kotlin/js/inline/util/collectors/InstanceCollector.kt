/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.inline.util.collectors

import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsObjectLiteral
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import java.util.ArrayList

class InstanceCollector<T : JsNode>(val klass: Class<T>, val visitNestedDeclarations: Boolean) : RecursiveJsVisitor() {
    val collected: MutableList<T> = ArrayList()

    override fun visitFunction(x: JsFunction) {
        if (visitNestedDeclarations) {
            visitElement(x)
        }
    }

    override fun visitObjectLiteral(x: JsObjectLiteral) {
        if (visitNestedDeclarations) {
            visitElement(x)
        }
    }

    override fun visitElement(node: JsNode) {
        if (klass.isInstance(node)) {
            collected.add(klass.cast(node)!!)
        }

        super.visitElement(node)
    }
}
