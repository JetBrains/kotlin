// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript string literal expression.
 */
public final class JsStringLiteral extends JsValueLiteral {

  private final String value;

  // These only get created by JsProgram so that they can be interned.
  JsStringLiteral(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean isBooleanFalse() {
    return value.length() == 0;
  }

  @Override
  public boolean isBooleanTrue() {
    return value.length() != 0;
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
    return NodeKind.STRING;
  }
}
