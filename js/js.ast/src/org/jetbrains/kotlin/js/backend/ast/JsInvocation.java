// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class JsInvocation extends JsExpression.JsExpressionHasArguments {
    @NotNull
    private JsExpression qualifier;

    public JsInvocation(@NotNull JsExpression qualifier, @NotNull List<? extends JsExpression> arguments) {
        super(new SmartList<JsExpression>(arguments));
        this.qualifier = qualifier;
    }

    public JsInvocation(@NotNull JsExpression qualifier, JsExpression... arguments) {
        this(qualifier, new SmartList<JsExpression>(arguments));
    }

    @NotNull
    @Override
    public List<JsExpression> getArguments() {
        return arguments;
    }

    @NotNull
    public JsExpression getQualifier() {
        return qualifier;
    }

    public void setQualifier(@NotNull JsExpression qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitInvocation(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(qualifier);
        visitor.acceptList(arguments);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            qualifier = v.accept(qualifier);
            v.acceptList(arguments);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsInvocation deepCopy() {
        JsExpression qualifierCopy = AstUtil.deepCopy(qualifier);
        List<JsExpression> argumentsCopy = AstUtil.deepCopy(arguments);
        return new JsInvocation(qualifierCopy, argumentsCopy).withMetadataFrom(this);
    }
}
