// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript postfix operation.
 */
public final class JsPostfixOperation extends JsUnaryOperation {

  public JsPostfixOperation(JsUnaryOperator op) {
    this(op, null);
  }

  public JsPostfixOperation(JsUnaryOperator op, JsExpression arg) {
    super(op, arg);
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      super.traverse(v, ctx);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.POSTFIX_OP;
  }
}
