// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript prefix operation.
 */
public final class JsPrefixOperation extends JsUnaryOperation implements CanBooleanEval {

  public JsPrefixOperation(JsUnaryOperator op) {
    this(op, null);
  }

  public JsPrefixOperation(JsUnaryOperator op, JsExpression arg) {
    super(op, arg);
  }

  @Override
  public boolean isBooleanFalse() {
    if (getOperator() == JsUnaryOperator.VOID) {
      return true;
    }
    if (getOperator() == JsUnaryOperator.NOT && getArg() instanceof CanBooleanEval) {
      CanBooleanEval eval = (CanBooleanEval) getArg();
      return eval.isBooleanTrue();
    }
    return false;
  }

  @Override
  public boolean isBooleanTrue() {
    if (getOperator() == JsUnaryOperator.NOT && getArg() instanceof CanBooleanEval) {
      CanBooleanEval eval = (CanBooleanEval) getArg();
      return eval.isBooleanFalse();
    }
    if (getOperator() == JsUnaryOperator.TYPEOF) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    if (getOperator() == JsUnaryOperator.TYPEOF) {
      return true;
    }
    return getOperator() != JsUnaryOperator.VOID;
  }

  @Override
  public boolean isDefinitelyNull() {
    return getOperator() == JsUnaryOperator.VOID;
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
    return NodeKind.PREFIX_OP;
  }
}
