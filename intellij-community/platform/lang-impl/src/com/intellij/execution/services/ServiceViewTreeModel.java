// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.containers.TreeTraversal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ServiceViewTreeModel extends BaseTreeModel<Object> implements InvokerSupplier {
  private final ServiceViewModel myModel;
  private final Object myRoot = ObjectUtils.sentinel("services root");

  ServiceViewTreeModel(ServiceViewModel model) {
    myModel = model;
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return myModel.getInvoker();
  }

  @Override
  public void dispose() {
  }

  @Override
  public boolean isLeaf(Object object) {
    return object != myRoot && myModel.getChildren(((ServiceViewItem)object)).isEmpty();
  }

  @Override
  public List<?> getChildren(Object parent) {
    if (parent == myRoot) {
      return myModel.getRoots();
    }
    return myModel.getChildren(((ServiceViewItem)parent));
  }

  @Override
  public Object getRoot() {
    return myRoot;
  }

  void rootsChanged() {
    treeStructureChanged(new TreePath(myRoot), null, null);
  }

  Promise<TreePath> findPath(@NotNull Object service, @NotNull Class<?> contributorClass) {
    AsyncPromise<TreePath> result = new AsyncPromise<>();
    getInvoker().runOrInvokeLater(() -> {
      List<? extends ServiceViewItem> roots = myModel.getRoots();
      ServiceViewItem serviceNode = JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>)node ->
        contributorClass.isInstance(node.getRootContributor()) ? new ArrayList<>(myModel.getChildren(node)) : null)
        .withRoots(roots)
        .traverse(TreeTraversal.PLAIN_BFS)
        .filter(node -> node.getValue().equals(service))
        .first();
      if (serviceNode != null) {
        List<Object> path = new ArrayList<>();
        do {
          path.add(serviceNode);
          serviceNode = roots.contains(serviceNode) ? null : serviceNode.getParent();
        }
        while (serviceNode != null);
        path.add(myRoot);
        Collections.reverse(path);
        result.setResult(new TreePath(ArrayUtil.toObjectArray(path)));
        return;
      }

      result.setError("Service not found");
    });
    return result;
  }
}
