// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents the JavaScript case statement.
 */
public final class JsCase extends JsSwitchMember {

  private JsExpression caseExpr;

  public JsCase() {
    super();
  }

  public JsExpression getCaseExpr() {
    return caseExpr;
  }

  public void setCaseExpr(JsExpression caseExpr) {
    this.caseExpr = caseExpr;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      caseExpr = v.accept(caseExpr);
      v.acceptWithInsertRemove(statements);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.CASE;
  }
}
