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

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

class ServiceView extends JPanel implements Disposable {
  private final Project myProject;
  private final ServiceViewState myState;
  private final ServiceViewTree myTree;
  private final ServiceViewUi myUi;

  private ServiceViewItem myLastSelection;

  ServiceView(@NotNull Project project, @NotNull ServiceViewUi ui, @NotNull ServiceViewState state) {
    super(new BorderLayout());
    myProject = project;
    myState = state;
    myUi = ui;

    ServiceViewTreeModel treeModel = new ServiceViewTreeModel(project);
    myTree = new ServiceViewTree(treeModel, this);
    ui.setMasterPanel(myTree, ServiceViewActionProvider.getInstance());
    add(myUi.getComponent(), BorderLayout.CENTER);

    project.getMessageBus().connect(this).subscribe(ServiceViewEventListener.TOPIC, treeModel::refresh);
    myTree.addTreeSelectionListener(e -> onSelectionChanged());

    treeModel.refreshAll();
    state.treeState.applyTo(myTree, treeModel.getRoot());

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

  void selectItem(Object item) {
    if (myLastSelection == null || !myLastSelection.getValue().equals(item)) {
      TreeUtil.select(myTree, new NodeSelectionVisitor(item), path -> {});
    }
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

  private static class NodeSelectionVisitor implements TreeVisitor {
    private final Object myValue;

    NodeSelectionVisitor(Object value) {
      myValue = value;
    }

    @NotNull
    @Override
    public Action visit(@NotNull TreePath path) {
      Object node = path.getLastPathComponent();
      if (node instanceof ServiceViewItem && ((ServiceViewItem)node).getValue().equals(myValue)) return Action.INTERRUPT;

      return Action.CONTINUE;
    }
  }
}
