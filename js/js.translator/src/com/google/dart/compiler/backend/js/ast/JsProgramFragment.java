// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * One independently loadable fragment of a {@link JsProgram}.
 */
public class JsProgramFragment extends JsNodeImpl {

  private final JsGlobalBlock globalBlock;

  public JsProgramFragment() {
    this.globalBlock = new JsGlobalBlock();
  }

  public JsBlock getGlobalBlock() {
    return globalBlock;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.accept(globalBlock);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PROGRAM_FRAGMENT;
  }
}
