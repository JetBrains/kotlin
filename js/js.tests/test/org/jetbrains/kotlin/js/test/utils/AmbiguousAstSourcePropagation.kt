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

package org.jetbrains.kotlin.js.test.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.backend.ast.*

class AmbiguousAstSourcePropagation : RecursiveJsVisitor() {
    private var sourceDefined = false

    override fun visitConditional(x: JsConditional) = acceptAll(x, x.testExpression, x.thenExpression, x.elseExpression)
    
    override fun visitBinaryExpression(x: JsBinaryOperation) = acceptAll(x, x.arg1, x.arg2)

    override fun visitPostfixOperation(x: JsPostfixOperation) = acceptAll(x, x.arg)

    override fun visitArrayAccess(x: JsArrayAccess) = acceptAll(x, x.arrayExpression, x.indexExpression)

    override fun visitPropertyInitializer(x: JsPropertyInitializer) = acceptAll(x, x.labelExpr, x.valueExpr)

    override fun visitInvocation(invocation: JsInvocation) = acceptAll(invocation, invocation.qualifier,
                                                                       *invocation.arguments.toTypedArray())

    override fun visitNameRef(nameRef: JsNameRef) {
        val qualifier = nameRef.qualifier
        if (qualifier != null) {
            acceptAll(nameRef, qualifier)
        }
        else {
            super.visitNameRef(nameRef)
        }
    }

    override fun visitElement(node: JsNode) {
        val old = sourceDefined
        propagate(node)

        sourceDefined = false
        super.visitElement(node)
        sourceDefined = old
    }
    
    private fun acceptAll(node: JsNode, first: JsNode, vararg remaining: JsNode) {
        val old = sourceDefined
        propagate(node)
        
        accept(first)

        sourceDefined = false
        remaining.forEach { accept(it) }
        sourceDefined = old
    }

    private fun propagate(node: JsNode) {
        if (!sourceDefined) {
            val source = node.source
            if (source is JsLocation || source is PsiElement) {
                sourceDefined = true
            }
        }
        else if (node !is JsExpressionStatement) {
            node.source = null
        }
    }
}