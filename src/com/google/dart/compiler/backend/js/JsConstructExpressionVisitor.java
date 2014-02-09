// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.*;

/**
 * Searches for method invocations in constructor expressions that would not
 * normally be surrounded by parentheses.
 */
public class JsConstructExpressionVisitor extends JsVisitor {

  public static boolean exec(JsExpression expression) {
    if (JsPrecedenceVisitor.exec(expression) < JsPrecedenceVisitor.PRECEDENCE_NEW) {
      return true;
    }
    JsConstructExpressionVisitor visitor = new JsConstructExpressionVisitor();
    visitor.accept(expression);
    return visitor.containsInvocation;
  }

  private boolean containsInvocation = false;

  private JsConstructExpressionVisitor() {
  }

  /**
   * We only look at the array expression since the index has its own scope.
   */
  @Override
  public boolean visit(JsArrayAccess x, JsContext ctx) {
    accept(x.getArrayExpr());
    return false;
  }

  /**
   * Array literals have their own scoping.
   */
  @Override
  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    return false;
  }

  /**
   * Functions have their own scoping.
   */
  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsInvocation x, JsContext ctx) {
    containsInvocation = true;
    return false;
  }

  @Override
  public boolean visit(JsNameRef x, JsContext ctx) {
    if (!x.isLeaf()) {
      accept(x.getQualifier());
    }
    return false;
  }

  /**
   * New constructs bind to the nearest set of parentheses.
   */
  @Override
  public boolean visit(JsNew x, JsContext ctx) {
    return false;
  }

  /**
   * Object literals have their own scope.
   */
  @Override
  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    return false;
  }

  /**
   * We only look at nodes that would not normally be surrounded by parentheses.
   */
  @Override
  protected <T extends JsVisitable> T doAccept(T node) {
    // Assign to Object to prevent 'inconvertible types' compile errors due
    // to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6548436
    // reproducible in jdk1.6.0_02.
    Object o = node;
    if (o instanceof JsExpression) {
      JsExpression expression = (JsExpression) o;
      int precedence = JsPrecedenceVisitor.exec(expression);
      // Only visit expressions that won't automatically be surrounded by
      // parentheses
      if (precedence < JsPrecedenceVisitor.PRECEDENCE_NEW) {
        return node;
      }
    }
    return super.doAccept(node);
  }
}
