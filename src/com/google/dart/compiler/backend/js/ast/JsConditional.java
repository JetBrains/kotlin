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
    public void accept(JsVisitor v) {
        v.visitConditional(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(testExpression);
        visitor.accept(thenExpression);
        visitor.accept(elseExpression);
    }
}
