// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A special scope used only for catch blocks. It only holds a single symbol:
 * the catch argument's name.
 */
public class JsCatchScope extends JsScope {

  private final JsName name;

  public JsCatchScope(JsScope parent, String ident) {
    super(parent, "Catch scope");
    this.name = new JsName(this, ident, ident, ident);
  }

  @Override
  public JsName declareName(String ident) {
    // Declare into parent scope!
    return getParent().declareName(ident);
  }

  @Override
  public JsName declareName(String ident, String shortIdent) {
    // Declare into parent scope!
    return getParent().declareName(ident, shortIdent);
  }

  @Override
  public Iterator<JsName> getAllNames() {
    return new Iterator<JsName>() {
      private boolean didIterate = false;

      @Override
      public boolean hasNext() {
        return !didIterate;
      }

      @Override
      public JsName next() {
        if (didIterate) {
          throw new NoSuchElementException();
        }
        didIterate = true;
        return name;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  @Override
  protected JsName doCreateName(String ident, String shortIdent, String originalName) {
    throw new UnsupportedOperationException("Cannot create a name in a catch scope");
  }

  @Override
  protected JsName findExistingNameNoRecurse(String ident) {
    if (name.getIdent().equals(ident)) {
      return name;
    } else {
      return null;
    }
  }
}
