// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class JsNew extends JsExpression.JsExpressionHasArguments {
    private JsExpression constructorExpression;

    public JsNew(JsExpression constructorExpression) {
        this(constructorExpression, new SmartList<JsExpression>());
    }

    public JsNew(JsExpression constructorExpression, List<? extends JsExpression> arguments) {
        super(new SmartList<JsExpression>(arguments));
        this.constructorExpression = constructorExpression;
    }

    public JsNew(JsExpression constructorExpression, JsExpression... arguments) {
        this(constructorExpression, new SmartList<JsExpression>(arguments));
    }

    public JsExpression getConstructorExpression() {
        return constructorExpression;
    }

    public void setConstructorExpression(JsExpression constructorExpression) {
        this.constructorExpression = constructorExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitNew(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(constructorExpression);
        visitor.acceptList(arguments);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            constructorExpression = v.accept(constructorExpression);
            v.acceptList(arguments);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsNew deepCopy() {
        JsExpression constructorCopy = AstUtil.deepCopy(constructorExpression);
        List<JsExpression> argumentsCopy = AstUtil.deepCopy(arguments);
        return new JsNew(constructorCopy, argumentsCopy).withMetadataFrom(this);
    }
}
