// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.JsReservedIdentifiers;

/**
 * The root scope is the parent of every scope. All identifiers in this scope
 * are not obfuscatable. This scope is prefilled with reserved global
 * JavaScript symbols.
 */
public final class JsRootScope extends JsScope {

  private final JsProgram program;

  public JsRootScope(JsProgram program) {
    super("Root");
    this.program = program;
  }

  @Override
  public JsProgram getProgram() {
    return program;
  }

  @Override
  protected JsName doCreateName(String ident, String shortIdent, String originalName) {
    JsName name = super.doCreateName(ident, shortIdent, originalName);
    name.setObfuscatable(false);
    return name;
  }

  @Override
  protected JsName findExistingNameNoRecurse(String ident) {
    JsName name = super.findExistingNameNoRecurse(ident);
    if (name == null) {
      if (JsReservedIdentifiers.getReservedGlobalSymbols().contains(ident)) {
        /*
         * Lazily add JsNames for reserved identifiers.  Since a JsName for a reserved global symbol
         * must report a legitimate enclosing scope, we can't simply have a shared set of symbol
         * names.
         */
        name = doCreateName(ident, ident, ident);
      }
    }
    return name;
  }
}
