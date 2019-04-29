// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.ui.tree.Searchable;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.tree.TreePath;
import java.util.*;

class ServiceViewTreeModel extends BaseTreeModel<Object> implements InvokerSupplier, Searchable {
  final Project myProject;
  final Object myRoot = ObjectUtils.sentinel("services root");
  final Invoker myInvoker = new Invoker.BackgroundThread(this);

  ServiceViewTreeModel(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return myInvoker;
  }

  @NotNull
  @Override
  public Promise<TreePath> getTreePath(Object object) {
    return Promises.resolvedPromise(new TreePath(myRoot)); // TODO [konstantin.aleev]
  }

  @Override
  public boolean isLeaf(Object object) {
    return object != myRoot && ((ServiceTreeNode)object).getChildren().isEmpty();
  }

  @Override
  public List<?> getChildren(Object parent) {
    if (parent == myRoot) {
      return getRootChildren();
    }
    return ((ServiceTreeNode)parent).getChildren();
  }

  @NotNull
  private List<?> getRootChildren() {
    List<Object> result = new ArrayList<>();
    for (ServiceViewContributor<?> contributor : ServiceViewManagerImpl.EP_NAME.getExtensions()) {
      result.addAll(getContributorChildren(myProject, null, contributor));
    }
    return result;
  }

  @Override
  public Object getRoot() {
    return myRoot;
  }

  void refresh(ServiceViewEventListener.ServiceEvent e) {
    refreshAll();
  }

  void refreshAll() {
    treeStructureChanged(null, null, null);
  }

  private static <T> List<ServiceTreeNode> getContributorChildren(Project project,
                                                                  ServiceTreeNode parent,
                                                                  ServiceViewContributor<T> contributor) {
    Set<ServiceTreeNode> children = new LinkedHashSet<>();
    Map<Object, ServiceGroupNode> groupNodes = new HashMap<>();
    for (T service : contributor.getServices(project)) {
      Object value = service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor)service).asService() : service;
      ServiceTreeNode serviceNode = new ServiceNode(value, parent, contributor.getServiceDescriptor(service), project,
                                                    service instanceof ServiceViewContributor ? (ServiceViewContributor)service : null);
      if (value instanceof NodeDescriptor) {
        ((NodeDescriptor)value).update();
      }

      ServiceTreeNode child = serviceNode;
      if (contributor instanceof ServiceViewGroupingContributor) {
        ServiceViewGroupingContributor<T, Object> groupingContributor = (ServiceViewGroupingContributor<T, Object>)contributor;
        Object group = groupingContributor.groupBy(service);
        if (group != null) {
          ServiceGroupNode groupNode = groupNodes.get(group);
          if (groupNode == null) {
            groupNode = new ServiceGroupNode(group, parent, groupingContributor.getGroupDescriptor(group));
            groupNodes.put(group, groupNode);
          }
          groupNode.getChildren().add(serviceNode);
          child = groupNode;
        }
      }
      children.add(child);
    }
    return new ArrayList<>(children);
  }

  private static abstract class ServiceTreeNode implements ServiceViewItem {
    private final Object myValue;
    private final ServiceTreeNode myParent;
    private final ServiceViewDescriptor myViewDescriptor;
    private List<ServiceTreeNode> myChildren;

    protected ServiceTreeNode(@NotNull Object value, @Nullable ServiceTreeNode parent, @NotNull ServiceViewDescriptor viewDescriptor) {
      myValue = value;
      myParent = parent;
      myViewDescriptor = viewDescriptor;
    }

    @NotNull
    @Override
    public Object getValue() {
      return myValue;
    }

    @NotNull
    @Override
    public ServiceViewDescriptor getViewDescriptor() {
      return myViewDescriptor;
    }

    @Nullable
    ServiceTreeNode getParent() {
      return myParent;
    }

    @NotNull
    List<ServiceTreeNode> getChildren() {
      if (myChildren == null) {
        myChildren = doGetChildren();
      }
      return myChildren;
    }

    @NotNull
    abstract List<ServiceTreeNode> doGetChildren();

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ServiceTreeNode node = (ServiceTreeNode)o;
      return myValue.equals(node.myValue);
    }

    @Override
    public int hashCode() {
      return myValue.hashCode();
    }
  }

  private static class ServiceNode extends ServiceTreeNode {
    private final Project myProject;
    private final ServiceViewContributor<?> myContributor;

    ServiceNode(@NotNull Object service, @Nullable ServiceTreeNode parent, @NotNull ServiceViewDescriptor viewDescriptor,
                @NotNull Project project, @Nullable ServiceViewContributor contributor) {
      super(service, parent, viewDescriptor);
      myProject = project;
      myContributor = contributor;
    }

    @NotNull
    @Override
    List<ServiceTreeNode> doGetChildren() {
      return myContributor == null ? Collections.emptyList() : getContributorChildren(myProject, this, myContributor);
    }
  }

  private static class ServiceGroupNode extends ServiceTreeNode {
    ServiceGroupNode(@NotNull Object group, @Nullable ServiceTreeNode parent, @NotNull ServiceViewDescriptor viewDescriptor) {
      super(group, parent, viewDescriptor);
    }

    @NotNull
    @Override
    List<ServiceTreeNode> doGetChildren() {
      return new ArrayList<>();
    }
  }
}
