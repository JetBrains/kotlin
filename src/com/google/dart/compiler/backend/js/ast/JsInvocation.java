// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a JavaScript invocation.
 */
public final class JsInvocation extends JsExpressionImpl implements HasArguments {
    private final List<JsExpression> arguments;
    private JsExpression qualifier;

    public JsInvocation() {
        arguments = new SmartList<JsExpression>();
    }

    public JsInvocation(JsExpression qualifier, List<JsExpression> arguments) {
        this.qualifier = qualifier;
        this.arguments = arguments;
    }

    public JsInvocation(JsExpression qualifier, JsExpression arg) {
        this(qualifier, Collections.singletonList(arg));
    }

    public JsInvocation(JsExpression qualifier, JsExpression... arguments) {
        this(qualifier, Arrays.asList(arguments));
    }

    public JsInvocation(JsExpression qualifier) {
        this();
        this.qualifier = qualifier;
    }

    @Override
    public List<JsExpression> getArguments() {
        return arguments;
    }

    public JsExpression getQualifier() {
        return qualifier;
    }

    @Override
    public boolean hasSideEffects() {
        return true;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return false;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    public void setQualifier(JsExpression qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            qualifier = v.accept(qualifier);
            v.acceptList(arguments);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.INVOKE;
    }
}
