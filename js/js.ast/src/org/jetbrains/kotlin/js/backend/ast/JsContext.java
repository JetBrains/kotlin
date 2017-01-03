// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The context in which a JsNode visitation occurs. This represents the set of
 * possible operations a JsVisitor subclass can perform on the currently visited
 * node.
 */
public abstract class JsContext<T extends JsNode> {

  public <R extends T> void addPrevious(R node) {
    throw new UnsupportedOperationException();
  }

  public <R extends T> void addPrevious(List<R> nodes) {
    for (R node : nodes) {
      addPrevious(node);
    }
  }

  public <R extends T> void addNext(R node) {
    throw new UnsupportedOperationException();
  }

  public abstract void removeMe();

  public abstract <R extends T> void replaceMe(R node);

  @Nullable
  public abstract T getCurrentNode();
}
