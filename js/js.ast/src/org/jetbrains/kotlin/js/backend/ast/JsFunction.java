// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.common.Symbol;
import org.jetbrains.kotlin.js.util.AstUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class JsFunction extends JsLiteral implements HasName {
    public enum Modifier { STATIC, GET, SET, GENERATOR }

    @NotNull
    private JsBlock body;
    private List<JsParameter> params;
    @NotNull
    private final JsFunctionScope scope;
    private JsName name;
    private Set<Modifier> modifiers;

    public JsFunction(@NotNull JsScope parentScope, @NotNull String description) {
        this(parentScope, description, null);
    }

    public JsFunction(@NotNull JsScope parentScope, @NotNull JsBlock body, @NotNull String description) {
        this(parentScope, description, null);
        this.body = body;
    }

    private JsFunction(
            @NotNull JsScope parentScope,
            @NotNull String description,
            @Nullable JsName name
    ) {
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

    public boolean isStatic() {
        return modifiers != null && modifiers.contains(Modifier.STATIC);
    }

    public boolean isGetter() {
        return modifiers != null && modifiers.contains(Modifier.GET);
    }

    public boolean isSetter() {
        return modifiers != null && modifiers.contains(Modifier.SET);
    }

    public boolean isGenerator() {
        return modifiers != null && modifiers.contains(Modifier.GENERATOR);
    }

    @NotNull
    public Set<Modifier> getModifiers() {
        if (modifiers == null) {
            modifiers = EnumSet.noneOf(Modifier.class);
        }
        return modifiers;
    }

    public void setBody(@NotNull JsBlock body) {
        this.body = body;
    }

    @Override
    public void setName(@Nullable JsName name) {
        this.name = name;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitFunction(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.acceptWithInsertRemove(getParameters());
        visitor.accept(body);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            v.acceptList(getParameters());
            body = v.acceptStatement(body);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsFunction deepCopy() {
        JsFunction functionCopy = new JsFunction(scope.getParent(), scope.getDescription(), name);
        functionCopy.getScope().copyOwnNames(scope);
        functionCopy.setBody(body.deepCopy());
        functionCopy.params = AstUtil.deepCopy(params);
        functionCopy.modifiers = modifiers == null ? null : EnumSet.copyOf(modifiers);

        return functionCopy.withMetadataFrom(this);
    }
}
