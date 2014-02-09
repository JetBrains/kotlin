// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a JavaScript block statement.
 */
public class JsBlock extends JsNodeImpl implements JsStatement {
    private final List<JsStatement> statements;

    public JsBlock() {
        this(new ArrayList<JsStatement>());
    }

    public JsBlock(JsStatement statement) {
        this(Collections.singletonList(statement));
    }

    public JsBlock(JsStatement... statements) {
        this(Arrays.asList(statements));
    }

    public JsBlock(List<JsStatement> statements) {
        this.statements = statements;
    }

    public List<JsStatement> getStatements() {
        return statements;
    }

    public boolean isEmpty() {
        return statements.isEmpty();
    }

    public boolean isGlobalBlock() {
        return false;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptWithInsertRemove(statements);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.BLOCK;
    }
}
