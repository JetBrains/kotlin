// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents the JavaScript break statement.
 */
public final class JsBreak extends JsStatement {

  private final JsNameRef label;

  public JsBreak() {
    this(null);
  }

  public JsBreak(JsNameRef label) {
    super();
    this.label = label;
  }

  public JsNameRef getLabel() {
    return label;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      if (label != null) {
        v.accept(label);
      }
    }
    v.endVisit(this, ctx);
  }

  @Override
  public boolean unconditionalControlBreak() {
    return true;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.BREAK;
  }
}
