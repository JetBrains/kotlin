// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package compiler.backend.js;

import compiler.backend.js.ast.*;

/**
 * Determines if an expression statement needs to be surrounded by parentheses.
 * 
 * The statement or the left-most expression needs to be surrounded by
 * parentheses if the left-most expression is an object literal or a function
 * object. Function declarations do not need parentheses.
 * 
 * For example the following require parentheses:<br>
 * <ul>
 * <li>{ key : 'value'}</li>
 * <li>{ key : 'value'}.key</li>
 * <li>function () {return 1;}()</li>
 * <li>function () {return 1;}.prototype</li>
 * </ul>
 * 
 * The following do not require parentheses:<br>
 * <ul>
 * <li>var x = { key : 'value'}</li>
 * <li>"string" + { key : 'value'}.key</li>
 * <li>function func() {}</li>
 * <li>function() {}</li>
 * </ul>
 */
public class JsFirstExpressionVisitor extends JsVisitor {

  public static boolean exec(JsExprStmt statement) {
    JsFirstExpressionVisitor visitor = new JsFirstExpressionVisitor();
    JsExpression expression = statement.getExpression();
    // Pure function declarations do not need parentheses
    if (expression instanceof JsFunction) {
      return false;
    }
    visitor.accept(statement.getExpression());
    return visitor.needsParentheses;
  }

  private boolean needsParentheses = false;

  private JsFirstExpressionVisitor() {
  }

  @Override
  public boolean visit(JsArrayAccess x, JsContext ctx) {
    accept(x.getArrayExpr());
    return false;
  }

  @Override
  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsBinaryOperation x, JsContext ctx) {
    accept(x.getArg1());
    return false;
  }

  @Override
  public boolean visit(JsConditional x, JsContext ctx) {
    accept(x.getTestExpression());
    return false;
  }

  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    needsParentheses = true;
    return false;
  }

  @Override
  public boolean visit(JsInvocation x, JsContext ctx) {
    accept(x.getQualifier());
    return false;
  }

  @Override
  public boolean visit(JsNameRef x, JsContext ctx) {
    if (!x.isLeaf()) {
      accept(x.getQualifier());
    }
    return false;
  }

  @Override
  public boolean visit(JsNew x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    needsParentheses = true;
    return false;
  }

  @Override
  public boolean visit(JsPostfixOperation x, JsContext ctx) {
    accept(x.getArg());
    return false;
  }

  @Override
  public boolean visit(JsPrefixOperation x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsRegExp x, JsContext ctx) {
    return false;
  }
}
