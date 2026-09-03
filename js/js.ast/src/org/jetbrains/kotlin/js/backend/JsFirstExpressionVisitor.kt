/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend

import org.jetbrains.kotlin.js.backend.ast.*

/**
 * Determines if an expression statement needs to be surrounded by parentheses.
 *
 * The statement or the left-most expression needs to be surrounded by
 * parentheses if the left-most expression is an object literal or a function
 * object. Function declarations do not need parentheses.
 *
 * For example the following require parentheses:
 * - `{ key : 'value'}`
 * - `{ key : 'value'}.key`
 * - `function () {return 1;}()`
 * - `function () {return 1;}.prototype`
 *
 * The following do not require parentheses:
 * - `var x = { key : 'value'}`
 * - `"string" + { key : 'value'}.key`
 * - `function func() {}`
 * - `function() {}`
 */
internal class JsFirstExpressionVisitor private constructor() : RecursiveJsVisitor() {
    private var needsParentheses = false

    override fun visitArrayAccess(x: JsArrayAccess) {
        accept(x.arrayExpression)
    }

    override fun visitArray(x: JsArrayLiteral) {
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        accept(x.arg1)
    }

    override fun visitConditional(x: JsConditional) {
        accept(x.testExpression)
    }

    override fun visitFunction(x: JsFunction) {
        needsParentheses = true
    }

    override fun visitInvocation(invocation: JsInvocation) {
        accept(invocation.qualifier)
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        if (!nameRef.isLeaf) {
            accept(nameRef.qualifier)
        }
    }

    override fun visitNew(x: JsNew) {
    }

    override fun visitObjectLiteral(x: JsObjectLiteral) {
        needsParentheses = true
    }

    override fun visitSimpleAssignment(x: JsAssignmentOperation.Simple) {
        accept(x.target)
    }

    override fun visitDestructuringAssignment(x: JsAssignmentOperation.Destructuring) {
        // The left-most token is the assignment target. An object pattern starts with '{',
        // which would otherwise be parsed as a block at the beginning of a statement.
        if (x.pattern is JsDeclarable.ObjectPattern) {
            needsParentheses = true
        }
    }

    override fun visitPostfixOperation(x: JsPostfixOperation) {
        accept(x.arg)
    }

    override fun visitPrefixOperation(x: JsPrefixOperation) {
    }

    companion object {
        fun exec(statement: JsExpressionStatement): Boolean {
            val expression = statement.expression
            // Pure function declarations do not need parentheses
            if (expression is JsFunction || expression is JsClass) {
                return false
            }

            val visitor = JsFirstExpressionVisitor()
            visitor.accept(statement.expression)
            return visitor.needsParentheses
        }
    }
}
