// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represnts a JavaScript block in the global scope.
 */
public class JsGlobalBlock extends JsBlock {

  public JsGlobalBlock() {
  }

  @Override
  public boolean isGlobalBlock() {
    return true;
  }
}
