// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceEventListener.ServiceEvent;
import com.intellij.execution.services.ServiceModel.ServiceGroupNode;
import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceModelFilter.ServiceViewFilter;
import com.intellij.openapi.Disposable;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

abstract class ServiceViewModel implements Disposable, InvokerSupplier {
  protected final ServiceModel myModel;
  protected final ServiceModelFilter myModelFilter;
  private final ServiceViewFilter myFilter;
  private final List<ServiceViewModelListener> myListeners = ContainerUtil.newSmartList();

  protected ServiceViewModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter, ServiceViewFilter condition) {
    myModel = model;
    myModelFilter = modelFilter;
    myFilter = condition;
  }

  @NotNull
  List<? extends ServiceViewItem> getRoots() {
    return filterEmptyGroups(doGetRoots());
  }

  @NotNull
  protected abstract List<? extends ServiceViewItem> doGetRoots();

  abstract void eventProcessed(ServiceEvent e);

  void filtersChanged() {
    notifyListeners();
  }

  ServiceViewFilter getFilter() {
    return myFilter;
  }

  @NotNull
  List<? extends ServiceViewItem> getChildren(@NotNull ServiceViewItem parent) {
    return filterEmptyGroups(myModelFilter.filter(parent.getChildren(), myFilter));
  }

  void addModelListener(@NotNull ServiceViewModelListener listener) {
    myListeners.add(listener);
  }

  void removeModelListener(@NotNull ServiceViewModelListener listener) {
    myListeners.remove(listener);
  }

  protected void notifyListeners() {
    for (ServiceViewModelListener listener : myListeners) {
      listener.rootsChanged();
    }
  }

  @Override
  public void dispose() {
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return myModel.getInvoker();
  }

  @NotNull
  private List<? extends ServiceViewItem> filterEmptyGroups(@NotNull List<? extends ServiceViewItem> items) {
    return ContainerUtil.filter(items, item -> !(item instanceof ServiceGroupNode) || !getChildren(item).isEmpty());
  }

  @Nullable
  private static ServiceViewItem findItem(ServiceViewItem viewItem, List<? extends ServiceViewItem> modelItems) {
    return findItem(getPath(viewItem), modelItems);
  }

  @Nullable
  private static ServiceViewItem findItem(Deque<ServiceViewItem> path, List<? extends ServiceViewItem> modelItems) {
    ServiceViewItem node = path.removeFirst();
    for (ServiceViewItem root : modelItems) {
      if (root.equals(node)) {
        if (path.isEmpty()) {
          return root;
        }
        else {
          return findItem(path, root.getChildren());
        }
      }
    }
    return null;
  }

  private static Deque<ServiceViewItem> getPath(ServiceViewItem item) {
    Deque<ServiceViewItem> path = new LinkedList<>();
    do {
      path.addFirst(item);
      item = item.getParent();
    }
    while (item != null);
    return path;
  }

  interface ServiceViewModelListener {
    void rootsChanged();
  }

  static class AllServicesModel extends ServiceViewModel {
    AllServicesModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter) {
      super(model, modelFilter, null);
    }

    @Override
    @NotNull
    protected List<? extends ServiceViewItem> doGetRoots() {
      return myModelFilter.filter(myModel.getRoots(), null);
    }

    @Override
    void eventProcessed(ServiceEvent e) {
      notifyListeners();
    }
  }

  static class ContributorModel extends ServiceViewModel {
    private final ServiceViewContributor myContributor;

    ContributorModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter, @NotNull ServiceViewContributor contributor,
                     @Nullable ServiceViewFilter parentFilter) {
      super(model, modelFilter, new ServiceViewFilter(parentFilter) {
        @Override
        public boolean value(ServiceViewItem item) {
          return contributor.equals(item.getContributor());
        }
      });
      myContributor = contributor;
    }

    @NotNull
    @Override
    protected List<? extends ServiceViewItem> doGetRoots() {
      return myModelFilter.filter(ContainerUtil.filter(myModel.getRoots(), getFilter()), getFilter());
    }

    @Override
    void eventProcessed(ServiceEvent e) {
      if (e.contributorClass.isInstance(myContributor)) {
        notifyListeners();
      }
    }
  }

  static class GroupModel extends ServiceViewModel {
    private final AtomicReference<ServiceGroupNode> myGroupRef;

    GroupModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter,
               @NotNull AtomicReference<ServiceGroupNode> groupRef, @Nullable ServiceViewFilter parentFilter) {
      super(model, modelFilter, new ServiceViewFilter(parentFilter) {
        @Override
        public boolean value(ServiceViewItem item) {
          ServiceViewItem parent = item.getParent();
          return parent != null && parent.equals(groupRef.get());
        }
      });
      myGroupRef = groupRef;
    }

    @NotNull
    @Override
    protected List<? extends ServiceViewItem> doGetRoots() {
      ServiceGroupNode group = myGroupRef.get();
      return group == null ? Collections.emptyList() : getChildren(group);
    }

    @Override
    void eventProcessed(ServiceEvent e) {
      ServiceGroupNode group = myGroupRef.get();
      if (group == null || !e.contributorClass.isInstance(group.getRootContributor())) return;

      myGroupRef.set((ServiceGroupNode)findItem(group, myModel.getRoots()));
      notifyListeners();
    }
  }

  static class SingeServiceModel extends ServiceViewModel {
    private final AtomicReference<ServiceViewItem> myServiceRef;

    SingeServiceModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter,
                      @NotNull AtomicReference<ServiceViewItem> serviceRef, @Nullable ServiceViewFilter parentFilter) {
      super(model, modelFilter, new ServiceViewFilter(parentFilter) {
        @Override
        public boolean value(ServiceViewItem item) {
          return item.equals(serviceRef.get());
        }
      });
      myServiceRef = serviceRef;
    }

    @NotNull
    @Override
    protected List<? extends ServiceViewItem> doGetRoots() {
      ServiceViewItem service = myServiceRef.get();
      return service == null ? Collections.emptyList() : Collections.singletonList(service);
    }

    @Override
    void eventProcessed(ServiceEvent e) {
      ServiceViewItem service = myServiceRef.get();
      if (service == null || !e.contributorClass.isInstance(service.getRootContributor())) return;

      myServiceRef.set(findItem(service, myModel.getRoots()));
      notifyListeners();
    }
  }

  static class ServiceListModel extends ServiceViewModel {
    private final List<ServiceViewItem> myRoots;

    ServiceListModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter, @NotNull List<ServiceViewItem> roots,
                     @Nullable ServiceViewFilter parentFilter) {
      super(model, modelFilter, new ServiceViewFilter(parentFilter) {
        @Override
        public boolean value(ServiceViewItem item) {
          return roots.contains(item);
        }
      });
      myRoots = roots;
    }

    @NotNull
    @Override
    protected List<? extends ServiceViewItem> doGetRoots() {
      return myModelFilter.filter(myRoots, getFilter());
    }

    @Override
    void eventProcessed(ServiceEvent e) {
      boolean update = false;

      List<ServiceViewItem> toRemove = new ArrayList<>();
      for (int i = 0; i < myRoots.size(); i++) {
        ServiceViewItem node = myRoots.get(i);
        if (!e.contributorClass.isInstance(node.getRootContributor())) continue;

        ServiceViewItem updatedNode = findItem(node, myModel.getRoots());
        if (updatedNode != null) {
          //noinspection SuspiciousListRemoveInLoop
          myRoots.remove(i);
          myRoots.add(i, updatedNode);
        }
        else {
          toRemove.add(node);
        }
        update = true;
      }
      myRoots.removeAll(toRemove);

      if (update) {
        notifyListeners();
      }
    }
  }
}
