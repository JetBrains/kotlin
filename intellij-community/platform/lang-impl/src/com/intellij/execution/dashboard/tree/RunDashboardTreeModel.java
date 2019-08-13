/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.dashboard.tree;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.RowsDnDSupport;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.EditableModel;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.Comparator;

/**
 * @author Konstantin Aleev
 */
public class RunDashboardTreeModel extends DefaultTreeModel implements EditableModel, RowsDnDSupport.RefinedDropSupport {
  private final Project myProject;
  private final Tree myTree;

  public RunDashboardTreeModel(TreeNode root, @NotNull Project project, @NotNull Tree tree) {
    super(root);
    myProject = project;
    myTree = tree;
  }

  @Override
  public void addRow() {
  }

  @Override
  public void exchangeRows(int oldIndex, int newIndex) {
  }

  @Override
  public boolean canExchangeRows(int oldIndex, int newIndex) {
    return false;
  }

  @Override
  public void removeRow(int idx) {
  }

  @Override
  public boolean isDropInto(JComponent component, int oldIndex, int newIndex) {
    if (myProject.isDisposed() || DumbService.getInstance(myProject).isDumb()) return false;


    RunDashboardRunConfigurationNode oldNode = getRunConfigurationNode(oldIndex);
    if (oldNode == null) return false;

    GroupingNode newNode = getGroupingNode(newIndex);
    return newNode != null && newNode.getGroup() instanceof FolderDashboardGroupingRule.FolderDashboardGroup;
  }

  @Override
  public boolean canDrop(int oldIndex, int newIndex, @NotNull Position position) {
    if (myProject.isDisposed() || DumbService.getInstance(myProject).isDumb()) return false;

    RunDashboardRunConfigurationNode oldNode = getRunConfigurationNode(oldIndex);
    if (oldNode == null) return false;

    if (position == Position.INTO) {
      GroupingNode newNode = getGroupingNode(newIndex);
      return newNode != null && newNode.getGroup() instanceof FolderDashboardGroupingRule.FolderDashboardGroup;
    }

    return getRunConfigurationNode(newIndex) != null;
  }

  @Override
  public void drop(int oldIndex, int newIndex, @NotNull Position position) {
    if (myProject.isDisposed() || DumbService.getInstance(myProject).isDumb()) return;

    RunDashboardRunConfigurationNode oldNode = getRunConfigurationNode(oldIndex);
    if (oldNode == null) return;

    final RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(myProject);
    runManager.fireBeginUpdate();
    try {
      if (position == Position.INTO) {
        GroupingNode newNode = getGroupingNode(newIndex);
        if (newNode == null || !(newNode.getGroup() instanceof FolderDashboardGroupingRule.FolderDashboardGroup)) return;

        oldNode.getConfigurationSettings().setFolderName(newNode.getGroup().getName());
        return;
      }

      RunDashboardRunConfigurationNode newNode = getRunConfigurationNode(newIndex);
      if (newNode == null) return;

      oldNode.getConfigurationSettings().setFolderName(newNode.getConfigurationSettings().getFolderName());

      TObjectIntHashMap<RunnerAndConfigurationSettings> indices = new TObjectIntHashMap<>();
      int i = 0;
      for (RunnerAndConfigurationSettings each : runManager.getAllSettings()) {
        if (each.equals(oldNode.getConfigurationSettings())) continue;

        if (each.equals(newNode.getConfigurationSettings())) {
          if (position == Position.ABOVE) {
            indices.put(oldNode.getConfigurationSettings(), i++);
            indices.put(newNode.getConfigurationSettings(), i++);
          }
          else if (position == Position.BELOW) {
            indices.put(newNode.getConfigurationSettings(), i++);
            indices.put(oldNode.getConfigurationSettings(), i++);
          }
        }
        else {
          indices.put(each, i++);
        }
      }
      runManager.setOrder(Comparator.comparingInt(indices::get));
    }
    finally {
      runManager.fireEndUpdate();
    }
  }

  @Nullable
  private RunDashboardRunConfigurationNode getRunConfigurationNode(int index) {
    return getDashboardNode(index, RunDashboardRunConfigurationNode.class);
  }

  @Nullable
  private GroupingNode getGroupingNode(int index) {
    return getDashboardNode(index, GroupingNode.class);
  }

  @Nullable <T> T getDashboardNode(int index, Class<T> nodeClass) {
    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)myTree.getPathForRow(index).getLastPathComponent();
    return ObjectUtils.tryCast(treeNode.getUserObject(), nodeClass);
  }
}
