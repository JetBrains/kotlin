// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript regular expression.
 */
public final class JsRegExp extends JsValueLiteral {

  private String flags;
  private String pattern;

  public JsRegExp() {
  }

  public String getFlags() {
    return flags;
  }

  public String getPattern() {
    return pattern;
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

  public void setFlags(String suffix) {
    this.flags = suffix;
  }

  public void setPattern(String re) {
    this.pattern = re;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.REGEXP;
  }
}
