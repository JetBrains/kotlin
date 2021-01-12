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
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class EditConfigurationAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
    boolean enabled = node != null && RunManager.getInstance(project).hasSettings(node.getConfigurationSettings());
    e.getPresentation().setEnabled(enabled);
    boolean popupPlace = ActionPlaces.isPopupPlace(e.getPlace());
    e.getPresentation().setVisible(enabled || !popupPlace);
    if (popupPlace) {
      e.getPresentation().setText(getTemplatePresentation().getText() + "...");
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
    if (node == null) return;

    RunDialog.editConfiguration(project, node.getConfigurationSettings(),
                                ExecutionBundle.message("run.dashboard.edit.configuration.dialog.title"));
  }
}
