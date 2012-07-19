// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript do..while statement.
 */
public class JsDoWhile extends JsWhile {
    public JsDoWhile() {
    }

    public JsDoWhile(JsExpression condition, JsStatement body) {
        super(condition, body);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.DO;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            condition = v.accept(condition);
            body = v.accept(body);
        }
        v.endVisit(this, ctx);
    }
}
