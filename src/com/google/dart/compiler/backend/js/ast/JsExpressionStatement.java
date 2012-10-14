// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

public final class JsExpressionStatement extends AbstractNode implements JsStatement {
    private JsExpression expression;

    public JsExpressionStatement(JsExpression expression) {
        this.expression = expression;
    }

    public JsExpression getExpression() {
        return expression;
    }

    @Override
    public void traverse(JsVisitor v, JsContext context) {
        if (v.visit(this, context)) {
            expression = v.accept(expression);
        }
        v.endVisit(this, context);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.EXPRESSION_STMT;
    }

    @Override
    public Object getSourceInfo() {
        return expression.getSourceInfo();
    }

    @Override
    public void setSourceInfo(Object info) {
        expression.setSourceInfo(info);
    }
}
