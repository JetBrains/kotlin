// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript invocation.
 */
public final class JsInvocation extends JsExpression implements HasArguments {

  private final List<JsExpression> args = new ArrayList<JsExpression>();
  private JsExpression qualifier;

  public JsInvocation() {
  }

  @Override
  public List<JsExpression> getArguments() {
    return args;
  }

  public JsExpression getQualifier() {
    return qualifier;
  }

  @Override
  public boolean hasSideEffects() {
    return true;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return false;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  public void setQualifier(JsExpression qualifier) {
    this.qualifier = qualifier;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      qualifier = v.accept(qualifier);
      v.acceptList(args);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.INVOKE;
  }
}
