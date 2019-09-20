// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.intellij.execution.dashboard.actions.RunDashboardActionUtils.getTarget;

public class RestoreConfigurationAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    RunDashboardRunConfigurationNode node = project == null ? null : getTarget(e);
    boolean enabled = node != null && !RunManager.getInstance(project).hasSettings(node.getConfigurationSettings());
    e.getPresentation().setEnabled(enabled);
    boolean popupPlace = ActionPlaces.isPopupPlace(e.getPlace());
    e.getPresentation().setVisible(enabled || !popupPlace);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    RunDashboardRunConfigurationNode node = project == null ? null : getTarget(e);
    if (node == null) return;

    RunManager runManager = RunManager.getInstance(project);
    RunnerAndConfigurationSettings settings = node.getConfigurationSettings();
    runManager.setUniqueNameIfNeeded(settings.getConfiguration());
    if (settings.isTemporary()) {
      runManager.setTemporaryConfiguration(settings);
    }
    else {
      runManager.addConfiguration(settings, settings.isShared());
    }
  }
}
