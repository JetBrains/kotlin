// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.DataManager;
import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.navigationToolbar.NavBarModel;
import com.intellij.ide.navigationToolbar.NavBarPanel;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.tree.RestoreSelectionListener;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

class ServiceTreeView extends ServiceView {
  private static final String ADD_SERVICE_ACTION_ID = "ServiceView.AddService";

  private final ServiceViewTree myTree;
  private final ServiceViewTreeModel myTreeModel;
  private final ServiceViewModel.ServiceViewModelListener myListener;

  private final NavBarPanel myNavBarPanel;

  private volatile ServiceViewItem myLastSelection;
  private boolean mySelected;
  private volatile Promise<?> myUpdateSelectionPromise;

  ServiceTreeView(@NotNull Project project, @NotNull ServiceViewModel model, @NotNull ServiceViewUi ui, @NotNull ServiceViewState state) {
    super(new BorderLayout(), project, model, ui);

    myTreeModel = new ServiceViewTreeModel(model);
    myTree = new ServiceViewTree(myTreeModel, this);

    myListener = new MyViewModelListener();
    model.addModelListener(myListener);

    ServiceViewActionProvider actionProvider = ServiceViewActionProvider.getInstance();
    ui.setServiceToolbar(actionProvider);
    ui.setMasterComponent(myTree, actionProvider);

    myTree.setDragEnabled(true);
    DnDManager.getInstance().registerSource(ServiceViewDragHelper.createSource(this), myTree);
    DnDManager.getInstance().registerTarget(ServiceViewDragHelper.createTarget(myTree), myTree);

    add(myUi.getComponent(), BorderLayout.CENTER);

    myTree.addTreeSelectionListener(new RestoreSelectionListener());
    myTree.addTreeSelectionListener(e -> onSelectionChanged());
    model.addModelListener(this::rootsChanged);

    Consumer<ServiceViewItem> selector = item ->
      select(item.getValue(), item.getRootContributor().getClass())
        .onSuccess(result -> AppUIUtil.invokeOnEdt(() -> {
          JComponent component = getUi().getDetailsComponent();
          if (component != null) {
            IdeFocusManager.getInstance(getProject()).requestFocus(component, false);
          }
        }, getProject().getDisposed()));
    myNavBarPanel = new ServiceViewNavBarPanel(getProject(), true, getModel(), selector);
    myNavBarPanel.getModel().updateModel(null);
    myUi.setNavBar(myNavBarPanel);

    if (model instanceof ServiceViewModel.AllServicesModel) {
      setEmptyText(myTree, myTree.getEmptyText());
    }

    if (state.expandedPaths.isEmpty()) {
      state.treeState.applyTo(myTree, myTreeModel.getRoot());
    }
    else {
      Set<ServiceViewItem> roots = new THashSet<>(model.getVisibleRoots());
      List<TreePath> adjusted = adjustPaths(state.expandedPaths, roots, myTreeModel.getRoot());
      if (!adjusted.isEmpty()) {
        TreeUtil.promiseExpand(myTree, new PathExpandVisitor(adjusted));
      }
    }
  }

  @Override
  public void dispose() {
    getModel().removeModelListener(myListener);
    super.dispose();
  }

  @Override
  void saveState(@NotNull ServiceViewState state) {
    super.saveState(state);
    myUi.saveState(state);
    state.treeState = TreeState.createOn(myTree);
    state.expandedPaths = TreeUtil.collectExpandedPaths(myTree);
  }

  @NotNull
  @Override
  List<ServiceViewItem> getSelectedItems() {
    int[] rows = myTree.getSelectionRows();
    if (rows == null || rows.length == 0) return Collections.emptyList();

    List<Object> objects = TreeUtil.collectSelectedUserObjects(myTree);
    if (objects.size() != rows.length) {
      return ContainerUtil.mapNotNull(objects, o -> ObjectUtils.tryCast(o, ServiceViewItem.class));
    }

    List<Pair<Object, Integer>> objectRows = new ArrayList<>();
    for (int i = 0; i < rows.length; i++) {
      objectRows.add(Pair.create(objects.get(i), rows[i]));
    }
    Collections.sort(objectRows, Comparator.comparing(pair -> pair.second));
    return ContainerUtil.mapNotNull(objectRows, pair -> ObjectUtils.tryCast(pair.first, ServiceViewItem.class));
  }

  @Override
  Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass) {
    ServiceViewItem selectedItem = myLastSelection;
    if (selectedItem == null || !selectedItem.getValue().equals(service)) {
      AsyncPromise<Void> result = new AsyncPromise<>();
      myTreeModel.findPath(service, contributorClass)
        .onError(result::setError)
        .onSuccess(path -> {
          TreeUtil.promiseSelect(myTree, new PathSelectionVisitor(path))
            .onError(result::setError)
            .onSuccess(selectedPath -> {
              result.setResult(null);
              cancelSelectionUpdate();
            });
          cancelSelectionUpdate();
        });
      return result;
    }
    return Promises.resolvedPromise();
  }

  @Override
  void onViewSelected() {
    mySelected = true;
    if (myLastSelection != null) {
      ServiceViewDescriptor descriptor = myLastSelection.getViewDescriptor();
      onViewSelected(descriptor);
      myUi.setDetailsComponent(descriptor.getContentComponent());
    }
    else {
      myUi.setDetailsComponent(null);
    }
  }

  @Override
  void onViewUnselected() {
    mySelected = false;
    if (myLastSelection != null) {
      myLastSelection.getViewDescriptor().onNodeUnselected();
    }
  }

  @Override
  void jumpToServices() {
    if (myTree.isShowing()) {
      IdeFocusManager.getInstance(getProject()).requestFocus(myTree, false);
    }
    else {
      myNavBarPanel.rebuildAndSelectTail(true);
    }
  }

  private void onSelectionChanged() {
    List<ServiceViewItem> selected = getSelectedItems();
    ServiceViewItem newSelection = ContainerUtil.getOnlyItem(selected);
    if (Comparing.equal(newSelection, myLastSelection)) return;

    ServiceViewDescriptor oldDescriptor = myLastSelection == null ? null : myLastSelection.getViewDescriptor();
    if (oldDescriptor != null && mySelected) {
      oldDescriptor.onNodeUnselected();
    }

    myLastSelection = newSelection;
    myNavBarPanel.getModel().updateModel(newSelection);

    if (!mySelected) return;

    ServiceViewDescriptor newDescriptor = newSelection == null ? null : newSelection.getViewDescriptor();
    if (newDescriptor != null) {
      newDescriptor.onNodeSelected();
    }
    myUi.setDetailsComponent(newDescriptor == null ? null : newDescriptor.getContentComponent());
  }

  private void rootsChanged() {
    updateNavBar();
    ServiceViewItem lastSelection = myLastSelection;
    ServiceViewItem updatedItem = lastSelection == null ? null : getModel().findItem(lastSelection);
    AppUIUtil.invokeOnEdt(() -> {
      List<ServiceViewItem> selected = getSelectedItems();
      if (selected.isEmpty()) {
        ServiceViewItem item = ContainerUtil.getFirstItem(getModel().getRoots());
        if (item != null) {
          select(item.getValue(), item.getRootContributor().getClass());
          return;
        }
      }

      ServiceViewItem newSelection = ContainerUtil.getOnlyItem(selected);
      if (Comparing.equal(newSelection, updatedItem)) {
        newSelection = updatedItem;
      }
      if (Comparing.equal(newSelection, myLastSelection)) {
        myLastSelection = newSelection;
        if (mySelected) {
          ServiceViewDescriptor descriptor = newSelection == null ? null : newSelection.getViewDescriptor();
          myUi.setDetailsComponent(descriptor == null ? null : descriptor.getContentComponent());
        }
      }
    }, getProject().getDisposed());
  }

  private void updateNavBar() {
    AsyncPromise<ServiceViewItem> itemPromise = new AsyncPromise<>();
    itemPromise.onSuccess(item -> {
      if (item == null) return;

      getModel().getInvoker().invoke(() -> {
        ServiceViewItem updatedItem = getModel().findItem(item);
        if (updatedItem != null) {
          AppUIUtil.invokeOnEdt(() -> {
            ServiceViewItem navBarItem = getNavBarItem();
            if (updatedItem.equals(navBarItem)) {
              myNavBarPanel.getModel().updateModel(updatedItem);
            }
          }, getProject().getDisposed());
        }
      });
    });
    AppUIUtil.invokeOnEdt(() -> itemPromise.setResult(getNavBarItem()), getProject().getDisposed());
  }

  private ServiceViewItem getNavBarItem() {
    NavBarModel navBarModel = myNavBarPanel.getModel();
    if (navBarModel.isEmpty()) return null;

    return ObjectUtils.tryCast(navBarModel.getElement(navBarModel.size() - 1), ServiceViewItem.class);
  }

  @Override
  void setAutoScrollToSourceHandler(@NotNull AutoScrollToSourceHandler autoScrollToSourceHandler) {
    super.setAutoScrollToSourceHandler(autoScrollToSourceHandler);
    autoScrollToSourceHandler.install(myTree);
  }

  @Override
  List<Object> getChildrenSafe(@NotNull List<Object> valueSubPath) {
    Queue<Object> values = new LinkedList<>(valueSubPath);
    Object visibleRoot = values.poll();
    if (visibleRoot == null) return Collections.emptyList();

    int count = myTree.getRowCount();
    for (int i = 0; i < count; i++) {
      TreePath path = myTree.getPathForRow(i);
      Object node = path.getLastPathComponent();
      if (!(node instanceof ServiceViewItem)) continue;

      ServiceViewItem item = (ServiceViewItem)node;
      if (!visibleRoot.equals(item.getValue())) continue;

      while (!values.isEmpty()) {
        Object value = values.poll();
        item = ContainerUtil.find(getModel().getChildren(item), child -> value.equals(child.getValue()));
        if (item == null) return Collections.emptyList();
      }
      return ContainerUtil.map(getModel().getChildren(item), ServiceViewItem::getValue);
    }
    return Collections.emptyList();
  }

  @Override
  boolean hasItems() {
    return !myTree.isEmpty();
  }

  private void cancelSelectionUpdate() {
    Promise<?> selectPromise = myUpdateSelectionPromise;
    if (selectPromise instanceof AsyncPromise) {
      ((AsyncPromise<?>)selectPromise).cancel();
    }
  }

  private static void setEmptyText(JComponent component, StatusText emptyText) {
    emptyText.setText("No services configured.");
    emptyText.appendSecondaryText("Add Service",
                                  new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.linkColor()),
                                  new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                      ActionGroup addActionGroup = ObjectUtils.tryCast(
                                        ActionManager.getInstance().getAction(ADD_SERVICE_ACTION_ID), ActionGroup.class);
                                      if (addActionGroup == null) return;

                                      DataContext dataContext = DataManager.getInstance().getDataContext(component);
                                      JBPopupFactory.getInstance().createActionGroupPopup(
                                        addActionGroup.getTemplatePresentation().getText(), addActionGroup, dataContext,
                                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                        false, null, -1, null, ActionPlaces.getActionGroupPopupPlace(ADD_SERVICE_ACTION_ID))
                                        .show(new RelativePoint(component, component.getMousePosition()));
                                    }
                                  });
    AnAction addAction = ActionManager.getInstance().getAction(ADD_SERVICE_ACTION_ID);
    ShortcutSet shortcutSet = addAction == null ? null : addAction.getShortcutSet();
    Shortcut shortcut = shortcutSet == null ? null : ArrayUtil.getFirstElement(shortcutSet.getShortcuts());
    if (shortcut != null) {
      emptyText.appendSecondaryText(" (" + KeymapUtil.getShortcutText(shortcut) + ")", StatusText.DEFAULT_ATTRIBUTES, null);
    }
  }

  private static List<TreePath> adjustPaths(List<TreePath> paths, Collection<? extends ServiceViewItem> roots, Object treeRoot) {
    List<TreePath> result = new SmartList<>();
    for (TreePath path : paths) {
      Object[] items = path.getPath();
      for (int i = 1; i < items.length; i++) {
        //noinspection SuspiciousMethodCalls
        if (roots.contains(items[i])) {
          Object[] adjustedItems = new Object[items.length - i + 1];
          adjustedItems[0] = treeRoot;
          System.arraycopy(items, i, adjustedItems, 1, items.length - i);
          result.add(new TreePath(adjustedItems));
          break;
        }
      }
    }
    return result;
  }

  private class MyViewModelListener implements ServiceViewModel.ServiceViewModelListener {
    @Override
    public void rootsChanged() {
      AppUIUtil.invokeOnEdt(() -> {
        TreePath[] currentPaths = myTree.getSelectionPaths();
        List<TreePath> selectedPaths =
          currentPaths == null || currentPaths.length == 0 ? Collections.emptyList() : Arrays.asList(currentPaths);
        myTreeModel.rootsChanged();
        if (selectedPaths.isEmpty()) return;

        myTreeModel.getInvoker().invokeLater(() -> {
          List<Promise<TreePath>> pathPromises =
            ContainerUtil.mapNotNull(selectedPaths, path -> {
              ServiceViewItem item = ObjectUtils.tryCast(path.getLastPathComponent(), ServiceViewItem.class);
              return item == null ? null : myTreeModel.findPath(item.getValue(), item.getRootContributor().getClass());
            });
          Promises.collectResults(pathPromises, true).onProcessed(paths -> {
            if (paths != null && !paths.isEmpty() && !paths.equals(selectedPaths)) {
              Promise<?> newSelectPromise = TreeUtil.promiseSelect(myTree, paths.stream().map(PathSelectionVisitor::new));
              cancelSelectionUpdate();
              if (newSelectPromise instanceof AsyncPromise) {
                ((AsyncPromise<?>)newSelectPromise).onError(t -> {
                  if (t instanceof CancellationException) {
                    TreeUtil.promiseExpand(myTree, paths.stream().map(path -> new PathSelectionVisitor(path.getParentPath())));
                  }
                });
              }
              myUpdateSelectionPromise = newSelectPromise;
            }
          });
        });
      }, getProject().getDisposed());
    }
  }

  private static class PathSelectionVisitor implements TreeVisitor {
    private final Queue<Object> myPath;

    PathSelectionVisitor(TreePath path) {
      myPath = ContainerUtil.newLinkedList(path.getPath());
    }

    @NotNull
    @Override
    public Action visit(@NotNull TreePath path) {
      Object node = path.getLastPathComponent();
      if (node.equals(myPath.peek())) {
        myPath.poll();
        return myPath.isEmpty() ? Action.INTERRUPT : Action.CONTINUE;
      }
      return Action.SKIP_CHILDREN;
    }
  }

  private static class PathExpandVisitor implements TreeVisitor {
    private final List<TreePath> myPaths;

    PathExpandVisitor(List<TreePath> paths) {
      myPaths = paths;
    }

    @NotNull
    @Override
    public Action visit(@NotNull TreePath path) {
      if (path.getParentPath() == null) return Action.CONTINUE;

      for (TreePath treePath : myPaths) {
        if (treePath.equals(path)) {
          myPaths.remove(treePath);
          return myPaths.isEmpty() ? Action.INTERRUPT : Action.CONTINUE;
        }
      }
      return Action.SKIP_CHILDREN;
    }
  }
}
