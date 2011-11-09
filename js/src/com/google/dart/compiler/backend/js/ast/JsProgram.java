// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.HashMap;
import java.util.Map;

/**
 * A JavaScript program.
 */
public final class JsProgram extends JsNode {

  private final JsStatement debuggerStmt;
  private final JsEmpty emptyStmt;
  private final JsBooleanLiteral falseLiteral;
  private JsProgramFragment[] fragments;
  private final Map<String, JsFunction> indexedFunctions = new HashMap<String, JsFunction>();
  private final JsNullLiteral nullLiteral;
  private final Map<Double, JsNumberLiteral> numberLiteralMap =
      new HashMap<Double, JsNumberLiteral>();
  private final JsScope objectScope;
  private final JsRootScope rootScope;
  private final Map<String, JsStringLiteral> stringLiteralMap =
      new HashMap<String, JsStringLiteral>();
  private final JsScope topScope;
  private final JsBooleanLiteral trueLiteral;

  /**
   * Constructs a JavaScript program object.
   */
  public JsProgram(String unitId) {
    rootScope = new JsRootScope(this);
    topScope = new JsScope(rootScope, "Global", unitId);
    objectScope = new JsScope(rootScope, "Object");
    setFragmentCount(1);

    debuggerStmt = new JsDebugger();
    emptyStmt = new JsEmpty();
    falseLiteral = new JsBooleanLiteral(false);
    nullLiteral = new JsNullLiteral();
    trueLiteral = new JsBooleanLiteral(true);
  }

  public JsBooleanLiteral getBooleanLiteral(boolean truth) {
    if (truth) {
      return getTrueLiteral();
    }
    return getFalseLiteral();
  }

  /**
   * Gets the {@link JsStatement} to use whenever parsed source include a
   * <code>debugger</code> statement.
   */
  public JsStatement getDebuggerStmt() {
    return debuggerStmt;
  }

  public JsEmpty getEmptyStmt() {
    return emptyStmt;
  }

  public JsBooleanLiteral getFalseLiteral() {
    return falseLiteral;
  }

  public JsBlock getFragmentBlock(int fragment) {
    if (fragment < 0 || fragment >= fragments.length) {
      throw new IllegalArgumentException("Invalid fragment: " + fragment);
    }
    return fragments[fragment].getGlobalBlock();
  }

  public int getFragmentCount() {
    return this.fragments.length;
  }

  /**
   * Gets the one and only global block.
   */
  public JsBlock getGlobalBlock() {
    return getFragmentBlock(0);
  }

  public JsFunction getIndexedFunction(String name) {
    return indexedFunctions.get(name);
  }

  public JsNullLiteral getNullLiteral() {
    return nullLiteral;
  }

  public JsNumberLiteral getNumberLiteral(double value) {
    JsNumberLiteral lit = numberLiteralMap.get(value);
    if (lit == null) {
      lit = new JsNumberLiteral(value);
      numberLiteralMap.put(value, lit);
    }

    return lit;
  }

  public JsScope getObjectScope() {
    return objectScope;
  }

  /**
   * Gets the quasi-mythical root scope. This is not the same as the top scope;
   * all unresolvable identifiers wind up here, because they are considered
   * external to the program.
   */
  public JsRootScope getRootScope() {
    return rootScope;
  }

  /**
   * Gets the top level scope. This is the scope of all the statements in the
   * main program.
   */
  public JsScope getScope() {
    return topScope;
  }

  /**
   * Creates or retrieves a JsStringLiteral from an interned object pool.
   */
  public JsStringLiteral getStringLiteral(String value) {
    JsStringLiteral lit = stringLiteralMap.get(value);
    if (lit == null) {
      lit = new JsStringLiteral(value);
      stringLiteralMap.put(value, lit);
    }
    return lit;
  }

  public JsBooleanLiteral getTrueLiteral() {
    return trueLiteral;
  }

  public JsNameRef getUndefinedLiteral() {
    return new JsNameRef("$Dart$Null");
  }

  public void setFragmentCount(int fragments) {
    this.fragments = new JsProgramFragment[fragments];
    for (int i = 0; i < fragments; i++) {
      this.fragments[i] = new JsProgramFragment();
    }
  }

  public void setIndexedFunctions(Map<String, JsFunction> indexedFunctions) {
    this.indexedFunctions.clear();
    this.indexedFunctions.putAll(indexedFunctions);
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      for (JsProgramFragment fragment : fragments) {
        v.accept(fragment);
      }
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PROGRAM;
  }
}
