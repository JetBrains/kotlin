/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend

import org.jetbrains.kotlin.js.backend.ast.*

/**
 * Searches for method invocations in constructor expressions that would not
 * normally be surrounded by parentheses.
 */
class JsConstructExpressionVisitor private constructor() : RecursiveJsVisitor() {
    private var containsInvocation = false

    /**
     * We only look at the array expression since the index has its own scope.
     */
    override fun visitArrayAccess(x: JsArrayAccess) {
        accept(x.arrayExpression)
    }

    /**
     * Array literals have their own scoping.
     */
    override fun visitArray(x: JsArrayLiteral) {
    }

    /**
     * Functions have their own scoping.
     */
    override fun visitFunction(x: JsFunction) {
    }

    override fun visitInvocation(invocation: JsInvocation) {
        containsInvocation = true
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        if (!nameRef.isLeaf) {
            accept(nameRef.qualifier)
        }
    }

    /**
     * New constructs bind to the nearest set of parentheses.
     */
    override fun visitNew(x: JsNew) {
    }

    /**
     * Object literals have their own scope.
     */
    override fun visitObjectLiteral(x: JsObjectLiteral) {
    }

    /**
     * We only look at nodes that would not normally be surrounded by parentheses.
     */
    override fun <T : JsNode?> accept(node: T) {
        if (node is JsExpression) {
            val precedence = JsPrecedenceVisitor.exec(node)
            // Only visit expressions that won't automatically be surrounded by
            // parentheses
            if (precedence < JsPrecedenceVisitor.PRECEDENCE_NEW) {
                return
            }
        }
        super.accept(node)
    }

    companion object {
        fun exec(expression: JsExpression): Boolean {
            if (JsPrecedenceVisitor.exec(expression) < JsPrecedenceVisitor.PRECEDENCE_NEW) {
                return true
            }
            val visitor = JsConstructExpressionVisitor()
            visitor.accept(expression)
            return visitor.containsInvocation
        }
    }
}
