// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;

import java.util.List;

public final class JsNew extends JsExpressionImpl.JsExpressionHasArguments {
    private JsExpression constructorExpression;

    public JsNew(JsExpression constructorExpression) {
        this(constructorExpression, new SmartList<JsExpression>());
    }

    public JsNew(JsExpression constructorExpression, List<JsExpression> arguments) {
        super(arguments);
        this.constructorExpression = constructorExpression;
    }

    public JsExpression getConstructorExpression() {
        return constructorExpression;
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
    public void accept(JsVisitor v, JsContext context) {
        v.visitNew(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        constructorExpression = visitor.accept(constructorExpression);
        visitor.acceptList(arguments);
    }
}
