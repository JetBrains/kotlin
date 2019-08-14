// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.view.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.InputEvent;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Vladislav.Soroka
 */
public class SelectExternalSystemNodeDialog extends DialogWrapper {

  @NotNull
  private final SimpleTree myTree;
  @Nullable
  private final Predicate<SimpleNode> mySelector;
  @Nullable
  protected Boolean groupTasks;
  @Nullable
  protected Boolean useTasksNode;

  public SelectExternalSystemNodeDialog(@NotNull ProjectSystemId systemId,
                                        @NotNull Project project,
                                        @NotNull String title,
                                        Class<? extends ExternalSystemNode> nodeClass,
                                        @Nullable Predicate<SimpleNode> selector) {
    //noinspection unchecked
    this(systemId, project, title, new Class[]{nodeClass}, selector);
  }

  public SelectExternalSystemNodeDialog(@NotNull ProjectSystemId systemId,
                                        @NotNull Project project,
                                        @NotNull String title,
                                        final Class<? extends ExternalSystemNode>[] nodeClasses,
                                        @Nullable Predicate<SimpleNode> selector) {
    super(project, false);
    mySelector = selector;
    setTitle(title);

    myTree = new SimpleTree();
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    ExternalProjectsView projectsView = ExternalProjectsManagerImpl.getInstance(project).getExternalProjectsView(systemId);
    if (projectsView == null) {
      ToolWindow toolWindow = ExternalToolWindowManager.getToolWindow(project, systemId);
      if (toolWindow instanceof ToolWindowEx) {
        projectsView = new ExternalProjectsViewImpl(project, (ToolWindowEx)toolWindow, systemId);
        Disposer.register(getDisposable(), (ExternalProjectsViewImpl)projectsView);
      }
    }
    if(projectsView != null) {
      final ExternalProjectsStructure treeStructure = new ExternalProjectsStructure(project, myTree) {
        @Override
        protected Class<? extends ExternalSystemNode>[] getVisibleNodesClasses() {
          return nodeClasses;
        }

        @NotNull
        @Override
        public Object getRootElement() {
          Object rootElement = super.getRootElement();
          return customizeProjectsTreeRoot(rootElement);
        }
      };
      Disposer.register(myDisposable, treeStructure);
      treeStructure.init(new ExternalProjectsViewAdapter(projectsView) {
        @Nullable
        @Override
        public ExternalProjectsStructure getStructure() {
          return treeStructure;
        }

        @Override
        public void updateUpTo(ExternalSystemNode node) {
          treeStructure.updateUpTo(node);
        }

        @Override
        public boolean getGroupTasks() {
          return groupTasks != null ? groupTasks : super.getGroupTasks();
        }

        @Override
        public boolean useTasksNode() {
          return useTasksNode != null ? useTasksNode : super.useTasksNode();
        }

        @Override
        public void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
          SelectExternalSystemNodeDialog.this.handleDoubleClickOrEnter(node, actionId, inputEvent);
        }
      });

      final Collection<ExternalProjectInfo> projectsData =
        ProjectDataManager.getInstance().getExternalProjectsData(project, systemId);

      final List<DataNode<ProjectData>> dataNodes =
        ContainerUtil.mapNotNull(projectsData, info -> info.getExternalProjectStructure());
      treeStructure.updateProjects(dataNodes);
      TreeUtil.expandAll(myTree);

      if (mySelector != null) {
        TreeUtil.promiseSelect(myTree, path -> {
          SimpleNode node = TreeUtil.getLastUserObject(SimpleNode.class, path);
          return node != null && mySelector.test(node) ? TreeVisitor.Action.INTERRUPT : TreeVisitor.Action.CONTINUE;
        });
      }
    }

    init();
  }

  protected Object customizeProjectsTreeRoot(Object rootElement) {
    return rootElement;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  protected void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
  }

  protected SimpleNode getSelectedNode() {
    return myTree.getNodeFor(myTree.getSelectionPath());
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    final JScrollPane pane = ScrollPaneFactory.createScrollPane(myTree);
    pane.setPreferredSize(JBUI.size(320, 400));
    return pane;
  }
}
