// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;

/**
 * Represents a JavaScript label statement.
 */
public class JsLabel extends JsNodeImpl implements JsStatement, HasName {

  private final JsName label;

  private JsStatement stmt;

  public JsLabel(JsName label) {
    this.label = label;
  }

  @Override
  public JsName getName() {
    return label;
  }

  @Override
  public Symbol getSymbol() {
    return label;
  }

  public JsStatement getStmt() {
    return stmt;
  }

  public void setStmt(JsStatement stmt) {
    this.stmt = stmt;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      stmt = v.accept(stmt);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.LABEL;
  }
}
