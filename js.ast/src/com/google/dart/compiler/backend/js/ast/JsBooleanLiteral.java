// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents a JavaScript literal boolean expression.
 */
public final class JsBooleanLiteral extends JsValueLiteral {

  private final boolean value;

  // Should be interned by JsProgram
  JsBooleanLiteral(boolean value) {
    this.value = value;
  }

  public boolean getValue() {
    return value;
  }

  @Override
  public boolean isBooleanFalse() {
    return value == false;
  }

  @Override
  public boolean isBooleanTrue() {
    return value == true;
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
    return NodeKind.BOOLEAN;
  }
}
