// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.JsArrayAccess;
import com.google.dart.compiler.backend.js.ast.JsArrayLiteral;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsContext;
import com.google.dart.compiler.backend.js.ast.JsExprStmt;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral;
import com.google.dart.compiler.backend.js.ast.JsPostfixOperation;
import com.google.dart.compiler.backend.js.ast.JsPrefixOperation;
import com.google.dart.compiler.backend.js.ast.JsRegExp;
import com.google.dart.compiler.backend.js.ast.JsVisitor;

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
public class JsFirstExpressionVisitor extends JsVisitor {
    public static boolean exec(JsExprStmt statement) {
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
    public boolean visit(JsArrayAccess x, JsContext ctx) {
        accept(x.getArrayExpr());
        return false;
    }

    @Override
    public boolean visit(JsArrayLiteral x, JsContext ctx) {
        return false;
    }

    @Override
    public boolean visit(JsBinaryOperation x, JsContext ctx) {
        accept(x.getArg1());
        return false;
    }

    @Override
    public boolean visit(JsConditional x, JsContext ctx) {
        accept(x.getTestExpression());
        return false;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
        needsParentheses = true;
        return false;
    }

    @Override
    public boolean visit(JsInvocation x, JsContext ctx) {
        accept(x.getQualifier());
        return false;
    }

    @Override
    public boolean visit(JsNameRef x, JsContext ctx) {
        if (!x.isLeaf()) {
            accept(x.getQualifier());
        }
        return false;
    }

    @Override
    public boolean visit(JsNew x, JsContext ctx) {
        return false;
    }

    @Override
    public boolean visit(JsObjectLiteral x, JsContext ctx) {
        needsParentheses = true;
        return false;
    }

    @Override
    public boolean visit(JsPostfixOperation x, JsContext ctx) {
        accept(x.getArg());
        return false;
    }

    @Override
    public boolean visit(JsPrefixOperation x, JsContext ctx) {
        return false;
    }

    @Override
    public boolean visit(JsRegExp x, JsContext ctx) {
        return false;
    }
}
