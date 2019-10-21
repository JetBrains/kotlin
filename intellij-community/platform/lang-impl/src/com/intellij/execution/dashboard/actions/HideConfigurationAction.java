// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardManagerImpl;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class HideConfigurationAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    JBIterable<RunDashboardRunConfigurationNode> nodes = RunDashboardActionUtils.getTargets(e);
    e.getPresentation().setEnabledAndVisible(nodes.isNotEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    Set<RunConfiguration> configurations =
      RunDashboardActionUtils.getTargets(e).map(node -> node.getConfigurationSettings().getConfiguration()).toSet();
    ((RunDashboardManagerImpl)RunDashboardManager.getInstance(project)).hideConfigurations(configurations);
  }
}
