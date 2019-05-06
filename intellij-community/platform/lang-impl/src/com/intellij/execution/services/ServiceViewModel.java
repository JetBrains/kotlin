// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

interface ServiceViewModel extends Disposable, InvokerSupplier {
  List<? extends ServiceTreeNode> getRoots();

  void refresh(@NotNull ServiceViewEventListener.ServiceEvent e);

  void addModelListener(ServiceViewModelListener listener);

  void removeModelListener(ServiceViewModelListener listener);

  interface ServiceViewModelListener {
    void rootsChanged();
  }

  static <T> List<ServiceTreeNode> getContributorChildren(Project project,
                                                          ServiceTreeNode parent,
                                                          ServiceViewContributor<T> contributor) {
    Set<ServiceTreeNode> children = new LinkedHashSet<>();
    Map<Object, ServiceGroupNode> groupNodes = new HashMap<>();
    for (T service : contributor.getServices(project)) {
      Object value = service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor)service).asService() : service;

      if (contributor instanceof ServiceViewGroupingContributor) {
        ServiceViewGroupingContributor<T, Object> groupingContributor = (ServiceViewGroupingContributor<T, Object>)contributor;
        Object group = groupingContributor.groupBy(service);
        if (group != null) {
          ServiceGroupNode groupNode = groupNodes.get(group);
          if (groupNode == null) {
            groupNode = new ServiceGroupNode(group, parent, contributor, groupingContributor.getGroupDescriptor(group));
            groupNodes.put(group, groupNode);
          }
          ServiceTreeNode serviceNode = new ServiceNode(value, groupNode, contributor, contributor.getServiceDescriptor(service), project,
                                                        service instanceof ServiceViewContributor ? (ServiceViewContributor)service : null);
          groupNode.getChildren().add(serviceNode);
          children.add(groupNode);
          continue;
        }
      }

      ServiceTreeNode serviceNode = new ServiceNode(value, parent, contributor, contributor.getServiceDescriptor(service), project,
                                                    service instanceof ServiceViewContributor ? (ServiceViewContributor)service : null);
      children.add(serviceNode);
    }
    return new ArrayList<>(children);
  }

  class AllServicesModel implements ServiceViewModel {
    private final Project myProject;
    private final Invoker myInvoker = new Invoker.BackgroundThread(this);
    private final List<ServiceViewModelListener> myListeners = ContainerUtil.newSmartList();
    private final List<ServiceTreeNode> myRoots = new ArrayList<>();
    private boolean myRootInitialized;

    public AllServicesModel(Project project) {
      myProject = project;
    }

    @Override
    public List<? extends ServiceTreeNode> getRoots() {
      if (!myRootInitialized) {
        myRootInitialized = true;
        myRoots.clear();
        myRoots.addAll(doGetRoots());
      }
      return myRoots;
    }

    private List<? extends ServiceTreeNode> doGetRoots() {
      List<ServiceTreeNode> result = new ArrayList<>();
      for (ServiceViewContributor<?> contributor : ServiceViewManagerImpl.EP_NAME.getExtensions()) {
        result.addAll(getContributorChildren(myProject, null, contributor));
      }
      return result;
    }

    @Override
    public void refresh(@NotNull ServiceViewEventListener.ServiceEvent e) {
      getInvoker().runOrInvokeLater(() -> reset(e.contributorClass));
    }

    private void reset(Class<?> contributorClass) {
      int startIndex = -1;

      if (myRoots.isEmpty()) {
        startIndex = 0;
      }
      else {
        Map<ServiceViewContributor, Integer> indexes = new HashMap<>();
        List<ServiceTreeNode> toRemove = new ArrayList<>();
        ServiceViewContributor previous = null;
        for (int i = 0; i < myRoots.size(); i++) {
          ServiceTreeNode child = myRoots.get(i);
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
            startIndex = myRoots.size() - toRemove.size();
          }
        }
        myRoots.removeAll(toRemove);
      }

      List<ServiceTreeNode> newChildren = null;
      for (ServiceViewContributor<?> contributor : ServiceViewManagerImpl.EP_NAME.getExtensions()) {
        if (contributorClass.isInstance(contributor)) {
          newChildren = getContributorChildren(myProject, null, contributor);
          break;
        }
      }
      if (newChildren != null) {
        myRoots.addAll(startIndex, newChildren);
      }

      for (ServiceViewModelListener listener : myListeners) {
        listener.rootsChanged();
      }
    }

    @Override
    public void addModelListener(ServiceViewModelListener listener) {
      myListeners.add(listener);
    }

    @Override
    public void removeModelListener(ServiceViewModelListener listener) {
      myListeners.remove(listener);
    }

    @Override
    public void dispose() {
    }

    @NotNull
    @Override
    public Invoker getInvoker() {
      return myInvoker;
    }
  }

  abstract class ServiceTreeNode implements ServiceViewItem {
    private final Object myValue;
    private final ServiceTreeNode myParent;
    private final ServiceViewContributor myContributor;
    private final ServiceViewDescriptor myViewDescriptor;
    private List<ServiceTreeNode> myChildren;
    private boolean myPresentationUpdated;

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
      if (!myPresentationUpdated) {
        myPresentationUpdated = true;
        if (myValue instanceof NodeDescriptor) {
          ((NodeDescriptor)myValue).update();
        }
      }
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

  class ServiceNode extends ServiceTreeNode {
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

  class ServiceGroupNode extends ServiceTreeNode {
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
