// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;

/**
 * Represents a JavaScript label statement.
 */
public class JsLabel extends JsNodeImpl implements JsStatement, HasName {
    private final JsName label;

    private JsStatement statement;

    public JsLabel(JsName label) {
        this.label = label;
    }

    public JsLabel(JsName label, JsStatement statement) {
        this.label = label;
        this.statement = statement;
    }

    @Override
    public JsName getName() {
        return label;
    }

    @Override
    public Symbol getSymbol() {
        return label;
    }

    public JsStatement getStatement() {
        return statement;
    }

    public void setStatement(JsStatement statement) {
        this.statement = statement;
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visitLabel(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        visitor.accept(statement);
    }
}
