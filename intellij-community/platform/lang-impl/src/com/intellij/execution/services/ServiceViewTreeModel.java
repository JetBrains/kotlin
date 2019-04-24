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
  List<ServiceTreeNode> myRootChildren = null;

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
      if (myRootChildren == null) {
        myRootChildren = getRootChildren();
      }
      return myRootChildren;
    }
    return ((ServiceTreeNode)parent).getChildren();
  }

  @NotNull
  private List<ServiceTreeNode> getRootChildren() {
    List<ServiceTreeNode> result = new ArrayList<>();
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
    getInvoker().runOrInvokeLater(() -> reset(e.contributorClass));
  }

  private void reset(Class<?> contributorClass) {
    int startIndex = -1;

    if (myRootChildren == null) {
      myRootChildren = new ArrayList<>();
      startIndex = 0;
    }
    else {
      Map<ServiceViewContributor, Integer> indexes = new HashMap<>();
      List<ServiceTreeNode> toRemove = new ArrayList<>();
      ServiceViewContributor previous = null;
      for (int i = 0; i < myRootChildren.size(); i++) {
        ServiceTreeNode child = myRootChildren.get(i);
        if (contributorClass.isInstance(child.getContributor())) {
          toRemove.add(child);
          if (startIndex < 0) {
            startIndex = i;
          }
        }
        else if (previous != child.getContributor()) {
          previous = child.getContributor();
          indexes.put(previous, i);
        }
      }
      if (startIndex < 0) {
        ServiceViewContributor[] contributors = ServiceViewManagerImpl.EP_NAME.getExtensions();
        for (int i = contributors.length - 1; i >= 0; i--) {
          if (!contributorClass.isInstance(contributors[i])) {
            startIndex = indexes.getOrDefault(contributors[i], Integer.valueOf(-1));
            if (startIndex == 0) {
              break;
            }
          }
          else {
            break;
          }
        }
        if (startIndex < 0) {
          startIndex = myRootChildren.size() - toRemove.size();
        }
      }
      myRootChildren.removeAll(toRemove);
    }

    List<ServiceTreeNode> newChildren = null;
    for (ServiceViewContributor<?> contributor : ServiceViewManagerImpl.EP_NAME.getExtensions()) {
      if (contributorClass.isInstance(contributor)) {
        newChildren = getContributorChildren(myProject, null, contributor);
        break;
      }
    }
    if (newChildren != null) {
      myRootChildren.addAll(startIndex, newChildren);
    }

    treeStructureChanged(new TreePath(myRoot), null, null);
  }

  void refreshAll() {
    getInvoker().runOrInvokeLater(() -> myRootChildren = null);
    treeStructureChanged(null, null, null);
  }

  private static <T> List<ServiceTreeNode> getContributorChildren(Project project,
                                                                  ServiceTreeNode parent,
                                                                  ServiceViewContributor<T> contributor) {
    Set<ServiceTreeNode> children = new LinkedHashSet<>();
    Map<Object, ServiceGroupNode> groupNodes = new HashMap<>();
    for (T service : contributor.getServices(project)) {
      Object value = service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor)service).asService() : service;
      ServiceTreeNode serviceNode = new ServiceNode(value, parent, contributor, contributor.getServiceDescriptor(service), project,
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
            groupNode = new ServiceGroupNode(group, parent, contributor, groupingContributor.getGroupDescriptor(group));
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
    private final ServiceViewContributor myContributor;
    private final ServiceViewDescriptor myViewDescriptor;
    private List<ServiceTreeNode> myChildren;

    protected ServiceTreeNode(@NotNull Object value, @Nullable ServiceTreeNode parent, @NotNull ServiceViewContributor contributor,
                              @NotNull ServiceViewDescriptor viewDescriptor) {
      myValue = value;
      myParent = parent;
      myContributor = contributor;
      myViewDescriptor = viewDescriptor;
    }

    @NotNull
    @Override
    public Object getValue() {
      return myValue;
    }

    @NotNull
    ServiceViewContributor getContributor() {
      return myContributor;
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
    private final ServiceViewContributor<?> myProvidingContributor;

    ServiceNode(@NotNull Object service, @Nullable ServiceTreeNode parent, @NotNull ServiceViewContributor contributor,
                @NotNull ServiceViewDescriptor viewDescriptor,
                @NotNull Project project, @Nullable ServiceViewContributor providingContributor) {
      super(service, parent, contributor, viewDescriptor);
      myProject = project;
      myProvidingContributor = providingContributor;
    }

    @NotNull
    @Override
    List<ServiceTreeNode> doGetChildren() {
      return myProvidingContributor == null ? Collections.emptyList() : getContributorChildren(myProject, this, myProvidingContributor);
    }
  }

  private static class ServiceGroupNode extends ServiceTreeNode {
    ServiceGroupNode(@NotNull Object group, @Nullable ServiceTreeNode parent, @NotNull ServiceViewContributor contributor,
                     @NotNull ServiceViewDescriptor viewDescriptor) {
      super(group, parent, contributor, viewDescriptor);
    }

    @NotNull
    @Override
    List<ServiceTreeNode> doGetChildren() {
      return new ArrayList<>();
    }
  }
}
