// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceEventListener.ServiceEvent;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.Function;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.containers.TreeTraversal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ServiceModel implements Disposable, InvokerSupplier {
  private static final ExtensionPointName<ServiceViewContributor> EP_NAME =
    ExtensionPointName.create("com.intellij.serviceViewContributor");

  private final Project myProject;
  private final Invoker myInvoker = new Invoker.BackgroundThread(this);
  private final List<ServiceViewItem> myRoots = new CopyOnWriteArrayList<>();
  private volatile boolean myRootsInitialized;

  ServiceModel(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
  }

  @NotNull
  @Override
  public Invoker getInvoker() {
    return myInvoker;
  }

  @NotNull
  List<? extends ServiceViewItem> getRoots() {
    return myRootsInitialized ? myRoots : Collections.emptyList();
  }

  CancellablePromise<?> initRoots() {
    return getInvoker().runOrInvokeLater(() -> {
      if (!myRootsInitialized) {
        myRoots.addAll(doGetRoots());
        myRootsInitialized = true;
      }
    });
  }

  private List<? extends ServiceViewItem> doGetRoots() {
    List<ServiceViewItem> result = new ArrayList<>();
    for (ServiceViewContributor<?> contributor : getContributors()) {
      ContributorNode root = new ContributorNode(myProject, contributor);
      root.loadChildren();
      if (!root.getChildren().isEmpty()) {
        result.add(root);
      }
    }
    return result;
  }

  @Nullable
  ServiceViewItem findItem(Object service, Class<?> contributorClass) {
    Object value = service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor)service).asService() : service;
    return JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>)node ->
      contributorClass.isInstance(node.getRootContributor()) ? new ArrayList<>(node.getChildren()) : null)
      .withRoots(myRoots)
      .traverse(TreeTraversal.PLAIN_BFS)
      .filter(node -> node.getValue().equals(value))
      .first();
  }

  @Nullable
  ServiceViewItem findItemById(List<String> ids, ServiceViewContributor contributor) {
    if (ids.isEmpty()) return null;

    List<? extends ServiceViewItem> roots = ContainerUtil.filter(getRoots(), item -> contributor.equals(item.getContributor()));
    if (roots.isEmpty()) return null;

    return findItemById(new LinkedList<>(ids), roots);
  }

  private static ServiceViewItem findItemById(Deque<String> path, List<? extends ServiceViewItem> roots) {
    String id = path.removeFirst();
    for (ServiceViewItem root : roots) {
      if (id.equals(root.getViewDescriptor().getId())) {
        return path.isEmpty() ? root : findItemById(path, root.getChildren());
      }
    }
    return null;
  }

  @NotNull
  CancellablePromise<?> refresh(@NotNull ServiceEvent e) {
    return getInvoker().runOrInvokeLater(() -> {
      switch (e.type) {
        case SERVICE_ADDED:
          addService(e);
          break;
        case SERVICE_REMOVED:
          removeService(e);
          break;
        case SERVICE_CHANGED:
          serviceChanged(e);
          break;
        case SERVICE_STRUCTURE_CHANGED:
          serviceStructureChanged(e);
          break;
        case GROUP_CHANGED:
          groupChanged(e);
          break;
        default:
          reset(e.contributorClass);
      }
    });
  }

  private void reset(Class<?> contributorClass) {
    int index = -1;

    if (myRoots.isEmpty()) {
      index = 0;
    }
    else {
      ServiceViewItem contributorNode = null;
      for (int i = 0; i < myRoots.size(); i++) {
        ServiceViewItem child = myRoots.get(i);
        if (contributorClass.isInstance(child.getContributor())) {
          contributorNode = child;
          index = i;
          break;
        }
      }
      if (contributorNode != null) {
        myRoots.remove(contributorNode);
      }
      else {
        index = getContributorNodeIndex(contributorClass);
      }
    }

    ContributorNode newRoot = null;
    for (ServiceViewContributor<?> contributor : getContributors()) {
      if (contributorClass.isInstance(contributor)) {
        newRoot = new ContributorNode(myProject, contributor);
        newRoot.loadChildren();
        if (newRoot.getChildren().isEmpty()) {
          newRoot = null;
        }
        break;
      }
    }
    if (newRoot != null) {
      myRoots.add(index, newRoot);
    }
  }

  private int getContributorNodeIndex(Class<?> contributorClass) {
    int index = -1;
    ServiceViewContributor[] contributors = getContributors();
    List<ServiceViewContributor> existingContributors = ContainerUtil.map(myRoots, ServiceViewItem::getContributor);
    for (int i = contributors.length - 1; i >= 0; i--) {
      ServiceViewContributor contributor = contributors[i];
      if (!contributorClass.isInstance(contributor)) {
        index = existingContributors.indexOf(contributor);
        if (index == 0) {
          break;
        }
      }
      else {
        break;
      }
    }
    if (index < 0) {
      index = myRoots.size();
    }
    return index;
  }

  private void addService(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item != null) return;

    if (e.parent != null) {
      ServiceViewItem parent = findItem(e.parent, e.contributorClass);
      if (parent == null) return;

      addService(e.target, parent.getChildren(), myProject, parent, (ServiceViewContributor<?>)e.parent, e.index);
      return;
    }

    ServiceViewItem contributorNode = null;
    for (ServiceViewItem child : myRoots) {
      if (e.contributorClass.isInstance(child.getContributor())) {
        contributorNode = child;
        break;
      }
    }
    if (contributorNode == null) {
      int index = getContributorNodeIndex(e.contributorClass);
      for (ServiceViewContributor<?> contributor : getContributors()) {
        if (e.contributorClass.isInstance(contributor)) {
          contributorNode = new ContributorNode(myProject, contributor);
          myRoots.add(index, contributorNode);
          break;
        }
      }
      if (contributorNode == null) {
        return;
      }
    }

    addService(e.target, contributorNode.getChildren(), myProject, contributorNode,
               (ServiceViewContributor<?>)contributorNode.getContributor(), e.index);
  }

  private void removeService(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item == null) return;

    ServiceViewItem parent = item.getParent();
    if (parent instanceof ServiceGroupNode) {
      parent.getChildren().remove(item);
      if (!parent.getChildren().isEmpty()) return;

      item = parent;
      parent = parent.getParent();
    }
    if (parent instanceof ContributorNode) {
      parent.getChildren().remove(item);
      if (!parent.getChildren().isEmpty()) return;

      item = parent;
      parent = parent.getParent();
    }
    if (parent == null) {
      myRoots.remove(item);
    }
    else {
      parent.getChildren().remove(item);
    }
  }

  private void serviceChanged(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item == null) return;

    //noinspection unchecked
    ServiceViewDescriptor viewDescriptor = item.getContributor().getServiceDescriptor(e.target);
    item.setViewDescriptor(viewDescriptor);
  }

  private void groupChanged(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item == null) return;

    //noinspection unchecked
    ServiceViewDescriptor viewDescriptor = ((ServiceViewGroupingContributor)item.getContributor()).getGroupDescriptor(e.target);
    item.setViewDescriptor(viewDescriptor);
  }

  private void serviceStructureChanged(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item == null) return;

    ServiceViewItem parent = item.getParent();
    if (parent == null) return;

    ServiceGroupNode group = null;
    if (parent instanceof ServiceGroupNode) {
      group = (ServiceGroupNode)parent;
      parent = group.getParent();
      if (parent == null) return;
    }

    List<ServiceViewItem> services = flatServices(parent.getChildren());
    int index = services.indexOf(item);

    if (group != null) {
      group.getChildren().remove(item);
    }
    else {
      parent.getChildren().remove(item);
    }

    addService(e.target, parent.getChildren(), myProject, parent, (ServiceViewContributor<?>)parent.getContributor(), index);
    if (group != null && group.getChildren().isEmpty()) {
      parent.getChildren().remove(group);
    }
  }

  @NotNull
  public static ServiceViewContributor[] getContributors() {
    ServiceViewContributor[] result = EP_NAME.getExtensions();
    return Registry.is("ide.service.view") ?
           result :
           Arrays.stream(result).filter(c -> c instanceof ServiceViewAlwaysEnabledContributor).toArray(ServiceViewContributor[]::new);
  }

  private static <T> List<ServiceViewItem> getContributorChildren(Project project,
                                                                  ServiceViewItem parent,
                                                                  ServiceViewContributor<T> contributor) {
    List<ServiceViewItem> children = new ArrayList<>();
    for (T service : contributor.getServices(project)) {
      addService(service, children, project, parent, contributor, -1);
    }
    return children;
  }

  private static List<ServiceViewItem> flatServices(Collection<ServiceViewItem> items) {
    return items.stream()
      .flatMap(child -> child instanceof ServiceGroupNode ? child.getChildren().stream() : Stream.of(child))
      .collect(Collectors.toList());
  }

  private static <T> void addService(Object service,
                                     List<ServiceViewItem> children,
                                     Project project,
                                     ServiceViewItem parent,
                                     ServiceViewContributor<T> contributor,
                                     int index) {
    //noinspection unchecked
    T typedService = (T)service;
    Object value = service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor)service).asService() : service;
    if (!(contributor instanceof ServiceViewGroupingContributor) ||
        !addGroupNode((ServiceViewGroupingContributor<T, ?>)contributor,
                      typedService, value, parent, project, children, index)) {
      ServiceViewItem
        serviceNode = new ServiceNode(value, parent, contributor, contributor.getServiceDescriptor(typedService), project,
                                      service instanceof ServiceViewContributor ? (ServiceViewContributor)service : null);
      addChild(children, serviceNode, index);
    }
  }

  private static <T, G> boolean addGroupNode(ServiceViewGroupingContributor<T, G> groupingContributor,
                                             T service,
                                             Object value,
                                             ServiceViewItem parent,
                                             Project project,
                                             List<ServiceViewItem> children,
                                             int index) {
    G group = groupingContributor.groupBy(service);
    if (group == null) return false;

    ServiceGroupNode groupNode = null;
    for (ServiceViewItem child : children) {
      if (child instanceof ServiceGroupNode && child.getValue().equals(group)) {
        groupNode = (ServiceGroupNode)child;
        break;
      }
    }
    if (groupNode == null) {
      groupNode = new ServiceGroupNode(group, parent, groupingContributor, groupingContributor.getGroupDescriptor(group));
      ServiceViewItem
        serviceNode = new ServiceNode(value, groupNode, groupingContributor, groupingContributor.getServiceDescriptor(service), project,
                                      service instanceof ServiceViewContributor ? (ServiceViewContributor)service : null);
      groupNode.getChildren().add(serviceNode);
      addChild(children, groupNode, index);
      return true;
    }

    ServiceViewItem
      serviceNode = new ServiceNode(value, groupNode, groupingContributor, groupingContributor.getServiceDescriptor(service), project,
                                    service instanceof ServiceViewContributor ? (ServiceViewContributor)service : null);
    addGroupChild(children, groupNode, serviceNode, index);
    return true;
  }

  @SuppressWarnings("DuplicatedCode")
  private static void addChild(List<ServiceViewItem> children, ServiceViewItem child, int index) {
    if (index < 0) {
      children.add(child);
      return;
    }
    List<ServiceViewItem> services = flatServices(children);
    if (services.size() <= index) {
      children.add(child);
      return;
    }
    ServiceViewItem anchor = services.get(index);
    ServiceViewItem anchorParent = anchor.getParent();
    int serviceIndex = children.indexOf(anchorParent instanceof ServiceGroupNode ? anchorParent : anchor);
    if (serviceIndex < 0) {
      children.add(child);
    }
    else {
      children.add(serviceIndex, child);
    }
  }

  @SuppressWarnings("DuplicatedCode")
  private static void addGroupChild(List<ServiceViewItem> children, ServiceGroupNode groupNode, ServiceViewItem child, int index) {
    List<ServiceViewItem> groupChildren = groupNode.getChildren();
    if (index < 0) {
      groupChildren.add(child);
      return;
    }
    List<ServiceViewItem> services = flatServices(children);
    if (services.size() <= index) {
      groupChildren.add(child);
      return;
    }

    for (ServiceViewItem groupChild : groupChildren) {
      int childIndex = services.indexOf(groupChild);
      if (childIndex >= index) {
        groupChildren.add(groupChildren.indexOf(groupChild), child);
        return;
      }
    }
    groupChildren.add(child);
  }

  abstract static class ServiceViewItem {
    private final Object myValue;
    private final ServiceViewItem myParent;
    private final ServiceViewContributor myContributor;
    private ServiceViewDescriptor myViewDescriptor;
    private List<ServiceViewItem> myChildren;
    private boolean myPresentationUpdated;

    protected ServiceViewItem(@NotNull Object value, @Nullable ServiceViewItem parent, @NotNull ServiceViewContributor contributor,
                              @NotNull ServiceViewDescriptor viewDescriptor) {
      myValue = value;
      myParent = parent;
      myContributor = contributor;
      myViewDescriptor = viewDescriptor;
    }

    @NotNull
    Object getValue() {
      return myValue;
    }

    @NotNull
    ServiceViewContributor getContributor() {
      return myContributor;
    }

    @NotNull
    ServiceViewContributor getRootContributor() {
      return myParent == null ? myContributor : myParent.getRootContributor();
    }

    @NotNull
    ServiceViewDescriptor getViewDescriptor() {
      if (!myPresentationUpdated) {
        myPresentationUpdated = true;
        if (myValue instanceof NodeDescriptor) {
          ((NodeDescriptor)myValue).update();
        }
      }
      return myViewDescriptor;
    }

    private void setViewDescriptor(@NotNull ServiceViewDescriptor viewDescriptor) {
      AppUIUtil.invokeOnEdt(() -> {
        myViewDescriptor = viewDescriptor;
        myPresentationUpdated = false;
      });
    }

    @Nullable
    ServiceViewItem getParent() {
      return myParent;
    }

    @NotNull
    List<ServiceViewItem> getChildren() {
      if (myChildren == null) {
        myChildren = doGetChildren();
      }
      return myChildren;
    }

    @NotNull
    protected abstract List<ServiceViewItem> doGetChildren();

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ServiceViewItem node = (ServiceViewItem)o;
      return myValue.equals(node.myValue);
    }

    @Override
    public int hashCode() {
      return myValue.hashCode();
    }
  }

  static class ContributorNode extends ServiceViewItem {
    private final Project myProject;

    ContributorNode(@NotNull Project project, @NotNull ServiceViewContributor contributor) {
      super(contributor, null, contributor, contributor.getViewDescriptor());
      myProject = project;
    }

    private void loadChildren() {
      List<ServiceViewItem> children = getChildren();
      if (!children.isEmpty()) {
        children.clear();
      }
      children.addAll(getContributorChildren(myProject, this, (ServiceViewContributor<?>)getContributor()));
    }

    @NotNull
    @Override
    protected List<ServiceViewItem> doGetChildren() {
      return new ArrayList<>();
    }
  }

  static class ServiceNode extends ServiceViewItem {
    private final Project myProject;
    private final ServiceViewContributor<?> myProvidingContributor;

    ServiceNode(@NotNull Object service, @Nullable ServiceViewItem parent, @NotNull ServiceViewContributor contributor,
                @NotNull ServiceViewDescriptor viewDescriptor,
                @NotNull Project project, @Nullable ServiceViewContributor providingContributor) {
      super(service, parent, contributor, viewDescriptor);
      myProject = project;
      myProvidingContributor = providingContributor;
    }

    @NotNull
    @Override
    protected List<ServiceViewItem> doGetChildren() {
      return myProvidingContributor == null ? Collections.emptyList() : getContributorChildren(myProject, this, myProvidingContributor);
    }
  }

  static class ServiceGroupNode extends ServiceViewItem {
    ServiceGroupNode(@NotNull Object group, @Nullable ServiceViewItem parent, @NotNull ServiceViewContributor contributor,
                     @NotNull ServiceViewDescriptor viewDescriptor) {
      super(group, parent, contributor, viewDescriptor);
    }

    @NotNull
    @Override
    protected List<ServiceViewItem> doGetChildren() {
      return new ArrayList<>();
    }
  }
}
