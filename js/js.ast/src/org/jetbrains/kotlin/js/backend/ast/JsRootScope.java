// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.backend.JsReservedIdentifiers;
import org.jetbrains.annotations.NotNull;

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
  protected JsName findOwnName(@NotNull String ident) {
    JsName name = super.findOwnName(ident);
    if (name == null) {
        if (JsReservedIdentifiers.reservedGlobalSymbols.contains(ident)) {
        /*
         * Lazily add JsNames for reserved identifiers.  Since a JsName for a reserved global symbol
         * must report a legitimate enclosing scope, we can't simply have a shared set of symbol
         * names.
         */
        name = doCreateName(ident);
      }
    }
    return name;
  }
}
