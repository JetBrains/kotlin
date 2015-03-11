// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.NotNull;

/**
 * Used in object literals to specify property values by name.
 */
public class JsPropertyInitializer extends SourceInfoAwareJsNode {
    @NotNull
    private JsExpression labelExpr;
    @NotNull
    private JsExpression valueExpr;

    public JsPropertyInitializer(@NotNull JsExpression labelExpr, @NotNull JsExpression valueExpr) {
        this.labelExpr = labelExpr;
        this.valueExpr = valueExpr;
    }

    @NotNull
    public JsExpression getLabelExpr() {
        return labelExpr;
    }

    @NotNull
    public JsExpression getValueExpr() {
        return valueExpr;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitPropertyInitializer(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(labelExpr);
        visitor.accept(valueExpr);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            JsExpression newLabel = v.accept(labelExpr);
            JsExpression newValue = v.accept(valueExpr);
            assert newLabel != null: "Label cannot be replaced with null";
            assert newValue != null: "Value cannot be replaced with null";
            labelExpr = newLabel;
            valueExpr = newValue;
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsPropertyInitializer deepCopy() {
        return new JsPropertyInitializer(labelExpr.deepCopy(), valueExpr.deepCopy()).withMetadataFrom(this);
    }
}
