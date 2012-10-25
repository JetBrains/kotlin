// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

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
      return getOperator() == JsUnaryOperator.TYPEOF;
  }

  @Override
  public boolean isDefinitelyNotNull() {
      return getOperator() == JsUnaryOperator.TYPEOF || getOperator() != JsUnaryOperator.VOID;
  }

  @Override
  public boolean isDefinitelyNull() {
    return getOperator() == JsUnaryOperator.VOID;
  }

  @Override
  public void accept(JsVisitor v, JsContext context) {
    v.visit(this, context);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PREFIX_OP;
  }
}
