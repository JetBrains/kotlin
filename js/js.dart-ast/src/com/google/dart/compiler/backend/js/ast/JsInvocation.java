// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.util.AstUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class JsInvocation extends JsExpressionImpl.JsExpressionHasArguments {
    private JsExpression qualifier;

    public JsInvocation() {
        super(new SmartList<JsExpression>());
    }

    public JsInvocation(JsExpression qualifier, @NotNull List<JsExpression> arguments) {
        super(arguments);
        this.qualifier = qualifier;
    }

    public JsInvocation(JsExpression qualifier, JsExpression arg) {
        this(qualifier, new SmartList<JsExpression>(arg));
    }

    public JsInvocation(JsExpression qualifier, JsExpression... arguments) {
        this(qualifier, Arrays.asList(arguments));
    }

    public JsInvocation(JsExpression qualifier) {
        this();
        this.qualifier = qualifier;
    }

    @NotNull
    @Override
    public List<JsExpression> getArguments() {
        return arguments;
    }

    public JsExpression getQualifier() {
        return qualifier;
    }

    public void setQualifier(JsExpression qualifier) {
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
