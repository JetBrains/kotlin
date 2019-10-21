// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardManagerImpl;
import com.intellij.execution.dashboard.RunDashboardServiceViewContributor;
import com.intellij.execution.services.ServiceViewActionUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class RestoreHiddenConfigurationsAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    RunDashboardServiceViewContributor root = ServiceViewActionUtils.getTarget(e, RunDashboardServiceViewContributor.class);
    if (root != null) {
      Set<RunConfiguration> hiddenConfigurations =
        ((RunDashboardManagerImpl)RunDashboardManager.getInstance(project)).getHiddenConfigurations();
      e.getPresentation().setEnabledAndVisible(!hiddenConfigurations.isEmpty());
      return;
    }
    Set<ConfigurationType> types = RunDashboardActionUtils.getTargetTypes(e);
    if (types.isEmpty()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    Set<RunConfiguration> hiddenConfigurations =
      ((RunDashboardManagerImpl)RunDashboardManager.getInstance(project)).getHiddenConfigurations();
    List<RunConfiguration> configurations =
      ContainerUtil.filter(hiddenConfigurations, configuration -> types.contains(configuration.getType()));
    e.getPresentation().setEnabledAndVisible(!configurations.isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    RunDashboardServiceViewContributor root = ServiceViewActionUtils.getTarget(e, RunDashboardServiceViewContributor.class);
    if (root != null) {
      // Restore all hidden configurations.
      RunDashboardManagerImpl runDashboardManager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
      runDashboardManager.restoreConfigurations(new THashSet<>(runDashboardManager.getHiddenConfigurations()));
      return;
    }

    Set<ConfigurationType> types = RunDashboardActionUtils.getTargetTypes(e);
    RunDashboardManagerImpl runDashboardManager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
    List<RunConfiguration> configurations =
      ContainerUtil.filter(runDashboardManager.getHiddenConfigurations(), configuration -> types.contains(configuration.getType()));
    runDashboardManager.restoreConfigurations(configurations);
  }
}
