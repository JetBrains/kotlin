// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.*;

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
    public void visitArrayAccess(JsArrayAccess x) {
        answer = 16;
    }

    @Override
    public void visitArray(JsArrayLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitBinaryExpression(JsBinaryOperation x) {
        answer = x.getOperator().getPrecedence();
    }

    @Override
    public void visitBoolean(JsLiteral.JsBooleanLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitConditional(JsConditional x) {
        answer = 3;
    }

    @Override
    public void visitFunction(JsFunction x) {
        answer = 17; // primary
    }

    @Override
    public void visitInvocation(JsInvocation invocation) {
        answer = 16;
    }

    @Override
    public void visitNameRef(JsNameRef nameRef) {
        if (nameRef.isLeaf()) {
            answer = 17; // primary
        }
        else {
            answer = 16; // property access
        }
    }

    @Override
    public void visitNew(JsNew x) {
        answer = PRECEDENCE_NEW;
    }

    @Override
    public void visitNull(JsNullLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitInt(JsNumberLiteral.JsIntLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitDouble(JsNumberLiteral.JsDoubleLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitObjectLiteral(JsObjectLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitPostfixOperation(JsPostfixOperation x) {
        answer = x.getOperator().getPrecedence();
    }

    @Override
    public void visitPrefixOperation(JsPrefixOperation x) {
        answer = x.getOperator().getPrecedence();
    }

    @Override
    public void visitPropertyInitializer(JsPropertyInitializer x) {
        answer = 17; // primary
    }

    @Override
    public void visitRegExp(JsRegExp x) {
        answer = 17; // primary
    }

    @Override
    public void visitString(JsStringLiteral x) {
        answer = 17; // primary
    }

    @Override
    public void visitThis(JsLiteral.JsThisRef x) {
        answer = 17; // primary
    }

    @Override
    protected void visitElement(JsNode node) {
        throw new RuntimeException("Only expressions have precedence.");
    }
}
