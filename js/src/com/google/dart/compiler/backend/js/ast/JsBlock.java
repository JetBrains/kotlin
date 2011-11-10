// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript block statement.
 */
public class JsBlock extends JsStatement {

    private final List<JsStatement> stmts = new ArrayList<JsStatement>();

    public JsBlock() {
    }

    public JsBlock(JsStatement stmt) {
        stmts.add(stmt);
    }

    public List<JsStatement> getStatements() {
        return stmts;
    }

    public void addStatement(JsStatement statement) {
        assert statement != null;
        stmts.add(statement);
    }

    public boolean isGlobalBlock() {
        return false;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptWithInsertRemove(stmts);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.BLOCK;
    }

    public void setStatements(List<JsStatement> statements) {
        assert this.stmts.isEmpty() : "Already contains statements.";
        this.stmts.addAll(statements);
    }

}
