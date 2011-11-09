// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

/**
 * An abstract base class for all JavaScript expressions.
 */
public abstract class JsExpression extends JsNode {

  protected JsExpression() {
  }

  /**
   * Determines whether the expression can cause side effects.
   */
  public abstract boolean hasSideEffects();

  /**
   * True if the target expression is definitely not null.
   */
  public abstract boolean isDefinitelyNotNull();

  /**
   * True if the target expression is definitely null.
   */
  public abstract boolean isDefinitelyNull();

  /**
   * Determines whether or not this expression is a leaf, such as a
   * {@link JsNameRef}, {@link JsBooleanLiteral}, and so on. Leaf expressions
   * never need to be parenthesized.
   */
  public boolean isLeaf() {
    // Conservatively say that it isn't a leaf.
    // Individual subclasses can speak for themselves if they are a leaf.
    return false;
  }

  public JsExprStmt makeStmt() {
    return new JsExprStmt(this);
  }

  @Override
  public JsExpression setSourceRef(SourceInfo info) {
    super.setSourceRef(info);
    return this;
  }
}
