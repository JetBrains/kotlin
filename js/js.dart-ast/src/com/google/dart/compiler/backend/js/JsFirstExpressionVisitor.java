// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.JsExpressionStatement;

/**
 * Determines if an expression statement needs to be surrounded by parentheses.
 * <p/>
 * The statement or the left-most expression needs to be surrounded by
 * parentheses if the left-most expression is an object literal or a function
 * object. Function declarations do not need parentheses.
 * <p/>
 * For example the following require parentheses:<br>
 * <ul>
 * <li>{ key : 'value'}</li>
 * <li>{ key : 'value'}.key</li>
 * <li>function () {return 1;}()</li>
 * <li>function () {return 1;}.prototype</li>
 * </ul>
 * <p/>
 * The following do not require parentheses:<br>
 * <ul>
 * <li>var x = { key : 'value'}</li>
 * <li>"string" + { key : 'value'}.key</li>
 * <li>function func() {}</li>
 * <li>function() {}</li>
 * </ul>
 */
public class JsFirstExpressionVisitor extends RecursiveJsVisitor {
    public static boolean exec(JsExpressionStatement statement) {
        JsExpression expression = statement.getExpression();
        // Pure function declarations do not need parentheses
        if (expression instanceof JsFunction) {
            return false;
        }

        JsFirstExpressionVisitor visitor = new JsFirstExpressionVisitor();
        visitor.accept(statement.getExpression());
        return visitor.needsParentheses;
    }

    private boolean needsParentheses = false;

    private JsFirstExpressionVisitor() {
    }

    @Override
    public void visitArrayAccess(JsArrayAccess x) {
        accept(x.getArrayExpression());
    }

    @Override
    public void visitArray(JsArrayLiteral x) {
    }

    @Override
    public void visitBinaryExpression(JsBinaryOperation x) {
        accept(x.getArg1());
    }

    @Override
    public void visitConditional(JsConditional x) {
        accept(x.getTestExpression());
    }

    @Override
    public void visitFunction(JsFunction x) {
        needsParentheses = true;
    }

    @Override
    public void visitInvocation(JsInvocation invocation) {
        accept(invocation.getQualifier());
    }

    @Override
    public void visitNameRef(JsNameRef nameRef) {
        if (!nameRef.isLeaf()) {
            accept(nameRef.getQualifier());
        }
    }

    @Override
    public void visitNew(JsNew x) {
    }

    @Override
    public void visitObjectLiteral(JsObjectLiteral x) {
        needsParentheses = true;
    }

    @Override
    public void visitPostfixOperation(JsPostfixOperation x) {
        accept(x.getArg());
    }

    @Override
    public void visitPrefixOperation(JsPrefixOperation x) {
    }
}
