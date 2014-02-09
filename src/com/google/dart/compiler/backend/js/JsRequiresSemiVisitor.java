// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsBreak;
import com.google.dart.compiler.backend.js.ast.JsContext;
import com.google.dart.compiler.backend.js.ast.JsDebugger;
import com.google.dart.compiler.backend.js.ast.JsDoWhile;
import com.google.dart.compiler.backend.js.ast.JsEmpty;
import com.google.dart.compiler.backend.js.ast.JsExprStmt;
import com.google.dart.compiler.backend.js.ast.JsFor;
import com.google.dart.compiler.backend.js.ast.JsForIn;
import com.google.dart.compiler.backend.js.ast.JsIf;
import com.google.dart.compiler.backend.js.ast.JsLabel;
import com.google.dart.compiler.backend.js.ast.JsReturn;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.backend.js.ast.JsSwitch;
import com.google.dart.compiler.backend.js.ast.JsThrow;
import com.google.dart.compiler.backend.js.ast.JsTry;
import com.google.dart.compiler.backend.js.ast.JsVars;
import com.google.dart.compiler.backend.js.ast.JsVisitor;
import com.google.dart.compiler.backend.js.ast.JsWhile;

/**
 * Determines if a statement at the end of a block requires a semicolon.
 * 
 * For example, the following statements require semicolons:<br>
 * <ul>
 * <li>if (cond);</li>
 * <li>while (cond);</li>
 * </ul>
 * 
 * The following do not require semicolons:<br>
 * <ul>
 * <li>return 1</li>
 * <li>do {} while(true)</li>
 * </ul>
 */
public class JsRequiresSemiVisitor extends JsVisitor {

  public static boolean exec(JsStatement lastStatement) {
    JsRequiresSemiVisitor visitor = new JsRequiresSemiVisitor();
    visitor.accept(lastStatement);
    return visitor.needsSemicolon;
  }

  private boolean needsSemicolon = false;

  private JsRequiresSemiVisitor() {
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsBreak x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsDebugger x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsDoWhile x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsEmpty x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsFor x, JsContext ctx) {
    if (x.getBody() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

  @Override
  public boolean visit(JsForIn x, JsContext ctx) {
    if (x.getBody() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

  @Override
  public boolean visit(JsIf x, JsContext ctx) {
    JsStatement thenStmt = x.getThenStatement();
    JsStatement elseStmt = x.getElseStatement();
    JsStatement toCheck = thenStmt;
    if (elseStmt != null) {
      toCheck = elseStmt;
    }
    if (toCheck instanceof JsEmpty) {
      needsSemicolon = true;
    } else {
      // Must recurse to determine last statement (possible if-else chain).
      accept(toCheck);
    }
    return false;
  }

  @Override
  public boolean visit(JsLabel x, JsContext ctx) {
    if (x.getStmt() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }

  @Override
  public boolean visit(JsReturn x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsSwitch x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsThrow x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsTry x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsVars x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsWhile x, JsContext ctx) {
    if (x.getBody() instanceof JsEmpty) {
      needsSemicolon = true;
    }
    return false;
  }
}
