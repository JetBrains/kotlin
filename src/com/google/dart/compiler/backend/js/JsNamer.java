// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.JsProgram;

/**
 * A namer runs through a program and renames the short names of JsNames.
 * Namers must assign short names that don't clash and that are valid
 * JS-identifiers. Nested JsScopes must not shadow JsNames from outer
 * scopes.
 * If a JsName is marked as non-obfuscatable then it must retain its short
 * name.
 */
public interface JsNamer {
  /**
   * Names the shortNames of all JsNames of the program so that they are valid
   * JS-identifiers and that there are no clashes and no shadowing.
   */
  public void exec(JsProgram program);
}
