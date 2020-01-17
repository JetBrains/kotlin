// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

public class EditRunConfigurationsAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null && project.isDisposed()) {
      return;
    }
    if (project == null) {
      //setup template project configurations
      project = ProjectManager.getInstance().getDefaultProject();
    }
    new EditConfigurationsDialog(project).show();
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabled(project == null || !DumbService.isDumb(project));
    if (ActionPlaces.RUN_CONFIGURATIONS_COMBOBOX.equals(e.getPlace())) {
      presentation.setText(ExecutionBundle.message("edit.configuration.action"));
      presentation.setDescription(presentation.getText());
    }
  }
}
