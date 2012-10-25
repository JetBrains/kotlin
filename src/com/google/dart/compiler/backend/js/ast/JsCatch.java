// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript catch clause.
 */
public class JsCatch extends JsNodeImpl implements HasCondition {

    protected final JsCatchScope scope;
    private JsBlock body;
    private JsExpression condition;
    private JsParameter param;

    public JsCatch(JsScope parent, String ident) {
        super();
        assert (parent != null);
        scope = new JsCatchScope(parent, ident);
        param = new JsParameter(scope.findName(ident));
    }

    public JsBlock getBody() {
        return body;
    }

    @Override
    public JsExpression getCondition() {
        return condition;
    }

    public JsParameter getParameter() {
        return param;
    }

    public JsScope getScope() {
        return scope;
    }

    public void setBody(JsBlock body) {
        this.body = body;
    }

    @Override
    public void setCondition(JsExpression condition) {
        this.condition = condition;
    }

    @Override
    public void accept(JsVisitor v, JsContext context) {
        v.visitCatch(this, context);
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
        param = visitor.accept(param);
        if (condition != null) {
            condition = visitor.accept(condition);
        }
        body = visitor.accept(body);
    }
}
