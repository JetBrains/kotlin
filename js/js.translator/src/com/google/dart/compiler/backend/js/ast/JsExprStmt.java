// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public final class JsExprStmt extends JsNodeImpl implements JsStatement {
    private JsExpression expr;

    public JsExprStmt(JsExpression expr) {
        super();
        this.expr = expr;
        this.setSourceInfo(expr);
    }

    public JsExpression getExpression() {
        return expr;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            expr = v.accept(expr);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.EXPRESSION_STMT;
    }
}
