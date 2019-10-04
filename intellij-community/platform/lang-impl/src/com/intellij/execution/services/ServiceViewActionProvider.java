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
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collections;
import java.util.List;

import static com.intellij.execution.services.ServiceViewDragHelper.getTheOnlyRootContributor;

class ServiceViewActionProvider {
  @NonNls private static final String SERVICE_VIEW_ITEM_TOOLBAR = "ServiceViewItemToolbar";
  @NonNls static final String SERVICE_VIEW_ITEM_POPUP = "ServiceViewItemPopup";
  @NonNls private static final String SERVICE_VIEW_TREE_TOOLBAR = "ServiceViewTreeToolbar";

  private static final ServiceViewActionProvider ourInstance = new ServiceViewActionProvider();

  static ServiceViewActionProvider getInstance() {
    return ourInstance;
  }

  ActionToolbar createServiceToolbar(@NotNull JComponent component) {
    ActionGroup actions = (ActionGroup)ActionManager.getInstance().getAction(SERVICE_VIEW_ITEM_TOOLBAR);
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.SERVICES_TOOLBAR, actions, false);
    toolbar.setTargetComponent(component);
    return toolbar;
  }

  JComponent wrapServiceToolbar(@NotNull ActionToolbar toolbar) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(toolbar.getComponent(), BorderLayout.CENTER);
    toolbar.getComponent().addComponentListener(new ComponentListener() {
      @Override
      public void componentResized(ComponentEvent e) {
      }

      @Override
      public void componentMoved(ComponentEvent e) {
      }

      @Override
      public void componentShown(ComponentEvent e) {
        wrapper.setVisible(true);
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        wrapper.setVisible(false);
      }
    });
    ActionToolbar prototypeToolbar = createPrototypeToolbar();
    JLabel spacer = new JLabel();
    spacer.setPreferredSize(new Dimension(prototypeToolbar.getComponent().getPreferredSize().width, 0));
    wrapper.add(spacer, BorderLayout.SOUTH);
    return wrapper;
  }

  void installPopupHandler(@NotNull JComponent component) {
    ActionGroup actions = (ActionGroup)ActionManager.getInstance().getAction(SERVICE_VIEW_ITEM_POPUP);
    PopupHandler.installPopupHandler(component, actions, ActionPlaces.SERVICES_POPUP, ActionManager.getInstance());
  }

  ActionToolbar createMasterComponentToolbar(@NotNull JComponent component) {
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
    treeActionsToolBar.setTargetComponent(component);

    return treeActionsToolBar;
  }

  List<AnAction> getAdditionalGearActions() {
    AnAction showServicesActions = ActionManager.getInstance().getAction("ServiceView.ShowServices");
    return showServicesActions == null ? Collections.emptyList() : Collections.singletonList(showServicesActions);
  }

  @Nullable
  static ServiceView getSelectedView(@NotNull AnActionEvent e) {
    return getSelectedView(e.getData(PlatformDataKeys.CONTEXT_COMPONENT));
  }

  @Nullable
  static ServiceView getSelectedView(@NotNull DataProvider provider) {
    return getSelectedView(ObjectUtils.tryCast(provider.getData(PlatformDataKeys.CONTEXT_COMPONENT.getName()), Component.class));
  }

  @Nullable
  private static ServiceView getSelectedView(@Nullable Component contextComponent) {
    while (contextComponent != null && !(contextComponent instanceof ServiceView)) {
      contextComponent = contextComponent.getParent();
    }
    return (ServiceView)contextComponent;
  }

  private static final AnAction EMPTY_ACTION = new DumbAwareAction(null, null, EmptyIcon.ICON_16) {
    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(false);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }
  };

  private static ActionToolbar createPrototypeToolbar() {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(EMPTY_ACTION);
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, false);
    toolbar.updateActionsImmediately();
    return toolbar;
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

    ServiceView serviceView = getSelectedView(e);
    if (serviceView == null) return AnAction.EMPTY_ARRAY;

    List<ServiceViewItem> selectedItems = serviceView.getSelectedItems();
    if (selectedItems.isEmpty()) return AnAction.EMPTY_ARRAY;

    ServiceViewDescriptor descriptor;
    if (selectedItems.size() == 1) {
      descriptor = selectedItems.get(0).getViewDescriptor();
    }
    else {
      ServiceViewContributor<?> contributor = getTheOnlyRootContributor(selectedItems);
      descriptor = contributor == null ? null : contributor.getViewDescriptor(project);
    }
    if (descriptor == null) return AnAction.EMPTY_ARRAY;

    ActionGroup group = toolbar ? descriptor.getToolbarActions() : descriptor.getPopupActions();
    return group == null ? AnAction.EMPTY_ARRAY : group.getChildren(e);
  }

  public static class ItemToolbarActionGroup extends ActionGroup {
    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return doGetActions(e, true);
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
