// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

public final class JsConditional extends JsExpression {
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

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            testExpression = v.accept(testExpression);
            thenExpression = v.accept(thenExpression);
            elseExpression = v.accept(elseExpression);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsConditional deepCopy() {
        JsExpression testCopy = AstUtil.deepCopy(testExpression);
        JsExpression thenCopy = AstUtil.deepCopy(thenExpression);
        JsExpression elseCopy = AstUtil.deepCopy(elseExpression);

        return new JsConditional(testCopy, thenCopy, elseCopy).withMetadataFrom(this);
    }
}
