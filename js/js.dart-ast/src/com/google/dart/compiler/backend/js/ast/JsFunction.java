// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class JsFunction extends JsLiteral implements HasName {
    @NotNull
    private JsBlock body;
    private List<JsParameter> params;
    @NotNull
    private final JsFunctionScope scope;
    private JsName name;

    public JsFunction(@NotNull JsScope parentScope, @NotNull String description) {
        this(parentScope, description, null);
    }

    public JsFunction(@NotNull JsScope parentScope, @NotNull JsBlock body, @NotNull String description) {
        this(parentScope, description, null);
        this.body = body;
    }

    private JsFunction(@NotNull JsScope parentScope, @NotNull String description, @Nullable JsName name) {
        this.name = name;
        scope = new JsFunctionScope(parentScope, name == null ? description : name.getIdent());
    }

    @NotNull
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

    @NotNull
    public List<JsParameter> getParameters() {
        if (params == null) {
            params = new SmartList<JsParameter>();
        }
        return params;
    }

    @NotNull
    public JsFunctionScope getScope() {
        return scope;
    }

    public void setBody(@NotNull JsBlock body) {
        this.body = body;
    }

    public void setName(@Nullable JsName name) {
        this.name = name;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitFunction(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.acceptWithInsertRemove(params);
        visitor.accept(body);
    }
}
