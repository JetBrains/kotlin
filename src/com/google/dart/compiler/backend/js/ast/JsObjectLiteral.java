// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript object literal.
 */
public final class JsObjectLiteral extends JsLiteral {

  private final List<JsPropertyInitializer> props = new ArrayList<JsPropertyInitializer>();

  public JsObjectLiteral() {
  }

  public List<JsPropertyInitializer> getPropertyInitializers() {
    return props;
  }

  @Override
  public boolean hasSideEffects() {
    for (JsPropertyInitializer prop : props) {
      if (prop.hasSideEffects()) {
        return true;
      }
    }
    return false;
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
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(props);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.OBJECT;
  }
}
