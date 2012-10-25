// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public class JsForIn extends SourceInfoAwareJsNode implements JsStatement {
    private JsStatement body;
    private JsExpression iterExpression;
    private JsExpression objectExpression;

    // Optional: the name of a new iterator variable to introduce
    private final JsName iterVarName;

    public JsForIn() {
        this(null);
    }

    public JsForIn(JsName iterVarName) {
        this.iterVarName = iterVarName;
    }

    public JsStatement getBody() {
        return body;
    }

    public JsExpression getIterExpression() {
        return iterExpression;
    }

    public JsName getIterVarName() {
        return iterVarName;
    }

    public JsExpression getObjectExpression() {
        return objectExpression;
    }

    public void setBody(JsStatement body) {
        this.body = body;
    }

    public void setIterExpression(JsExpression iterExpression) {
        this.iterExpression = iterExpression;
    }

    public void setObjectExpression(JsExpression objectExpression) {
        this.objectExpression = objectExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitForIn(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        if (iterExpression != null) {
            visitor.acceptLvalue(iterExpression);
        }
        visitor.accept(objectExpression);
        visitor.accept(body);
    }
}
