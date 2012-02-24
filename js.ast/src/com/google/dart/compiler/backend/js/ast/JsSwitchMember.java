// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A member/case in a JavaScript switch object.
 */
public abstract class JsSwitchMember extends JsNode {

  protected final List<JsStatement> stmts = new ArrayList<JsStatement>();

  protected JsSwitchMember() {
    super();
  }

  public List<JsStatement> getStmts() {
    return stmts;
  }
}
