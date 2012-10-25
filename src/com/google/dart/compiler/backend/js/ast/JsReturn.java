// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript return statement.
 */
public final class JsReturn extends JsNodeImpl implements JsStatement {
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
    public void accept(JsVisitor v, JsContext context) {
        v.visitReturn(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        if (expression != null) {
                        expression = visitor.accept(expression);
                    }
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.RETURN;
    }
}
