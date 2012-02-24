// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

/**
 * A <code>for</code> statement. If specified at all, the initializer part is
 * either a declaration of one or more variables, in which case
 * {@link #getInitVars()} is used, or an expression, in which case
 * {@link #getInitExpr()} is used. In the latter case, the comma operator is
 * often used to create a compound expression.
 *
 * <p>
 * Note that any of the parts of the <code>for</code> loop header can be
 * <code>null</code>, although the body will never be null.
 */
public class JsFor extends JsStatement {

  private JsStatement body;
  private JsExpression condition;
  private JsExpression incrExpr;
  private JsExpression initExpr;
  private JsVars initVars;

  public JsFor() {
    super();
  }

  public JsStatement getBody() {
    return body;
  }

  public JsExpression getCondition() {
    return condition;
  }

  public JsExpression getIncrExpr() {
    return incrExpr;
  }

  public JsExpression getInitExpr() {
    return initExpr;
  }

  public JsVars getInitVars() {
    return initVars;
  }

  public void setBody(JsStatement body) {
    this.body = body;
  }

  public void setCondition(JsExpression condition) {
    this.condition = condition;
  }

  public void setIncrExpr(JsExpression incrExpr) {
    this.incrExpr = incrExpr;
  }

  public void setInitExpr(JsExpression initExpr) {
    this.initExpr = initExpr;
  }

  public void setInitVars(JsVars initVars) {
    this.initVars = initVars;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      assert (!(initExpr != null && initVars != null));

      if (initExpr != null) {
        initExpr = v.accept(initExpr);
      } else if (initVars != null) {
        initVars = v.accept(initVars);
      }

      if (condition != null) {
        condition = v.accept(condition);
      }

      if (incrExpr != null) {
        incrExpr = v.accept(incrExpr);
      }
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public JsFor setSourceRef(SourceInfo info) {
    super.setSourceRef(info);
    return this;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.FOR;
  }
}
