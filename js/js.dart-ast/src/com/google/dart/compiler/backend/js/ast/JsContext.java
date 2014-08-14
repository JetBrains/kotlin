// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.Nullable;

/**
 * The context in which a JsNode visitation occurs. This represents the set of
 * possible operations a JsVisitor subclass can perform on the currently visited
 * node.
 */
public interface JsContext {
  boolean canInsert();

  boolean canRemove();

  void insertAfter(JsNode node);

  void insertBefore(JsNode node);

  boolean isLvalue();

  void removeMe();

  void replaceMe(JsNode node);

  @Nullable
  JsNode getCurrentNode();
}
