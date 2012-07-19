// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript switch statement.
 */
public class JsSwitch extends JsNodeImpl implements JsStatement {

  private final List<JsSwitchMember> cases = new ArrayList<JsSwitchMember>();
  private JsExpression expr;

  public JsSwitch() {
    super();
  }

  public List<JsSwitchMember> getCases() {
    return cases;
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
      v.acceptWithInsertRemove(cases);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.SWITCH;
  }
}
