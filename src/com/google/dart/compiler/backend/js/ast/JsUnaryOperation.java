// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript prefix or postfix operation.
 */
public abstract class JsUnaryOperation extends JsExpressionImpl {

  private JsExpression arg;

  private final JsUnaryOperator op;

  public JsUnaryOperation(JsUnaryOperator op) {
    this(op, null);
  }

  public JsUnaryOperation(JsUnaryOperator op, JsExpression arg) {
    super();
    this.op = op;
    this.arg = arg;
  }

  public JsExpression getArg() {
    return arg;
  }

  public JsUnaryOperator getOperator() {
    return op;
  }

  @Override
  public final boolean hasSideEffects() {
    return op.isModifying() || arg.hasSideEffects();
  }

  public void setArg(JsExpression arg) {
    this.arg = arg;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (op.isModifying()) {
      // The delete operator is practically like an assignment of undefined, so
      // for practical purposes we're treating it as an lvalue.
      arg = v.acceptLvalue(arg);
    } else {
      arg = v.accept(arg);
    }
  }
}
