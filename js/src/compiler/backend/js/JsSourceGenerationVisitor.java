// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package compiler.backend.js;

import compiler.backend.js.ast.JsContext;
import compiler.backend.js.ast.JsProgramFragment;
import compiler.util.TextOutput;
import compiler.backend.js.ast.JsBlock;
import compiler.backend.js.ast.JsProgram;

/**
 * Generates JavaScript source from an AST.
 */
public class JsSourceGenerationVisitor extends JsToStringGenerationVisitor {

  public JsSourceGenerationVisitor(TextOutput out) {
    super(out);
  }

  @Override
  public boolean visit(JsProgram x, JsContext ctx) {
    // Descend naturally.
    return true;
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext ctx) {
    // Descend naturally.
    return true;
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    printJsBlock(x, false, true);
    return false;
  }
}
