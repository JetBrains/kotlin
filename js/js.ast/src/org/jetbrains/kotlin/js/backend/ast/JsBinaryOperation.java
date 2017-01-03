// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JsBinaryOperation extends JsExpression {
    private JsExpression arg1;
    private JsExpression arg2;

    @NotNull
    private final JsBinaryOperator op;

    public JsBinaryOperation(@NotNull JsBinaryOperator op, @Nullable JsExpression arg1, @Nullable JsExpression arg2) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public JsExpression getArg1() {
        return arg1;
    }

    public JsExpression getArg2() {
        return arg2;
    }

    public void setArg1(JsExpression arg1) {
        this.arg1 = arg1;
    }

    public void setArg2(JsExpression arg2) {
        this.arg2 = arg2;
    }

    @NotNull
    public JsBinaryOperator getOperator() {
        return op;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitBinaryExpression(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        if (op.isAssignment()) {
            visitor.acceptLvalue(arg1);
        }
        else {
            visitor.accept(arg1);
        }
        visitor.accept(arg2);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            if (op.isAssignment()) {
                arg1 = v.acceptLvalue(arg1);
            } else {
                arg1 = v.accept(arg1);
            }
            arg2 = v.accept(arg2);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsExpression deepCopy() {
        return new JsBinaryOperation(op, AstUtil.deepCopy(arg1), AstUtil.deepCopy(arg2)).withMetadataFrom(this);
    }
}
