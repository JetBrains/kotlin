// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public class JsThrow extends SourceInfoAwareJsNode implements JsStatement {
    private JsExpression expression;

    public JsThrow() {
    }

    public JsThrow(JsExpression expression) {
        this.expression = expression;
    }

    public JsExpression getExpression() {
        return expression;
    }

    public void setExpression(JsExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitThrow(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(expression);
    }
}
