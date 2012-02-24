// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript <code>this</code> reference.
 */
public final class JsThisRef extends JsValueLiteral {

  public JsThisRef() {
    super();
  }

  @Override
  public boolean isBooleanFalse() {
    return false;
  }

  @Override
  public boolean isBooleanTrue() {
    return true;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    /*
     * You'd think that you could get a null this via function.call/apply, but
     * in fact you can't: they just make this be the window object instead. So
     * it really can't ever be null.
     */
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
    return NodeKind.THIS;
  }
}
