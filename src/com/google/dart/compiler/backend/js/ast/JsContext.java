// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * The context in which a JsNode visitation occurs. This represents the set of
 * possible operations a JsVisitor subclass can perform on the currently visited
 * node.
 */
public interface JsContext {

  boolean canInsert();

  boolean canRemove();

  void insertAfter(JsVisitable node);

  void insertBefore(JsVisitable node);

  boolean isLvalue();

  void removeMe();

  void replaceMe(JsVisitable node);
}
