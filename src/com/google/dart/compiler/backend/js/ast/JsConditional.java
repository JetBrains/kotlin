// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public final class JsConditional extends JsExpressionImpl {
    private JsExpression testExpression;
    private JsExpression elseExpression;
    private JsExpression thenExpression;

    public JsConditional() {
    }

    public JsConditional(JsExpression testExpression, JsExpression thenExpression, JsExpression elseExpression) {
        this.testExpression = testExpression;
        this.thenExpression = thenExpression;
        this.elseExpression = elseExpression;
    }

    public JsExpression getElseExpression() {
        return elseExpression;
    }

    public JsExpression getTestExpression() {
        return testExpression;
    }

    public JsExpression getThenExpression() {
        return thenExpression;
    }

    @Override
    public boolean hasSideEffects() {
        return testExpression.hasSideEffects() || thenExpression.hasSideEffects() || elseExpression.hasSideEffects();
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return thenExpression.isDefinitelyNotNull() && elseExpression.isDefinitelyNotNull();
    }

    @Override
    public boolean isDefinitelyNull() {
        return thenExpression.isDefinitelyNull() && elseExpression.isDefinitelyNull();
    }

    public void setElseExpression(JsExpression elseExpression) {
        this.elseExpression = elseExpression;
    }

    public void setTestExpression(JsExpression testExpression) {
        this.testExpression = testExpression;
    }

    public void setThenExpression(JsExpression thenExpression) {
        this.thenExpression = thenExpression;
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visitConditional(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        visitor.accept(testExpression);
        visitor.accept(thenExpression);
        visitor.accept(elseExpression);
    }
}
