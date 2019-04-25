// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.DataManager;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.Queue;

class ServiceView extends JPanel implements Disposable {
  private final Project myProject;
  private final ServiceViewState myState;
  private final ServiceViewTree myTree;
  private final ServiceViewTreeModel myTreeModel;
  private final ServiceViewUi myUi;

  private ServiceViewItem myLastSelection;

  ServiceView(@NotNull Project project, @NotNull ServiceViewUi ui, @NotNull ServiceViewState state) {
    super(new BorderLayout());
    myProject = project;
    myState = state;
    myUi = ui;

    myTreeModel = new ServiceViewTreeModel(project);
    myTree = new ServiceViewTree(myTreeModel, this);
    ui.setMasterPanel(myTree, ServiceViewActionProvider.getInstance());
    add(myUi.getComponent(), BorderLayout.CENTER);

    project.getMessageBus().connect(this).subscribe(ServiceViewEventListener.TOPIC, myTreeModel::refresh);
    myTree.addTreeSelectionListener(e -> onSelectionChanged());

    myTreeModel.refreshAll();
    state.treeState.applyTo(myTree, myTreeModel.getRoot());

    putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, (DataProvider)dataId -> {
      if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return ServiceViewManagerImpl.getToolWindowContextHelpId();
      }
      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return ContainerUtil.map2Array(getSelectedItems(), ServiceViewItem::getValue);
      }
      List<ServiceViewItem> selectedItems = getSelectedItems();
      ServiceViewItem selectedItem = ContainerUtil.getOnlyItem(selectedItems);
      ServiceViewDescriptor descriptor = selectedItem == null ? null : selectedItem.getViewDescriptor();
      DataProvider dataProvider = descriptor == null ? null : descriptor.getDataProvider();
      if (dataProvider != null) {
        return RecursionManager.doPreventingRecursion(this, false, () -> dataProvider.getData(dataId));
      }
      return null;
    });
  }

  List<ServiceViewItem> getSelectedItems() {
    return ContainerUtil.mapNotNull(TreeUtil.collectSelectedUserObjects(myTree), o -> ObjectUtils.tryCast(o, ServiceViewItem.class));
  }

  ServiceViewState getState() {
    myUi.saveState(myState);
    myState.treeState = TreeState.createOn(myTree);
    return myState;
  }

  @Override
  public void dispose() {
  }

  Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass) {
    if (myLastSelection == null || !myLastSelection.getValue().equals(service)) {
      AsyncPromise<Void> result = new AsyncPromise<>();
      myTreeModel.findPath(service, contributorClass)
        .onError(result::setError)
        .onSuccess(path -> TreeUtil.promiseSelect(myTree, new PathSelectionVisitor(path))
          .onError(result::setError)
          .onSuccess(selectedPath -> result.setResult(null)));
      return result;
    }
    return Promises.resolvedPromise();
  }

  private void onSelectionChanged() {
    List<ServiceViewItem> selected = getSelectedItems();
    ServiceViewItem newSelection = ContainerUtil.getOnlyItem(selected);
    if (Comparing.equal(newSelection, myLastSelection)) return;

    ServiceViewDescriptor oldDescriptor = myLastSelection == null ? null : myLastSelection.getViewDescriptor();
    if (oldDescriptor != null) {
      oldDescriptor.onNodeUnselected();
    }

    myLastSelection = newSelection;
    ServiceViewDescriptor newDescriptor = newSelection == null ? null : newSelection.getViewDescriptor();

    if (newDescriptor != null) {
      newDescriptor.onNodeSelected();
    }
    if (newDescriptor instanceof Navigatable) {
      Navigatable navigatable = (Navigatable)newDescriptor;
      if (ServiceViewManagerImpl.isAutoScrollToSourceEnabled(myProject) && navigatable.canNavigate()) navigatable.navigate(false);
    }

    myUi.setDetailsComponent(newDescriptor == null ? null : newDescriptor.getContentComponent());
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
}
