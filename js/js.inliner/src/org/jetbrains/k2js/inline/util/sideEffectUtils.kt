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

package org.jetbrains.k2js.inline.util

import com.google.dart.compiler.backend.js.ast.JsArrayAccess
import com.google.dart.compiler.backend.js.ast.JsArrayLiteral
import com.google.dart.compiler.backend.js.ast.JsBinaryOperation
import com.google.dart.compiler.backend.js.ast.JsConditional
import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsLiteral.JsThisRef
import com.google.dart.compiler.backend.js.ast.JsLiteral.JsValueLiteral
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsNew
import com.google.dart.compiler.backend.js.ast.JsNode
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral
import com.google.dart.compiler.backend.js.ast.JsPostfixOperation
import com.google.dart.compiler.backend.js.ast.JsPrefixOperation
import com.google.dart.compiler.backend.js.ast.RecursiveJsVisitor
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator

public fun canHaveSideEffect(x: JsExpression): Boolean {
    return with(SideEffectVisitor()) {
        accept(x)
        !sideEffectFree
    }
}

public fun needToAlias(x: JsExpression): Boolean {
    return with(NeedToAliasVisitor()) {
        accept(x)
        !sideEffectFree
    }
}

private open class SideEffectVisitor() : RecursiveJsVisitor() {
    public var sideEffectFree: Boolean = true
        protected set

    override fun visitElement(node: JsNode?) {
        sideEffectFree = sideEffectFree && isSideEffectFree(node)

        if (sideEffectFree) {
            super.visitElement(node)
        }
    }

    protected open fun isSideEffectFree(node: JsNode?): Boolean =
        when (node) {
            is JsValueLiteral,
            is JsConditional,
            is JsArrayAccess,
            is JsArrayLiteral,
            is JsNameRef ->
                true
            is JsBinaryOperation ->
                !node.getOperator().isAssignment()
            else ->
                false
        }
}

private class NeedToAliasVisitor() : SideEffectVisitor() {
    override fun isSideEffectFree(node: JsNode?): Boolean =
        when (node) {
            is JsThisRef,
            is JsConditional,
            is JsBinaryOperation,
            is JsArrayLiteral -> false
            is JsInvocation -> isFunctionCreatorInvocation(node)
            else -> super.isSideEffectFree(node)
        }
}