// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Used in object literals to specify property values by name.
 */
public class JsPropertyInitializer extends JsNodeImpl {
    private JsExpression labelExpr;
    private JsExpression valueExpr;

    public JsPropertyInitializer(JsExpression labelExpr) {
        this.labelExpr = labelExpr;
    }

    public JsPropertyInitializer(JsExpression labelExpr, JsExpression valueExpr) {
        this(labelExpr);
        this.valueExpr = valueExpr;
    }

    public JsExpression getLabelExpr() {
        return labelExpr;
    }

    public JsExpression getValueExpr() {
        return valueExpr;
    }

    public boolean hasSideEffects() {
        return labelExpr.hasSideEffects() || valueExpr.hasSideEffects();
    }

    public void setValueExpr(JsExpression valueExpr) {
        this.valueExpr = valueExpr;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            labelExpr = v.accept(labelExpr);
            valueExpr = v.accept(valueExpr);
        }
        v.endVisit(this, ctx);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.PROPERTY_INIT;
    }
}
