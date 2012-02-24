// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Abstract base class for JavaScript statement objects.
 */
public abstract class JsStatement extends JsNode {

  protected JsStatement() {
  }

  /**
   * Returns true if this statement definitely causes an abrupt change in flow
   * control.
   */
  public boolean unconditionalControlBreak() {
    return false;
  }
}
