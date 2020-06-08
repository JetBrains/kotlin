// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.dashboard.RunDashboardServiceViewContributor;
import com.intellij.execution.dashboard.tree.GroupingNode;
import com.intellij.execution.services.ServiceViewActionUtils;
import com.intellij.execution.services.ServiceViewManager;
import com.intellij.execution.services.ServiceViewManagerImpl;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class RunDashboardActionUtils {
  private RunDashboardActionUtils() {
  }

  @NotNull
  static JBIterable<RunDashboardRunConfigurationNode> getTargets(@NotNull AnActionEvent e) {
    return ServiceViewActionUtils.getTargets(e, RunDashboardRunConfigurationNode.class);
  }

  @Nullable
  static RunDashboardRunConfigurationNode getTarget(@NotNull AnActionEvent e) {
    return ServiceViewActionUtils.getTarget(e, RunDashboardRunConfigurationNode.class);
  }

  @NotNull
  static JBIterable<RunDashboardRunConfigurationNode> getLeafTargets(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return JBIterable.empty();

    JBIterable<Object> roots = JBIterable.of(e.getData(PlatformDataKeys.SELECTED_ITEMS));
    Set<RunDashboardRunConfigurationNode> result = new LinkedHashSet<>();
    if (!getLeaves(project, e, roots.toList(), Collections.emptyList(), result)) return JBIterable.empty();

    return JBIterable.from(result);
  }

  private static boolean getLeaves(Project project, AnActionEvent e, List<Object> items, List<Object> valueSubPath,
                                   Set<RunDashboardRunConfigurationNode> result) {
    for (Object item : items) {
      if (item instanceof RunDashboardServiceViewContributor || item instanceof GroupingNode) {
        List<Object> itemSubPath = new ArrayList<>(valueSubPath);
        itemSubPath.add(item);
        List<Object> children = ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getChildrenSafe(e, itemSubPath);
        if (!getLeaves(project, e, children, itemSubPath, result)) {
          return false;
        }
      }
      else if (item instanceof RunDashboardRunConfigurationNode) {
        result.add((RunDashboardRunConfigurationNode)item);
      }
      else if (item instanceof AbstractTreeNode) {
        AbstractTreeNode<?> parent = ((AbstractTreeNode<?>)item).getParent();
        if (parent instanceof RunDashboardRunConfigurationNode) {
          result.add((RunDashboardRunConfigurationNode)parent);
        }
        else {
          return false;
        }
      }
      else {
        return false;
      }
    }
    return true;
  }
}
