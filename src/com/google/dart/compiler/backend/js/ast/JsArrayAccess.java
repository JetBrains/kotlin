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

    public void setArrayExpression(JsExpression arrayExpression) {
        this.arrayExpression = arrayExpression;
    }

    public void setIndexExpression(JsExpression indexExpression) {
        this.indexExpression = indexExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitArrayAccess(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(arrayExpression);
        visitor.accept(indexExpression);
    }
}
