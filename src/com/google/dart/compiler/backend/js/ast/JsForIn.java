// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript for..in statement.
 */
public class JsForIn extends JsNodeImpl implements JsStatement {

  private JsStatement body;
  private JsExpression iterExpr;
  private JsExpression objExpr;

  // Optional: the name of a new iterator variable to introduce
  private final JsName iterVarName;

  public JsForIn() {
    this(null);
  }

  public JsForIn(JsName iterVarName) {
    this.iterVarName = iterVarName;
  }

  public JsStatement getBody() {
    return body;
  }

  public JsExpression getIterExpr() {
    return iterExpr;
  }

  public JsName getIterVarName() {
    return iterVarName;
  }

  public JsExpression getObjExpr() {
    return objExpr;
  }

  public void setBody(JsStatement body) {
    this.body = body;
  }

  public void setIterExpr(JsExpression iterExpr) {
    this.iterExpr = iterExpr;
  }

  public void setObjExpr(JsExpression objExpr) {
    this.objExpr = objExpr;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      if (iterExpr != null) {
        iterExpr = v.acceptLvalue(iterExpr);
      }
      objExpr = v.accept(objExpr);
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.FOR_IN;
  }
}
