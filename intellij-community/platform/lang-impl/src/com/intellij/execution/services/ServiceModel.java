// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.diagnostic.PluginException;
import com.intellij.execution.services.ServiceEventListener.ServiceEvent;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.WeighedItem;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ColoredItem;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.containers.TreeTraversal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class ServiceModel implements Disposable, InvokerSupplier {
  private static final ExtensionPointName<ServiceViewContributor<?>> EP_NAME =
    ExtensionPointName.create("com.intellij.serviceViewContributor");
  private static final Logger LOG = Logger.getInstance(ServiceModel.class);

  private final Project myProject;
  private final Invoker myInvoker = new Invoker.Background(this);
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
    return getInvoker().invoke(() -> {
      if (!myRootsInitialized) {
        myRoots.addAll(doGetRoots());
        myRootsInitialized = true;
      }
    });
  }

  private List<? extends ServiceViewItem> doGetRoots() {
    List<ServiceViewItem> result = new ArrayList<>();
    for (ServiceViewContributor<?> contributor : getContributors()) {
      try {
        ContributorNode root = new ContributorNode(myProject, contributor);
        root.loadChildren();
        if (!root.getChildren().isEmpty()) {
          result.add(root);
        }
      }
      catch (Exception e) {
        PluginException.logPluginError(LOG, "Failed to init service view contributor " + contributor.getClass(), e, contributor.getClass());
      }
    }
    return result;
  }

  private JBIterable<ServiceViewItem> findItems(Object service, Class<?> contributorClass) {
    Object value = service instanceof ServiceViewProvidingContributor ?
                   ((ServiceViewProvidingContributor<?, ?>)service).asService() : service;
    return JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>)node ->
      contributorClass.isInstance(node.getRootContributor()) ? new ArrayList<>(node.getChildren()) : null)
      .withRoots(myRoots)
      .traverse(TreeTraversal.PLAIN_BFS)
      .filter(node -> node.getValue().equals(value));
  }

  @Nullable
  ServiceViewItem findItem(Condition<? super ServiceViewItem> condition, Condition<? super ServiceViewItem> visitChildrenCondition) {
    return JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>)node ->
      visitChildrenCondition.value(node) ? new ArrayList<>(node.getChildren()) : null)
      .withRoots(myRoots)
      .traverse(TreeTraversal.PLAIN_BFS)
      .filter(condition)
      .first();
  }

  @Nullable
  ServiceViewItem findItem(Object service, Class<?> contributorClass) {
    return findItems(service, contributorClass).first();
  }

  @Nullable
  ServiceViewItem findItemById(List<String> ids, ServiceViewContributor<?> contributor) {
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
  CancellablePromise<?> handle(@NotNull ServiceEvent e) {
    return getInvoker().invoke(() -> {
      LOG.debug("Handle event: " + e);
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
        case SERVICE_GROUP_CHANGED:
          serviceGroupChanged(e);
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
    ServiceViewContributor<?>[] contributors = getContributors();
    List<ServiceViewContributor<?>> existingContributors = ContainerUtil.map(myRoots, ServiceViewItem::getContributor);
    for (int i = contributors.length - 1; i >= 0; i--) {
      ServiceViewContributor<?> contributor = contributors[i];
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
      ServiceViewContributor<?> parentContributor = parent instanceof ServiceNode ? ((ServiceNode)parent).getProvidingContributor() : null;
      if (parentContributor == null) return;

      addService(e.target, parent.getChildren(), myProject, parent, parentContributor);
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

    addService(e.target, contributorNode.getChildren(), myProject, contributorNode, contributorNode.getContributor());
  }

  private void removeService(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item == null) return;

    ServiceViewItem parent = item.getParent();
    while (parent instanceof ServiceGroupNode) {
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
    if (item instanceof ServiceNode) {
      updateServiceViewDescriptor((ServiceNode)item, e.target);
    }
  }

  private static void updateServiceViewDescriptor(ServiceNode node, Object target) {
    ServiceViewContributor<?> providingContributor = node.getProvidingContributor();
    if (providingContributor != null && !providingContributor.equals(target)) {
      ((ServiceViewItem)node).setViewDescriptor(providingContributor.getViewDescriptor(node.myProject));
      return;
    }

    //noinspection unchecked
    ServiceViewDescriptor viewDescriptor =
      ((ServiceViewContributor<Object>)node.getContributor()).getServiceDescriptor(node.myProject, target);
    ((ServiceViewItem)node).setViewDescriptor(viewDescriptor);
  }

  private void serviceStructureChanged(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (item instanceof ServiceNode) {
      ServiceNode node = (ServiceNode)item;
      updateServiceViewDescriptor(node, e.target);
      node.reloadChildren();
    }
  }

  private void serviceGroupChanged(ServiceEvent e) {
    ServiceViewItem item = findItem(e.target, e.contributorClass);
    if (!(item instanceof ServiceNode)) return;

    ServiceViewItem parent = item.getParent();
    if (parent == null) return;

    ServiceGroupNode group = null;
    if (parent instanceof ServiceGroupNode) {
      group = (ServiceGroupNode)parent;
      parent = group.getParent();
      while (parent instanceof ServiceGroupNode) {
        parent = parent.getParent();
      }
      if (parent == null) return;
    }

    if (group != null) {
      group.getChildren().remove(item);
    }
    else {
      parent.getChildren().remove(item);
    }

    Object value = e.target;
    ServiceViewContributor<?> providingContributor = ((ServiceNode)item).getProvidingContributor();
    if (providingContributor != null && !providingContributor.equals(e.target)) {
      value = providingContributor;
    }

    ServiceNode serviceNode = addService(value, parent.getChildren(), myProject, parent, item.getContributor());
    serviceNode.moveChildren((ServiceNode)item);
    while (group != null && group.getChildren().isEmpty()) {
      ServiceViewItem groupParent = group.getParent();
      if (groupParent == null) return;

      groupParent.getChildren().remove(group);
      group = groupParent instanceof ServiceGroupNode ? (ServiceGroupNode)groupParent : null;
    }
  }

  private void groupChanged(ServiceEvent e) {
    JBIterable<ServiceGroupNode> groups = findItems(e.target, e.contributorClass).filter(ServiceGroupNode.class);
    ServiceGroupNode first = groups.first();
    if (first == null) return;

    //noinspection unchecked
    ServiceViewDescriptor viewDescriptor = ((ServiceViewGroupingContributor)first.getContributor()).getGroupDescriptor(e.target);
    for (ServiceViewItem group : groups) {
      group.setViewDescriptor(viewDescriptor);
      ServiceViewItem parent = group.getParent();
      if (parent != null) {
        List<ServiceViewItem> children = parent.getChildren();
        children.remove(group);
        addGroupOrdered(children, (ServiceGroupNode)group);
      }
    }
  }

  @NotNull
  static ServiceViewContributor<?>[] getContributors() {
    return EP_NAME.getExtensions();
  }

  private static <T> List<ServiceViewItem> getContributorChildren(Project project,
                                                                  ServiceViewItem parent,
                                                                  ServiceViewContributor<T> contributor) {
    List<ServiceViewItem> children = new ArrayList<>();
    for (T service : contributor.getServices(project)) {
      addService(service, children, project, parent, contributor);
    }
    return children;
  }

  private static <T> ServiceNode addService(Object service,
                                            List<ServiceViewItem> children,
                                            Project project,
                                            ServiceViewItem parent,
                                            ServiceViewContributor<T> contributor) {
    //noinspection unchecked
    T typedService = (T)service;
    Object value =
      service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor<?, ?>)service).asService() : service;
    if (contributor instanceof ServiceViewGroupingContributor) {
      ServiceNode serviceNode =
        addGroupNode((ServiceViewGroupingContributor<T, ?>)contributor, typedService, value, parent, project, children);
      if (serviceNode != null) {
        return serviceNode;
      }
    }

    ServiceNode
      serviceNode = new ServiceNode(value, parent, contributor, contributor.getServiceDescriptor(project, typedService), project,
                                    service instanceof ServiceViewContributor ? (ServiceViewContributor<?>)service : null);
    addServiceOrdered(children, serviceNode, contributor);
    return serviceNode;
  }

  private static <T, G> ServiceNode addGroupNode(ServiceViewGroupingContributor<T, G> groupingContributor,
                                                 T service,
                                                 Object value,
                                                 ServiceViewItem parent,
                                                 Project project,
                                                 List<ServiceViewItem> children) {
    List<G> groups = groupingContributor.getGroups(service);
    if (groups.isEmpty()) return null;

    List<ServiceViewItem> currentChildren = children;
    ServiceViewItem groupParent = parent;
    for (G group : groups) {
      boolean found = false;
      for (ServiceViewItem child : currentChildren) {
        if (child.getValue().equals(group)) {
          groupParent = child;
          currentChildren = groupParent.getChildren();
          found = true;
          break;
        }
      }
      if (!found) {
        ServiceGroupNode groupNode =
          new ServiceGroupNode(group, groupParent, groupingContributor, groupingContributor.getGroupDescriptor(group));
        addGroupOrdered(currentChildren, groupNode);
        groupParent = groupNode;
        currentChildren = groupParent.getChildren();
      }
    }
    ServiceNode
      serviceNode = new ServiceNode(value, groupParent, groupingContributor, groupingContributor.getServiceDescriptor(project, service),
                                    project, service instanceof ServiceViewContributor ? (ServiceViewContributor<?>)service : null);
    addServiceOrdered(currentChildren, serviceNode, groupingContributor);
    return serviceNode;
  }

  private static void addServiceOrdered(List<ServiceViewItem> children, ServiceNode child, ServiceViewContributor<?> contributor) {
    if (!children.isEmpty() && contributor instanceof Comparator) {
      @SuppressWarnings("unchecked")
      Comparator<Object> comparator = (Comparator<Object>)contributor;
      for (int i = 0; i < children.size(); i++) {
        ServiceViewItem anchor = children.get(i);
        if (anchor instanceof ServiceNode) {
          if (comparator.compare(child.getService(), ((ServiceNode)anchor).getService()) < 0) {
            children.add(i, child);
            return;
          }
        }
      }
    }
    children.add(child);
  }

  private static void addGroupOrdered(List<ServiceViewItem> children, ServiceGroupNode child) {
    if (!children.isEmpty()) {
      for (int i = 0; i < children.size(); i++) {
        ServiceViewItem anchor = children.get(i);
        if (anchor instanceof ServiceNode) {
          children.add(i, child);
          return;
        }
        else if (anchor instanceof ServiceGroupNode) {
          if (compareGroups(child, (ServiceGroupNode)anchor) < 0) {
            children.add(i, child);
            return;
          }
        }
      }
    }
    children.add(child);
  }

  private static int compareGroups(ServiceGroupNode group1, ServiceGroupNode group2) {
    ServiceViewDescriptor groupDescriptor1 = group1.getViewDescriptor();
    WeighedItem weighedItem1 = ObjectUtils.tryCast(groupDescriptor1, WeighedItem.class);
    ServiceViewDescriptor groupDescriptor2 = group2.getViewDescriptor();
    WeighedItem weighedItem2 = ObjectUtils.tryCast(groupDescriptor2, WeighedItem.class);
    if (weighedItem1 != null) {
      if (weighedItem2 == null) return -1;

      int diff = weighedItem1.getWeight() - weighedItem2.getWeight();
      if (diff != 0) return diff;
    }
    else if (weighedItem2 != null) {
      return 1;
    }
    String name1 = ServiceViewDragHelper.getDisplayName(groupDescriptor1.getPresentation());
    String name2 = ServiceViewDragHelper.getDisplayName(groupDescriptor2.getPresentation());
    return StringUtil.naturalCompare(name1, name2);
  }

  abstract static class ServiceViewItem implements ColoredItem {
    private final Object myValue;
    private volatile ServiceViewItem myParent;
    private final ServiceViewContributor<?> myContributor;
    private ServiceViewDescriptor myViewDescriptor;
    private final List<ServiceViewItem> myChildren = new CopyOnWriteArrayList<>();
    private volatile boolean myPresentationUpdated;

    protected ServiceViewItem(@NotNull Object value, @Nullable ServiceViewItem parent, @NotNull ServiceViewContributor<?> contributor,
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
    ServiceViewContributor<?> getContributor() {
      return myContributor;
    }

    @NotNull
    ServiceViewContributor<?> getRootContributor() {
      return myParent == null ? myContributor : myParent.getRootContributor();
    }

    @NotNull
    ServiceViewDescriptor getViewDescriptor() {
      if (!myPresentationUpdated) {
        myPresentationUpdated = true;
        if (myValue instanceof NodeDescriptor) {
          ((NodeDescriptor<?>)myValue).update();
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

    private void setParent(@Nullable ServiceViewItem parent) {
      myParent = parent;
    }

    @NotNull
    List<ServiceViewItem> getChildren() {
      return myChildren;
    }

    @Nullable
    @Override
    public Color getColor() {
      ServiceViewDescriptor descriptor = getViewDescriptor();
      return descriptor instanceof ColoredItem ? ((ColoredItem)descriptor).getColor() : null;
    }

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

    @Override
    public String toString() {
      return myValue.toString();
    }
  }

  static class ContributorNode extends ServiceViewItem {
    private final Project myProject;

    ContributorNode(@NotNull Project project, @NotNull ServiceViewContributor<?> contributor) {
      super(contributor, null, contributor, contributor.getViewDescriptor(project));
      myProject = project;
    }

    private void loadChildren() {
      List<ServiceViewItem> children = getChildren();
      if (!children.isEmpty()) {
        children.clear();
      }
      children.addAll(getContributorChildren(myProject, this, getContributor()));
    }
  }

  static class ServiceNode extends ServiceViewItem {
    private final Project myProject;
    private final ServiceViewContributor<?> myProvidingContributor;
    private volatile boolean myChildrenInitialized;

    ServiceNode(@NotNull Object service, @Nullable ServiceViewItem parent, @NotNull ServiceViewContributor<?> contributor,
                @NotNull ServiceViewDescriptor viewDescriptor,
                @NotNull Project project, @Nullable ServiceViewContributor<?> providingContributor) {
      super(service, parent, contributor, viewDescriptor);
      myProject = project;
      myProvidingContributor = providingContributor;
    }

    @NotNull
    @Override
    List<ServiceViewItem> getChildren() {
      List<ServiceViewItem> children = super.getChildren();
      if (!myChildrenInitialized) {
        if (myProvidingContributor != null) {
          children.addAll(getContributorChildren(myProject, this, myProvidingContributor));
        }
        myChildrenInitialized = true;
      }
      return children;
    }

    boolean isChildrenInitialized() {
      return myChildrenInitialized;
    }

    private void reloadChildren() {
      super.getChildren().clear();
      myChildrenInitialized = false;
    }

    private void moveChildren(ServiceNode node) {
      List<ServiceViewItem> children = super.getChildren();
      children.clear();
      List<ServiceViewItem> nodeChildren = ((ServiceViewItem)node).myChildren;
      children.addAll(nodeChildren);
      nodeChildren.clear();
      for (ServiceViewItem child : children) {
        child.setParent(this);
      }
      myChildrenInitialized = node.myChildrenInitialized;
    }

    @Nullable
    ServiceViewContributor<?> getProvidingContributor() {
      return myProvidingContributor;
    }

    @NotNull
    private Object getService() {
      return myProvidingContributor != null ? myProvidingContributor : getValue();
    }
  }

  static class ServiceGroupNode extends ServiceViewItem {
    ServiceGroupNode(@NotNull Object group, @Nullable ServiceViewItem parent, @NotNull ServiceViewContributor<?> contributor,
                     @NotNull ServiceViewDescriptor viewDescriptor) {
      super(group, parent, contributor, viewDescriptor);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ServiceGroupNode node = (ServiceGroupNode)o;
      return getValue().equals(node.getValue()) && Comparing.equal(getParent(), node.getParent());
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      ServiceViewItem parent = getParent();
      result = 31 * result + (parent != null ? parent.hashCode() : 0);
      return result;
    }
  }
}
