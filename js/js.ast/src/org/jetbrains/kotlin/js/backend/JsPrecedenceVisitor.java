// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.*;

/**
 * Precedence indices from "JavaScript - The Definitive Guide" 4th Edition (page
 * 57)
 * <p/>
 * Precedence 17 is for indivisible primaries that either don't have children,
 * or provide their own delimiters.
 * <p/>
 * Precedence 16 is for really important things that have their own AST classes.
 * <p/>
 * Precedence 15 is for the new construct.
 * <p/>
 * Precedence 14 is for unary operators.
 * <p/>
 * Precedences 12 through 4 are for non-assigning binary operators.
 * <p/>
 * Precedence 3 is for the tertiary conditional.
 * <p/>
 * Precedence 2 is for assignments.
 * <p/>
 * Precedence 1 is for comma operations.
 */
class JsPrecedenceVisitor extends JsVisitor {
    static final int PRECEDENCE_NEW = 15;

    private int answer = -1;

    private JsPrecedenceVisitor() {
    }

    public static int exec(JsExpression expression) {
        JsPrecedenceVisitor visitor = new JsPrecedenceVisitor();
        visitor.accept(expression);
        if (visitor.answer < 0) {
            throw new RuntimeException("Precedence must be >= 0!");
        }
        return visitor.answer;
    }

    @Override
    public void visitArrayAccess(@NotNull JsArrayAccess x) {
        answer = 16;
    }

    @Override
    public void visitArray(@NotNull JsArrayLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitBinaryExpression(@NotNull JsBinaryOperation x) {
        answer = x.getOperator().getPrecedence();
    }

    @Override
    public void visitBoolean(@NotNull JsBooleanLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitConditional(@NotNull JsConditional x) {
        answer = 3;
    }

    @Override
    public void visitFunction(@NotNull JsFunction x) {
        if (x.isEs6Arrow()) {
            answer = 2;
        } else {
            answer = 17; // primary
        }
    }

    @Override
    public void visitInvocation(@NotNull JsInvocation invocation) {
        answer = 16;
    }

    @Override
    public void visitYield(@NotNull JsYield yield) {
        answer = 2; // https://esdiscuss.org/topic/precedence-of-yield-operator
    }

    @Override
    public void visitNameRef(@NotNull JsNameRef nameRef) {
        if (nameRef.isLeaf()) {
            answer = 17; // primary
        }
        else {
            answer = 16; // property access
        }
    }

    @Override
    public void visitNew(@NotNull JsNew x) {
        answer = PRECEDENCE_NEW;
    }

    @Override
    public void visitNull(@NotNull JsNullLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitInt(@NotNull JsIntLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitDouble(@NotNull JsDoubleLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitBigInt(@NotNull JsBigIntLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitObjectLiteral(@NotNull JsObjectLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitPostfixOperation(@NotNull JsPostfixOperation x) {
        answer = x.getOperator().getPrecedence();
    }

    @Override
    public void visitPrefixOperation(@NotNull JsPrefixOperation x) {
        answer = x.getOperator().getPrecedence();
    }

    @Override
    public void visitPropertyInitializer(@NotNull JsPropertyInitializer x) {
        answer = 17; // primary
    }

    @Override
    public void visitRegExp(@NotNull JsRegExp x) {
        answer = 17; // primary
    }

    @Override
    public void visitString(@NotNull JsStringLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitThis(@NotNull JsThisRef x) {
        answer = 17; // primary
    }

    @Override
    public void visitSuper(@NotNull JsSuperRef x) {
        answer = 17; // primary
    }

    @Override
    protected void visitElement(@NotNull JsNode node) {
        throw new RuntimeException("Only expressions have precedence.");
    }
}
