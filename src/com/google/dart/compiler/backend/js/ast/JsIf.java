// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript if statement.
 */
public final class JsIf extends JsNodeImpl implements JsStatement {
    private JsExpression ifExpression;
    private JsStatement thenStatement;
    private JsStatement elseStatement;

    public JsIf() {
    }

    public JsIf(JsExpression ifExpression, JsStatement thenStatement, JsStatement elseStatement) {
        this.ifExpression = ifExpression;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
    }

    public JsIf(JsExpression ifExpression, JsStatement thenStatement) {
        this.ifExpression = ifExpression;
        this.thenStatement = thenStatement;
    }

    public JsStatement getElseStatement() {
        return elseStatement;
    }

    public JsExpression getIfExpression() {
        return ifExpression;
    }

    public JsStatement getThenStatement() {
        return thenStatement;
    }

    public void setElseStatement(JsStatement elseStmt) {
        this.elseStatement = elseStmt;
    }

    public void setIfExpression(JsExpression ifExpr) {
        this.ifExpression = ifExpr;
    }

    public void setThenStatement(JsStatement thenStmt) {
        this.thenStatement = thenStmt;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            ifExpression = v.accept(ifExpression);
            thenStatement = v.accept(thenStatement);
            if (elseStatement != null) {
                elseStatement = v.accept(elseStatement);
            }
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.IF;
    }
}
