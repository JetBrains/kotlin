// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.common.Symbol;
import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a JavaScript label statement.
 */
public class JsLabel extends SourceInfoAwareJsNode implements JsStatement, HasName {
    private JsName label;

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
    public void setName(JsName name) {
        label = name;
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
    public void accept(JsVisitor v) {
        v.visitLabel(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(statement);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            statement = v.acceptStatement(statement);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsLabel deepCopy() {
        return new JsLabel(label, AstUtil.deepCopy(statement.deepCopy())).withMetadataFrom(this);
    }
}
