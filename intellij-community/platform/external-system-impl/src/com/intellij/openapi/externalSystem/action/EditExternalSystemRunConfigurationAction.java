// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.RunConfigurationNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class EditExternalSystemRunConfigurationAction extends ExternalSystemAction {

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;
    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null || selectedNodes.size() != 1) return false;
    return selectedNodes.get(0) instanceof RunConfigurationNode;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = getProject(e);
    assert project != null;
    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null || selectedNodes.size() != 1 || !(selectedNodes.get(0) instanceof RunConfigurationNode)) return;

    RunnerAndConfigurationSettings settings = ((RunConfigurationNode)selectedNodes.get(0)).getSettings();
    assert settings != null;
    ExternalSystemActionsCollector.trigger(project, getSystemId(e), this, e);
    RunManager.getInstance(project).setSelectedConfiguration(settings);
    EditConfigurationsDialog dialog = new EditConfigurationsDialog(project);
    dialog.show();
  }
}
