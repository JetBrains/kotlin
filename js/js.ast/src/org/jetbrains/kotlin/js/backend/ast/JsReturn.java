// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * A JavaScript return statement.
 */
public final class JsReturn extends SourceInfoAwareJsNode implements JsStatement {
    private JsExpression expression;

    public JsReturn() {
    }

    public JsReturn(JsExpression expression) {
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
        v.visitReturn(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        if (expression != null) {
            visitor.accept(expression);
        }
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            if (expression != null) {
                expression = v.accept(expression);
            }
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsReturn deepCopy() {
        return new JsReturn(AstUtil.deepCopy(expression)).withMetadataFrom(this);
    }
}
