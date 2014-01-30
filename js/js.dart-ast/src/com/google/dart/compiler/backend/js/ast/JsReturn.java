// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript return statement.
 */
public final class JsReturn extends SourceInfoAwareJsNode implements JsStatement {
    private JsExpression expression;

    public JsReturn() {
    }

    public JsReturn(JsExpression expression) {
        this.expression = expression;
    }

    public JsExpression getExpression() {
        return expression;
    }

    public void setExpression(JsExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitReturn(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        if (expression != null) {
            visitor.accept(expression);
        }
    }
}
