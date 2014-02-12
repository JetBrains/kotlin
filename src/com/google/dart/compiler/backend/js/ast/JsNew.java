// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;

import java.util.List;

/**
 * Represents the JavaScript new expression.
 */
public final class JsNew extends JsExpressionImpl implements HasArguments {
    private final List<JsExpression> arguments;
    private JsExpression ctorExpression;

    public JsNew(JsExpression ctorExpression) {
        this(ctorExpression, new SmartList<JsExpression>());
    }

    public JsNew(JsExpression ctorExpression, List<JsExpression> arguments) {
        this.ctorExpression = ctorExpression;
        this.arguments = arguments;
    }

    @Override
    public List<JsExpression> getArguments() {
        return arguments;
    }

    public JsExpression getConstructorExpression() {
        return ctorExpression;
    }

    @Override
    public boolean hasSideEffects() {
        return true;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        // Sadly, in JS it can be!
        // TODO: analysis could probably determine most instances cannot be null.
        return false;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            ctorExpression = v.accept(ctorExpression);
            v.acceptList(arguments);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.NEW;
    }
}
