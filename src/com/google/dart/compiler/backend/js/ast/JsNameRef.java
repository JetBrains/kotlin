// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;

/**
 * Represents a JavaScript expression that references a name.
 */
public final class JsNameRef extends JsExpressionImpl implements HasName {
    private String ident;
    private JsName name;
    private JsExpression qualifier;

    public JsNameRef(JsName name) {
        this.name = name;
    }

    public JsNameRef(String ident) {
        this.ident = ident;
    }

    public JsNameRef(String ident, JsExpression qualifier) {
        this.ident = ident;
        this.qualifier = qualifier;
    }

    public JsNameRef(String ident, String qualifier) {
        this(ident, new JsNameRef(qualifier));
    }

    public JsNameRef(JsName name, JsExpression qualifier) {
        this.name = name;
        this.qualifier = qualifier;
    }

    public String getIdent() {
        return (name == null) ? ident : name.getIdent();
    }

    @Override
    public JsName getName() {
        return name;
    }

    @Override
    public Symbol getSymbol() {
        return name;
    }

    public JsExpression getQualifier() {
        return qualifier;
    }

    @Override
    public boolean isLeaf() {
        return qualifier == null;
    }

    public void resolve(JsName name) {
        this.name = name;
        ident = null;
    }

    public void setQualifier(JsExpression qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitNameRef(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        if (qualifier != null) {
           visitor.accept(qualifier);
        }
    }
}
