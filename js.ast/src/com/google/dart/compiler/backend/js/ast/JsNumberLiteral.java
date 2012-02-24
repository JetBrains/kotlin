// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript literal decimal expression.
 */
public final class JsNumberLiteral extends JsValueLiteral {

  private final double value;

  // Should be interned by JsProgram
  JsNumberLiteral(double value) {
    this.value = value;
  }

  public double getValue() {
    return value;
  }

  @Override
  public boolean isBooleanFalse() {
    return value == 0.0;
  }

  @Override
  public boolean isBooleanTrue() {
    return value != 0.0;
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
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.NUMBER;
  }
}
