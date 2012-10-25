// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public final class JsExpressionStatement extends AbstractNode implements JsStatement {
    private JsExpression expression;

    public JsExpressionStatement(JsExpression expression) {
        this.expression = expression;
    }

    public JsExpression getExpression() {
        return expression;
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visit(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        expression = visitor.accept(expression);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.EXPRESSION_STMT;
    }

    @Override
    public Object getSourceInfo() {
        return null;
    }

    @Override
    public void setSourceInfo(Object info) {
        throw new IllegalStateException("You must not set source info for JsExpressionStatement, set for expression");
    }
}
