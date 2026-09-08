/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend

import org.jetbrains.kotlin.js.backend.ast.*

/**
 * Precedence indices from "JavaScript - The Definitive Guide" 4th Edition (page 57)
 *
 * Precedence 17 is for indivisible primaries that either don't have children,
 * or provide their own delimiters.
 *
 * Precedence 16 is for really important things that have their own AST classes.
 *
 * Precedence 15 is for the new construct.
 *
 * Precedence 14 is for unary operators.
 *
 * Precedences 12 through 4 are for non-assigning binary operators.
 *
 * Precedence 3 is for the tertiary conditional.
 *
 * Precedence 2 is for assignments.
 *
 * Precedence 1 is for comma operations.
 */
internal class JsPrecedenceVisitor private constructor() : JsVisitor() {
    private var answer = -1

    override fun visitArrayAccess(x: JsArrayAccess) {
        answer = 16
    }

    override fun visitArray(x: JsArrayLiteral) {
        answer = 17 // primary
    }

    override fun visitBinaryExpression(x: JsBinaryOperation) {
        answer = x.operator.precedence
    }

    override fun visitSimpleAssignment(x: JsAssignmentOperation.Simple) {
        answer = JsAssignmentOperation.PRECEDENCE
    }

    override fun visitDestructuringAssignment(x: JsAssignmentOperation.Destructuring) {
        answer = JsAssignmentOperation.PRECEDENCE
    }

    override fun visitBoolean(x: JsBooleanLiteral) {
        answer = 17 // primary
    }

    override fun visitConditional(x: JsConditional) {
        answer = 3
    }

    override fun visitFunction(x: JsFunction) {
        answer =
            if (x.isEs6Arrow) 2
            else 17 // primary
    }

    override fun visitInvocation(invocation: JsInvocation) {
        answer = 16
    }

    override fun visitYield(x: JsYield) {
        answer = 2 // https://esdiscuss.org/topic/precedence-of-yield-operator
    }

    override fun visitYieldStar(x: JsYieldStar) {
        answer = 2 // https://esdiscuss.org/topic/precedence-of-yield-operator
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        answer =
            if (nameRef.isLeaf) 17 // primary
            else 16 // property access
    }

    override fun visitNew(x: JsNew) {
        answer = PRECEDENCE_NEW
    }

    override fun visitNull(x: JsNullLiteral) {
        answer = 17 // primary
    }

    override fun visitInt(x: JsIntLiteral) {
        answer = 17 // primary
    }

    override fun visitDouble(x: JsDoubleLiteral) {
        answer = 17 // primary
    }

    override fun visitBigInt(x: JsBigIntLiteral) {
        answer = 17 // primary
    }

    override fun visitObjectLiteral(x: JsObjectLiteral) {
        answer = 17 // primary
    }

    override fun visitClass(x: JsClass) {
        answer = 17 // primary
    }

    override fun visitPostfixOperation(x: JsPostfixOperation) {
        answer = x.operator.precedence
    }

    override fun visitPrefixOperation(x: JsPrefixOperation) {
        answer = x.operator.precedence
    }

    override fun visitPropertyInitializer(x: JsPropertyInitializer) {
        answer = 17 // primary
    }

    override fun visitRegExp(x: JsRegExp) {
        answer = 17 // primary
    }

    override fun visitString(x: JsStringLiteral) {
        answer = 17 // primary
    }

    override fun visitTemplateString(x: JsTemplateStringLiteral) {
        answer =
            if (x.tag != null) 2
            else 17 // primary
    }

    override fun visitThis(x: JsThisRef) {
        answer = 17 // primary
    }

    override fun visitSuper(x: JsSuperRef) {
        answer = 17 // primary
    }

    override fun visitSpread(spread: JsSpread) {
        answer = 17 // primary
    }

    override fun visitElement(node: JsNode) {
        error("Only expressions have precedence.")
    }

    companion object {
        const val PRECEDENCE_NEW = 15

        fun exec(expression: JsExpression): Int {
            val visitor = JsPrecedenceVisitor()
            visitor.accept(expression)
            if (visitor.answer < 0) {
                error("Precedence must be >= 0!")
            }
            return visitor.answer
        }
    }
}
