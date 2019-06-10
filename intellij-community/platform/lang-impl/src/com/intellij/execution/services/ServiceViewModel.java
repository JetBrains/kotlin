// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceEventListener.ServiceEvent;
import com.intellij.execution.services.ServiceModel.ContributorNode;
import com.intellij.execution.services.ServiceModel.ServiceGroupNode;
import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceModelFilter.ServiceViewFilter;
import com.intellij.execution.services.ServiceViewState.ServiceState;
import com.intellij.openapi.Disposable;
import com.intellij.util.Function;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.containers.TreeTraversal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class ServiceViewModel implements Disposable, InvokerSupplier {
  protected final ServiceModel myModel;
  protected final ServiceModelFilter myModelFilter;
  private final ServiceViewFilter myFilter;
  private final List<ServiceViewModelListener> myListeners = new CopyOnWriteArrayList<>();
  private volatile boolean myFlat;
  private volatile boolean myShowContributorRoots;

  protected ServiceViewModel(@NotNull ServiceModel model, @NotNull ServiceModelFilter modelFilter, ServiceViewFilter condition) {
    myModel = model;
    myModelFilter = modelFilter;
    myFilter = condition;
  }

  @NotNull
  List<? extends ServiceViewItem> getRoots() {
    List<? extends ServiceViewItem> roots = filterEmptyGroups(doGetRoots());
    if (roots.stream().anyMatch(ContributorNode.class::isInstance)) {
      if (myShowContributorRoots) {
        roots = ContainerUtil.filter(roots, item -> !(item instanceof ContributorNode) || !doGetChildren(item).isEmpty());
      }
      else {
        roots = roots.stream()
          .flatMap(item -> item instanceof ContributorNode ? doGetChildren(item).stream() : Stream.of(item))
          .collect(Collectors.toList());
      }
    }
    if (myFlat) {
      return JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>)node -> new ArrayList<>(doGetChildren(node)))
        .withRoots(roots)
        .traverse(TreeTraversal.LEAVES_DFS)
        .toList();
    }
    return roots;
  }

  @NotNull
  protected abstract List<? extends ServiceViewItem> doGetRoots();

  abstract void eventProcessed(ServiceEvent e);

  void saveState(ServiceViewState viewState) {
    viewState.flat = myFlat;
    viewState.groupByType = myShowContributorRoots;
  }

  void filtersChanged() {
    notifyListeners();
  }

  ServiceViewFilter getFilter() {
    return myFilter;
  }

  @NotNull
  List<? extends ServiceViewItem> getChildren(@NotNull ServiceViewItem parent) {
    if (myFlat) {
      return Collections.emptyList();
    }
    return doGetChildren(parent);
  }

  @NotNull
  protected List<? extends ServiceViewItem> doGetChildren(@NotNull ServiceViewItem parent) {
    return filterEmptyGroups(myModelFilter.filter(parent.getChildren(), myFilter));
  }

  @Nullable
  protected ServiceViewItem findItem(@NotNull ServiceViewItem item) {
    ServiceViewItem updatedItem = findItem(item, myModel.getRoots());
    if (updatedItem != null) {
      return updatedItem;
    }
    return myModel.findItem(item.getValue(), item.getRootContributor().getClass());
  }

  void addModelListener(@NotNull ServiceViewModelListener listener) {
    myListeners.add(listener);
  }

  void removeModelListener(@NotNull ServiceViewModelListener listener) {
    myListeners.remove(listener);
  }

  boolean isFlat() {
    return myFlat;
  }

  void setFlat(boolean flat) {
    if (myFlat != flat) {
      myFlat = flat;
      notifyListeners();
    }
  }

  boolean isGroupByType() {
    return myShowContributorRoots;
  }

  void setGroupByType(boolean value) {
    if (myShowContributorRoots != value) {
      myShowContributorRoots = value;
      notifyListeners();
    }
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
    return ContainerUtil.filter(items, item -> !(item instanceof ServiceGroupNode) || !doGetChildren(item).isEmpty());
  }

  static ServiceViewModel createModel(@NotNull List<ServiceViewItem> items,
                                      @Nullable ServiceViewContributor contributor,
                                      @NotNull ServiceModel model,
                                      @NotNull ServiceModelFilter modelFilter,
                                      @Nullable ServiceViewFilter parentFilter) {
    if (contributor != null) {
      ServiceViewItem contributorRoot = null;
      for (ServiceViewItem root : model.getRoots()) {
        if (contributor == root.getContributor()) {
          contributorRoot = root;
          break;
        }
      }
      if (contributorRoot != null && contributorRoot.getChildren().equals(items)) {
        return new ContributorModel(model, modelFilter, contributor, parentFilter);
      }
    }

    if (items.size() == 1) {
      ServiceViewItem item = items.get(0);
      if (item instanceof ContributorNode) {
        return new ContributorModel(model, modelFilter, item.getContributor(), parentFilter);
      }
      if (item instanceof ServiceGroupNode) {
        AtomicReference<ServiceGroupNode> ref = new AtomicReference<>((ServiceGroupNode)item);
        return new GroupModel(model, modelFilter, ref, parentFilter);
      }
      else {
        AtomicReference<ServiceViewItem> ref = new AtomicReference<>(item);
        return new SingeServiceModel(model, modelFilter, ref, parentFilter);
      }
    }
    return new ServiceListModel(model, modelFilter, items, parentFilter);
  }

  @Nullable
  static ServiceViewModel loadModel(@NotNull ServiceViewState viewState,
                                    @NotNull ServiceModel model,
                                    @NotNull ServiceModelFilter modelFilter,
                                    @Nullable ServiceViewFilter parentFilter,
                                    @NotNull Map<String, ServiceViewContributor> contributors) {
    if (viewState.viewType.equals(ContributorModel.TYPE)) {
      ServiceState serviceState = ContainerUtil.getOnlyItem(viewState.roots);
      ServiceViewContributor contributor = serviceState == null ? null : contributors.get(serviceState.contributor);
      return contributor == null ? null : new ContributorModel(model, modelFilter, contributor, parentFilter);
    }
    else if (viewState.viewType.equals(GroupModel.TYPE)) {
      ServiceState serviceState = ContainerUtil.getOnlyItem(viewState.roots);
      ServiceViewContributor contributor = serviceState == null ? null : contributors.get(serviceState.contributor);
      if (contributor == null) return null;

      ServiceViewItem groupItem = model.findItemById(serviceState.path, contributor);
      if (groupItem instanceof ServiceGroupNode) {
        AtomicReference<ServiceGroupNode> ref = new AtomicReference<>((ServiceGroupNode)groupItem);
        return new GroupModel(model, modelFilter, ref, parentFilter);
      }
    }
    else if (viewState.viewType.equals(SingeServiceModel.TYPE)) {
      ServiceState serviceState = ContainerUtil.getOnlyItem(viewState.roots);
      ServiceViewContributor contributor = serviceState == null ? null : contributors.get(serviceState.contributor);
      if (contributor == null) return null;

      ServiceViewItem serviceItem = model.findItemById(serviceState.path, contributor);
      if (serviceItem != null) {
        AtomicReference<ServiceViewItem> ref = new AtomicReference<>(serviceItem);
        return new SingeServiceModel(model, modelFilter, ref, parentFilter);
      }
    }
    else if (viewState.viewType.equals(ServiceListModel.TYPE)) {
      List<ServiceViewItem> items = new ArrayList<>();
      for (ServiceState serviceState : viewState.roots) {
        ServiceViewContributor contributor = contributors.get(serviceState.contributor);
        if (contributor != null) {
          ContainerUtil.addIfNotNull(items, model.findItemById(serviceState.path, contributor));
        }
      }
      if (!items.isEmpty()) {
        return new ServiceListModel(model, modelFilter, items, parentFilter);
      }
    }
    return null;
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

  @Nullable
  private static List<String> getIdPath(@Nullable ServiceViewItem item) {
    List<String> path = new ArrayList<>();
    while (item != null) {
      String id = item.getViewDescriptor().getId();
      if (id == null) {
        return null;
      }
      path.add(id);
      item = item.getParent();
    }
    Collections.reverse(path);
    return path;
  }

  @Nullable
  private static ServiceState getState(@Nullable ServiceViewItem item) {
    if (item == null) return null;

    List<String> path = getIdPath(item);
    if (path == null) return null;

    ServiceState serviceState = new ServiceState();
    serviceState.contributor = item.getRootContributor().getClass().getName();
    serviceState.path = path;
    return serviceState;
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
    private static final String TYPE = "contributor";

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

    @Override
    void saveState(ServiceViewState viewState) {
      super.saveState(viewState);
      viewState.viewType = TYPE;
      ServiceState serviceState = new ServiceState();
      serviceState.contributor = myContributor.getClass().getName();
      viewState.roots = ContainerUtil.newSmartList(serviceState);
    }

    ServiceViewContributor getContributor() {
      return myContributor;
    }
  }

  static class GroupModel extends ServiceViewModel {
    private static final String TYPE = "group";

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
      return group == null ? Collections.emptyList() : doGetChildren(group);
    }

    @Override
    void eventProcessed(ServiceEvent e) {
      ServiceGroupNode group = myGroupRef.get();
      if (group == null || !e.contributorClass.isInstance(group.getRootContributor())) return;

      myGroupRef.set((ServiceGroupNode)findItem(group));
      notifyListeners();
    }

    @Override
    void saveState(ServiceViewState viewState) {
      super.saveState(viewState);
      viewState.viewType = TYPE;
      ContainerUtil.addIfNotNull(viewState.roots, ServiceViewModel.getState(myGroupRef.get()));
    }

    ServiceGroupNode getGroup() {
      return myGroupRef.get();
    }
  }

  static class SingeServiceModel extends ServiceViewModel {
    private static final String TYPE = "service";

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

      myServiceRef.set(findItem(service));
      notifyListeners();
    }

    @Override
    void saveState(ServiceViewState viewState) {
      super.saveState(viewState);
      viewState.viewType = TYPE;
      ContainerUtil.addIfNotNull(viewState.roots, ServiceViewModel.getState(myServiceRef.get()));
    }

    ServiceViewItem getService() {
      return myServiceRef.get();
    }
  }

  static class ServiceListModel extends ServiceViewModel {
    private static final String TYPE = "services";

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

        ServiceViewItem updatedNode = findItem(node);
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

    @Override
    void saveState(ServiceViewState viewState) {
      super.saveState(viewState);
      viewState.viewType = TYPE;
      for (ServiceViewItem root : myRoots) {
        ContainerUtil.addIfNotNull(viewState.roots, ServiceViewModel.getState(root));
      }
    }

    List<ServiceViewItem> getItems() {
      return myRoots;
    }
  }
}
