// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

public final class JsPostfixOperation extends JsUnaryOperation {
  public JsPostfixOperation(JsUnaryOperator op, JsExpression arg) {
    super(op, arg);
  }

  @Override
  public void accept(JsVisitor v) {
    v.visitPostfixOperation(this);
  }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            super.traverse(v, ctx);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsPostfixOperation deepCopy() {
        return new JsPostfixOperation(getOperator(), AstUtil.deepCopy(getArg())).withMetadataFrom(this);
    }
}
