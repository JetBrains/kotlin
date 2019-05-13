// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.List;

class ServiceViewActionProvider {
  @NonNls private static final String SERVICE_VIEW_NODE_TOOLBAR = "ServiceViewNodeToolbar";
  @NonNls private static final String SERVICE_VIEW_NODE_POPUP = "ServiceViewNodePopup";
  @NonNls private static final String SERVICE_VIEW_TREE_TOOLBAR = "ServiceViewTreeToolbar";

  private static final ServiceViewActionProvider ourInstance = new ServiceViewActionProvider();

  static ServiceViewActionProvider getInstance() {
    return ourInstance;
  }

  JComponent createServiceToolbar(@NotNull JComponent component) {
    ActionGroup actions = (ActionGroup)ActionManager.getInstance().getAction(SERVICE_VIEW_NODE_TOOLBAR);
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.SERVICES_TOOLBAR, actions, false);
    toolbar.setTargetComponent(component);
    return toolbar.getComponent();
  }

  void installPopupHandler(@NotNull JComponent component) {
    ActionGroup actions = (ActionGroup)ActionManager.getInstance().getAction(SERVICE_VIEW_NODE_POPUP);
    PopupHandler.installPopupHandler(component, actions, ActionPlaces.SERVICES_POPUP, ActionManager.getInstance());
  }

  JComponent createMasterComponentToolbar(@NotNull JComponent component) {
    JPanel toolBarPanel = new JPanel(new BorderLayout());

    DefaultActionGroup group = new DefaultActionGroup();

    if (component instanceof JTree) {
      TreeExpander treeExpander = new ServiceViewTreeExpander((JTree)component);
      AnAction expandAllAction = CommonActionsManager.getInstance().createExpandAllAction(treeExpander, component);
      group.add(expandAllAction);
      AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, component);
      group.add(collapseAllAction);
      group.addSeparator();
    }

    group.addSeparator();
    AnAction treeActions = ActionManager.getInstance().getAction(SERVICE_VIEW_TREE_TOOLBAR);
    treeActions.registerCustomShortcutSet(component, null);
    group.add(treeActions);

    ActionToolbar treeActionsToolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.SERVICES_TOOLBAR, group, true);
    toolBarPanel.add(treeActionsToolBar.getComponent(), BorderLayout.CENTER);
    treeActionsToolBar.setTargetComponent(component);

    return toolBarPanel;
  }

  private static class ServiceViewTreeExpander extends DefaultTreeExpander {
    private final TreeModel myTreeModel;
    private boolean myFlat;

    ServiceViewTreeExpander(JTree tree) {
      super(tree);
      myTreeModel = tree.getModel();
      myTreeModel.addTreeModelListener(new TreeModelAdapter() {
        @Override
        protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
          myFlat = isFlat();
        }
      });
    }

    @Override
    public boolean canExpand() {
      return super.canExpand() && !myFlat;
    }

    @Override
    public boolean canCollapse() {
      return super.canCollapse() && !myFlat;
    }

    private boolean isFlat() {
      Object root = myTreeModel.getRoot();
      if (root == null) return false;

      int childCount = myTreeModel.getChildCount(root);
      for (int i = 0; i < childCount; i++) {
        Object child = myTreeModel.getChild(root, i);
        if (!myTreeModel.isLeaf(child)) {
          return false;
        }
      }
      return true;
    }
  }

  @NotNull
  private static AnAction[] doGetActions(@Nullable AnActionEvent e, boolean toolbar) {
    if (e == null) return AnAction.EMPTY_ARRAY;

    Project project = e.getProject();
    if (project == null) return AnAction.EMPTY_ARRAY;

    Component contextComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    while (contextComponent != null && !(contextComponent instanceof ServiceView)) {
      contextComponent = contextComponent.getParent();
    }
    if (contextComponent == null) return AnAction.EMPTY_ARRAY;

    List<ServiceViewItem> selectedItems = ((ServiceView)contextComponent).getSelectedItems();
    if (selectedItems.isEmpty()) return AnAction.EMPTY_ARRAY;

    ServiceViewDescriptor descriptor = null;
    if (selectedItems.size() == 1) {
      descriptor = selectedItems.get(0).getViewDescriptor();
    }
    else {
      ServiceViewContributor contributor = getTheOnlyRootContributor(selectedItems);
      descriptor = contributor == null ? null : contributor.getViewDescriptor();
    }
    if (descriptor == null) return AnAction.EMPTY_ARRAY;

    ActionGroup group = toolbar ? descriptor.getToolbarActions() : descriptor.getPopupActions();
    return group == null ? AnAction.EMPTY_ARRAY : group.getChildren(e);
  }

  @Nullable
  private static ServiceViewContributor getTheOnlyRootContributor(@NotNull List<ServiceViewItem> items) {
    ServiceViewContributor contributor = null;
    for (ServiceViewItem item : items) {
      if (contributor == null) {
        contributor = item.getRootContributor();
      }
      else if (!contributor.equals(item.getRootContributor())) {
        return null;
      }
    }
    return contributor;
  }

  public static class ItemToolbarActionGroup extends ActionGroup {
    private final static AnAction[] FAKE_GROUP = new AnAction[]{new DumbAwareAction(null, null, EmptyIcon.ICON_16) {
      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(false);
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
      }
    }};

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      AnAction[] actions = doGetActions(e, true);
      return actions.length != 0 ? actions : FAKE_GROUP;
    }
  }

  public static class ItemPopupActionGroup extends ActionGroup {
    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return doGetActions(e, false);
    }
  }
}
