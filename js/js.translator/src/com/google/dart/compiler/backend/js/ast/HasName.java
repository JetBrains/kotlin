// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.HasSymbol;

/**
 * Implemented by JavaScript objects that have a name.
 */
public interface HasName extends HasSymbol {
  JsName getName();
}
