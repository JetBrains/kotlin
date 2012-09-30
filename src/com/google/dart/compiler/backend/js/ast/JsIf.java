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

    public void setElseStatement(JsStatement elseStatement) {
        this.elseStatement = elseStatement;
    }

    public void setIfExpression(JsExpression ifExpression) {
        this.ifExpression = ifExpression;
    }

    public void setThenStatement(JsStatement thenStatement) {
        this.thenStatement = thenStatement;
    }

    @Override
    public void traverse(JsVisitor v, JsContext context) {
        if (v.visit(this, context)) {
            ifExpression = v.accept(ifExpression);
            thenStatement = v.accept(thenStatement);
            if (elseStatement != null) {
                elseStatement = v.accept(elseStatement);
            }
        }
        v.endVisit(this, context);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.IF;
    }
}
