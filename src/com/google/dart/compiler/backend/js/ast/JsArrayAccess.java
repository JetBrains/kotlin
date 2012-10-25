// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a javascript expression for array access.
 */
public final class JsArrayAccess extends JsExpressionImpl {
    private JsExpression arrayExpression;
    private JsExpression indexExpression;

    public JsArrayAccess() {
        super();
    }

    public JsArrayAccess(JsExpression arrayExpression, JsExpression indexExpression) {
        this.arrayExpression = arrayExpression;
        this.indexExpression = indexExpression;
    }

    public JsExpression getArrayExpression() {
        return arrayExpression;
    }

    public JsExpression getIndexExpression() {
        return indexExpression;
    }

    @Override
    public boolean hasSideEffects() {
        return arrayExpression.hasSideEffects() || indexExpression.hasSideEffects();
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return false;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    public void setArrayExpression(JsExpression arrayExpression) {
        this.arrayExpression = arrayExpression;
    }

    public void setIndexExpression(JsExpression indexExpression) {
        this.indexExpression = indexExpression;
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visit(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        arrayExpression = visitor.accept(arrayExpression);
        indexExpression = visitor.accept(indexExpression);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.ARRAY_ACCESS;
    }
}
