// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.common.Symbol;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class JsFunction extends JsLiteral implements HasName {
    private JsBlock body;
    private List<JsParameter> params;
    private final JsScope scope;
    private JsName name;

    public JsFunction(JsScope parentScope) {
        this(parentScope, (JsName) null);
    }

    public JsFunction(JsScope parentScope, JsBlock body) {
        this(parentScope, (JsName) null);
        this.body = body;
    }

    private JsFunction(JsScope parentScope, @Nullable JsName name) {
        this.name = name;
        this.scope = new JsScope(parentScope, name == null ? null : name.getIdent());
    }

    public JsBlock getBody() {
        return body;
    }

    @Override
    public JsName getName() {
        return name;
    }

    @Override
    public Symbol getSymbol() {
        return name;
    }

    public List<JsParameter> getParameters() {
        if (params == null) {
            params = new SmartList<JsParameter>();
        }
        return params;
    }

    public JsScope getScope() {
        return scope;
    }

    @Override
    public boolean hasSideEffects() {
        // If there's a name, the name is assigned to.
        return name != null;
    }

    @Override
    public boolean isBooleanFalse() {
        return false;
    }

    @Override
    public boolean isBooleanTrue() {
        return true;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return true;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    public void setBody(JsBlock body) {
        this.body = body;
    }

    public void setName(@Nullable JsName name) {
        this.name = name;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptWithInsertRemove(params);
            body = v.accept(body);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public JsFunction setSourceRef(SourceInfo info) {
        super.setSourceRef(info);
        return this;
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.FUNCTION;
    }
}
