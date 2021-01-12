// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class ProblemsTreeModel extends BaseTreeModel<Node> implements InvokerSupplier {
  private final Invoker invoker = Invoker.forBackgroundThreadWithReadAction(this);
  private final AtomicReference<Root> root = new AtomicReference<>();
  private final AtomicReference<Comparator<Node>> comparator = new AtomicReference<>();

  public ProblemsTreeModel(@NotNull Disposable parent) {
    Disposer.register(parent, this);
  }

  @Override
  public void dispose() {
    super.dispose();
    setRoot(null);
  }

  @Override
  public @NotNull Invoker getInvoker() {
    return invoker;
  }

  @Override
  public Root getRoot() {
    Root root = this.root.get();
    if (root != null && invoker.isValidThread()) root.update();
    return root;
  }

  @Override
  public @NotNull List<? extends Node> getChildren(Object object) {
    assert invoker.isValidThread() : "unexpected thread";
    Node node = object instanceof Node ? (Node)object : null;
    Collection<? extends Node> children = node == null ? null : node.getChildren();
    if (children == null || children.isEmpty()) return emptyList();
    assert null != comparator.get() : "set comparator before";
    node.update();
    children.forEach(Node::update); // update presentation of child nodes before processing
    return children.stream().sorted(comparator.get()).collect(toList());
  }

  void setComparator(@NotNull Comparator<Node> comparator) {
    if (!comparator.equals(this.comparator.getAndSet(comparator))) structureChanged();
  }

  boolean isRoot(@NotNull Root root) {
    return root == this.root.get();
  }

  void setRoot(@Nullable Root root) {
    Root old = this.root.getAndSet(root);
    if (old != root && old != null) Disposer.dispose(old);
    structureChanged();
  }

  void structureChanged() {
    treeStructureChanged(null, null, null);
  }

  void structureChanged(@NotNull Node node) {
    treeStructureChanged(node.getPath(), null, null);
  }

  void nodeChanged(@NotNull Node node) {
    treeNodesChanged(node.getPath(), null, null);
  }
}
