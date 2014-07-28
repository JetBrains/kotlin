/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline

import com.google.dart.compiler.backend.js.ast.*

fun replaceThisReference<T : JsNode>(node: T, replacement: JsExpression) {
    val visitor = ThisReplacingVisitor(replacement)
    visitor.accept(node)
}

fun hasThisReference(node: JsNode): Boolean {
    val visitor = ContainsThisVisitor()
    visitor.accept(node)
    return visitor.containsThis
}

private class ThisReplacingVisitor(private val thisReplacement: JsExpression) : JsVisitorWithContextImpl() {
    override fun endVisit(x: JsLiteral.JsThisRef?, ctx: JsContext?) {
        ctx?.replaceMe(thisReplacement)
    }

    override fun visit(x: JsFunction?, ctx: JsContext?) = false

    override fun visit(x: JsObjectLiteral?, ctx: JsContext?) = false
}

private class ContainsThisVisitor(): RecursiveJsVisitor() {
    public var containsThis: Boolean = false
        private set

    override fun visitElement(node: JsNode?) {
        if (!containsThis) {
            super<RecursiveJsVisitor>.visitElement(node)
        }
    }

    override fun visitThis(x: JsLiteral.JsThisRef?) {
        containsThis = true
    }


    override fun visitFunction(x: JsFunction?) { }

    override fun visitObjectLiteral(x: JsObjectLiteral?) { }
}
