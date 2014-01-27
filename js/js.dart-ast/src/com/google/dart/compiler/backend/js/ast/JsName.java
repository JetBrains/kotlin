// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;

/**
 * An abstract base class for named JavaScript objects.
 */
public class JsName implements Symbol {
  private final JsScope enclosing;
  private final String ident;

  /**
   * @param ident the unmangled ident to use for this name
   */
  JsName(JsScope enclosing, String ident) {
    this.enclosing = enclosing;
    this.ident = ident;
  }

  public String getIdent() {
    return ident;
  }

  public JsNameRef makeRef() {
    return new JsNameRef(this);
  }

  @Override
  public String toString() {
    return ident;
  }

  @Override
  public int hashCode() {
    return ident.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JsName)) {
      return false;
    }
    JsName other = (JsName) obj;
    return ident.equals(other.ident) && enclosing == other.enclosing;
  }
}
