// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript <code>throw</code> statement.
 */
public class JsThrow extends JsStatement {

  private JsExpression expr;

  public JsThrow() {
    super();
  }

  public JsThrow(JsExpression expr) {
    super();
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
      expr = v.accept(expr);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public boolean unconditionalControlBreak() {
    return true;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.THROW;
  }
}
