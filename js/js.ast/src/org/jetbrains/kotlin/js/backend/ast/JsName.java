// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadata;
import org.jetbrains.kotlin.js.common.Symbol;

/**
 * An abstract base class for named JavaScript objects.
 */
public class JsName extends HasMetadata implements Symbol {
  @NotNull
  private final String ident;

  private final boolean temporary;

  /**
   * @param ident the unmangled ident to use for this name
   */
  JsName(@NotNull String ident, boolean temporary) {
    this.ident = ident;
    this.temporary = temporary;
  }

  public boolean isTemporary() {
    return temporary;
  }

  @NotNull
  public String getIdent() {
    return ident;
  }

  @NotNull
  public JsNameRef makeRef() {
    return new JsNameRef(this);
  }

  @Override
  public String toString() {
    return ident;
  }
}
