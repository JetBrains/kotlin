// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.common.HasSymbol;

/**
 * Implemented by JavaScript objects that have a name.
 */
public interface HasName extends HasSymbol {
  JsName getName();

  void setName(JsName name);
}
