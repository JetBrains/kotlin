// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript <code>try</code> statement.
 */
public class JsTry extends JsStatement {

  private final List<JsCatch> catches = new ArrayList<JsCatch>();
  private JsBlock finallyBlock;
  private JsBlock tryBlock;

  public JsTry() {
    super();
  }

  public List<JsCatch> getCatches() {
    return catches;
  }

  public JsBlock getFinallyBlock() {
    return finallyBlock;
  }

  public JsBlock getTryBlock() {
    return tryBlock;
  }

  public void setFinallyBlock(JsBlock block) {
    this.finallyBlock = block;
  }

  public void setTryBlock(JsBlock block) {
    tryBlock = block;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      tryBlock = v.accept(tryBlock);
      v.acceptWithInsertRemove(catches);
      if (finallyBlock != null) {
        finallyBlock = v.accept(finallyBlock);
      }
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.TRY;
  }
}
