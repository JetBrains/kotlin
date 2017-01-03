// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.common.Symbol;
import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a JavaScript expression that references a name.
 */
public final class JsNameRef extends JsExpression implements HasName {
    private String ident;
    private JsName name;
    private JsExpression qualifier;

    public JsNameRef(@NotNull JsName name) {
        this.name = name;
    }

    public JsNameRef(@NotNull String ident) {
        this.ident = ident;
    }

    public JsNameRef(@NotNull String ident, JsExpression qualifier) {
        this.ident = ident;
        this.qualifier = qualifier;
    }

    public JsNameRef(@NotNull String ident, @NotNull String qualifier) {
        this(ident, new JsNameRef(qualifier));
    }

    public JsNameRef(@NotNull JsName name, JsExpression qualifier) {
        this.name = name;
        this.qualifier = qualifier;
    }

    @NotNull
    public String getIdent() {
        return (name == null) ? ident : name.getIdent();
    }

    @Nullable
    @Override
    public JsName getName() {
        return name;
    }

    @Override
    public void setName(JsName name) {
        this.name = name;
    }

    @Nullable
    @Override
    public Symbol getSymbol() {
        return name;
    }

    @Nullable
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

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            if (qualifier != null) {
                qualifier = v.accept(qualifier);
            }
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsNameRef deepCopy() {
        JsExpression qualifierCopy = AstUtil.deepCopy(qualifier);

        if (name != null) return new JsNameRef(name, qualifierCopy).withMetadataFrom(this);

        return new JsNameRef(ident, qualifierCopy).withMetadataFrom(this);
    }
}
