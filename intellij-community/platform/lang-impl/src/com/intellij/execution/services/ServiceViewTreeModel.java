// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceViewModel.ServiceTreeNode;
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
import java.util.*;

class ServiceViewTreeModel extends BaseTreeModel<Object> implements InvokerSupplier {
  private final ServiceViewModel myModel;
  private final ServiceViewModel.ServiceViewModelListener myListener;
  private final Object myRoot = ObjectUtils.sentinel("services root");

  ServiceViewTreeModel(ServiceViewModel model) {
    myModel = model;
    myListener = new ServiceViewModel.ServiceViewModelListener() {
      @Override
      public void rootsChanged() {
        treeStructureChanged(new TreePath(myRoot), null, null);
      }
    };
    myModel.addModelListener(myListener);
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return myModel.getInvoker();
  }

  @Override
  public void dispose() {
    myModel.removeModelListener(myListener);
  }

  @Override
  public boolean isLeaf(Object object) {
    return object != myRoot && ((ServiceTreeNode)object).getChildren().isEmpty();
  }

  @Override
  public List<?> getChildren(Object parent) {
    if (parent == myRoot) {
      return myModel.getRoots();
    }
    return ((ServiceTreeNode)parent).getChildren();
  }

  @Override
  public Object getRoot() {
    return myRoot;
  }

  Promise<TreePath> findPath(@NotNull Object service, @NotNull Class<?> contributorClass) {
    AsyncPromise<TreePath> result = new AsyncPromise<>();
    getInvoker().runOrInvokeLater(() -> {
      ServiceTreeNode serviceNode = JBTreeTraverser.from((Function<ServiceTreeNode, List<ServiceTreeNode>>)node ->
        contributorClass.isInstance(node.getContributor()) ? node.getChildren() : null)
        .withRoots(myModel.getRoots())
        .traverse(TreeTraversal.PLAIN_BFS)
        .filter(node -> node.getValue().equals(service))
        .first();
      if (serviceNode != null) {
        List<Object> path = new ArrayList<>();
        while (serviceNode != null) {
          path.add(serviceNode);
          serviceNode = serviceNode.getParent();
        }
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
