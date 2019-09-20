// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardNode;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.dashboard.tree.GroupingNode;
import com.intellij.execution.dashboard.tree.RunDashboardGroupImpl;
import com.intellij.execution.services.ServiceViewActionUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RemoveRunConfigurationTypeAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Set<ConfigurationType> types = getTargetTypes(e);
    if (types.isEmpty()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(true);
    ConfigurationType type = ContainerUtil.getOnlyItem(types);
    presentation.setText(ExecutionBundle.message("run.dashboard.remove.run.configuration.type.action.name",
                                                 type != null ? type.getDisplayName() : "Selected Types"));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(project);
    Set<String> types = new HashSet<>(runDashboardManager.getTypes());
    Set<ConfigurationType> targetTypes = getTargetTypes(e);
    for (ConfigurationType type : targetTypes) {
      types.remove(type.getId());
    }
    runDashboardManager.setTypes(types);
  }

  private static Set<ConfigurationType> getTargetTypes(AnActionEvent e) {
    JBIterable<RunDashboardNode> targets = ServiceViewActionUtils.getTargets(e, RunDashboardNode.class);
    if (targets.isEmpty()) return Collections.emptySet();

    Set<ConfigurationType> types = new HashSet<>();
    for (RunDashboardNode node : targets) {
      if (node instanceof RunDashboardRunConfigurationNode) {
        types.add(((RunDashboardRunConfigurationNode)node).getConfigurationSettings().getType());
      }
      else if (node instanceof GroupingNode) {
        RunDashboardGroupImpl<?> group = (RunDashboardGroupImpl<?>)((GroupingNode)node).getGroup();
        ConfigurationType type = ObjectUtils.tryCast(group.getValue(), ConfigurationType.class);
        if (type == null) {
          return Collections.emptySet();
        }
        types.add(type);
      }
      else {
        return Collections.emptySet();
      }
    }
    return types;
  }
}
