// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.util.Hack;

import java.util.List;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JsModVisitor extends JsVisitor {

  private class ListContext<T extends JsVisitable> implements JsContext {

    private List<T> collection;
    private int index;
    private boolean removed;
    private boolean replaced;

    @Override
    public boolean canInsert() {
      return true;
    }

    @Override
    public boolean canRemove() {
      return true;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      checkRemoved();
      collection.add(index + 1, Hack.<T>cast(node));
      didChange = true;
    }

    @Override
    public void insertBefore(JsVisitable node) {
      checkRemoved();
      collection.add(index++, Hack.<T>cast(node));
      didChange = true;
    }

    @Override
    public boolean isLvalue() {
      return false;
    }

    @Override
    public void removeMe() {
      checkState();
      collection.remove(index--);
      didChange = removed = true;
    }

    @Override
    public void replaceMe(JsVisitable node) {
      checkState();
      checkReplacement(collection.get(index), node);
      collection.set(index, Hack.<T>cast(node));
      didChange = replaced = true;
    }

    protected void traverse(List<T> collection) {
      this.collection = collection;
      for (index = 0; index < collection.size(); ++index) {
        removed = replaced = false;
        doTraverse(collection.get(index), this);
      }
    }

    private void checkRemoved() {
      if (removed) {
        throw new RuntimeException("Node was already removed");
      }
    }

    private void checkState() {
      checkRemoved();
      if (replaced) {
        throw new RuntimeException("Node was already replaced");
      }
    }
  }

  private class LvalueContext extends NodeContext<JsExpression> {
    @Override
    public boolean isLvalue() {
      return true;
    }
  }

  private class NodeContext<T extends JsVisitable> implements JsContext {
    private T node;
    private boolean replaced;

    @Override
    public boolean canInsert() {
      return false;
    }

    @Override
    public boolean canRemove() {
      return false;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLvalue() {
      return false;
    }

    @Override
    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceMe(JsVisitable node) {
      if (replaced) {
        throw new RuntimeException("Node was already replaced");
      }
      checkReplacement(this.node, node);
      this.node = Hack.<T>cast(node);
      didChange = replaced = true;
    }

    protected T traverse(T node) {
      this.node = node;
      replaced = false;
      doTraverse(node, this);
      return this.node;
    }
  }

  protected static <T extends JsVisitable> void checkReplacement(T origNode, T newNode) {
    if (newNode == null) {
      throw new RuntimeException("Cannot replace with null");
    }
    if (newNode == origNode) {
      throw new RuntimeException("The replacement is the same as the original");
    }
  }

  protected boolean didChange = false;

  @Override
  public boolean didChange() {
    return didChange;
  }

  @Override
  protected <T extends JsVisitable> T doAccept(T node) {
    return new NodeContext<T>().traverse(node);
  }

  @Override
  protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
    NodeContext<T> ctx = new NodeContext<T>();
    for (int i = 0, c = collection.size(); i < c; ++i) {
      ctx.traverse(collection.get(i));
      if (ctx.replaced) {
        collection.set(i, ctx.node);
      }
    }
  }

  @Override
  protected JsExpression doAcceptLvalue(JsExpression expr) {
    return new LvalueContext().traverse(expr);
  }

  @Override
  protected <T extends JsVisitable> void doAcceptWithInsertRemove(List<T> collection) {
    new ListContext<T>().traverse(collection);
  }
}
