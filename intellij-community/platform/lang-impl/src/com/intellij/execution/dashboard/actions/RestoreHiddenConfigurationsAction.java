// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.*;
import com.intellij.execution.dashboard.tree.ConfigurationTypeDashboardGroupingRule;
import com.intellij.execution.dashboard.tree.GroupingNode;
import com.intellij.execution.dashboard.tree.RunDashboardGroupImpl;
import com.intellij.execution.services.ServiceViewActionUtils;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
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
      e.getPresentation().setEnabledAndVisible(hasHiddenConfiguration(project));
      return;
    }
    if (!PropertiesComponent.getInstance(project).getBoolean(ConfigurationTypeDashboardGroupingRule.NAME, true)) {
      JBIterable<RunDashboardNode> nodes = ServiceViewActionUtils.getTargets(e, RunDashboardNode.class);
      e.getPresentation().setEnabledAndVisible(nodes.isNotEmpty() && hasHiddenConfiguration(project));
      return;
    }
    Set<ConfigurationType> types = getTargetTypes(e);
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
    if (root != null ||
        !PropertiesComponent.getInstance(project).getBoolean(ConfigurationTypeDashboardGroupingRule.NAME, true)) {
      // Restore all hidden configurations if action is invoked on Run Dashboard contributor root node or
      // when grouping by configuration type is disabled.
      RunDashboardManagerImpl runDashboardManager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
      runDashboardManager.restoreConfigurations(new THashSet<>(runDashboardManager.getHiddenConfigurations()));
      return;
    }

    Set<ConfigurationType> types = getTargetTypes(e);
    RunDashboardManagerImpl runDashboardManager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
    List<RunConfiguration> configurations =
      ContainerUtil.filter(runDashboardManager.getHiddenConfigurations(), configuration -> types.contains(configuration.getType()));
    runDashboardManager.restoreConfigurations(configurations);
  }

  private static boolean hasHiddenConfiguration(Project project) {
    return !((RunDashboardManagerImpl)RunDashboardManager.getInstance(project)).getHiddenConfigurations().isEmpty();
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
