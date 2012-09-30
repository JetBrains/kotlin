// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript return statement.
 */
public final class JsReturn extends JsNodeImpl implements JsStatement {

  private JsExpression expr;

  public JsReturn() {
  }

  public JsReturn(JsExpression expr) {
    this.expr = expr;
  }

  public JsExpression getExpr() {
    return expr;
  }

  public void setExpr(JsExpression expr) {
    this.expr = expr;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      if (expr != null) {
        expr = v.accept(expr);
      }
    }
    v.endVisit(this, ctx);
  }

    @Override
  public NodeKind getKind() {
    return NodeKind.RETURN;
  }
}
