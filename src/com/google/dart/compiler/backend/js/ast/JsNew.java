// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the JavaScript new expression.
 */
public final class JsNew extends JsExpression implements HasArguments {

  private final List<JsExpression> args = new ArrayList<JsExpression>();
  private JsExpression ctorExpr;

  public JsNew(JsExpression ctorExpr) {
    this.ctorExpr = ctorExpr;
  }

  @Override
  public List<JsExpression> getArguments() {
    return args;
  }

  public JsExpression getConstructorExpression() {
    return ctorExpr;
  }

  @Override
  public boolean hasSideEffects() {
    return true;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    // Sadly, in JS it can be!
    // TODO: analysis could probably determine most instances cannot be null.
    return false;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      ctorExpr = v.accept(ctorExpr);
      v.acceptList(args);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.NEW;
  }
}
