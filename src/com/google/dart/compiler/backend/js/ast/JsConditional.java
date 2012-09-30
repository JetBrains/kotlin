// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript conditional expression.
 */
public final class JsConditional extends JsExpressionImpl {
    private JsExpression testExpr;
    private JsExpression elseExpr;
    private JsExpression thenExpr;

    public JsConditional() {
    }

    public JsConditional(JsExpression testExpr, JsExpression thenExpr, JsExpression elseExpr) {
        this.testExpr = testExpr;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    public JsExpression getElseExpression() {
        return elseExpr;
    }

    public JsExpression getTestExpression() {
        return testExpr;
    }

    public JsExpression getThenExpression() {
        return thenExpr;
    }

    @Override
    public boolean hasSideEffects() {
        return testExpr.hasSideEffects() || thenExpr.hasSideEffects() || elseExpr.hasSideEffects();
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return thenExpr.isDefinitelyNotNull() && elseExpr.isDefinitelyNotNull();
    }

    @Override
    public boolean isDefinitelyNull() {
        return thenExpr.isDefinitelyNull() && elseExpr.isDefinitelyNull();
    }

    public void setElseExpression(JsExpression elseExpr) {
        this.elseExpr = elseExpr;
    }

    public void setTestExpression(JsExpression testExpr) {
        this.testExpr = testExpr;
    }

    public void setThenExpression(JsExpression thenExpr) {
        this.thenExpr = thenExpr;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            testExpr = v.accept(testExpr);
            thenExpr = v.accept(thenExpr);
            elseExpr = v.accept(elseExpr);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.CONDITIONAL;
    }
}
