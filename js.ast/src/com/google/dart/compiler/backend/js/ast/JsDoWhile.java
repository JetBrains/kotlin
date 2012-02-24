// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript do..while statement.
 */
public class JsDoWhile extends JsStatement {

  private JsStatement body;
  private JsExpression condition;

  public JsDoWhile() {
    super();
  }

  public JsDoWhile(JsExpression condition, JsStatement body) {
    super();
    this.condition = condition;
    this.body = body;
  }

  public JsStatement getBody() {
    return body;
  }

  public JsExpression getCondition() {
    return condition;
  }

  public void setBody(JsStatement body) {
    this.body = body;
  }

  public void setCondition(JsExpression condition) {
    this.condition = condition;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      body = v.accept(body);
      condition = v.accept(condition);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.DO;
  }
}
