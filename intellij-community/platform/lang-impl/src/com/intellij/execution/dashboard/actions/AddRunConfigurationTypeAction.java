// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.impl.RunDashboardTypesPanel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AddRunConfigurationTypeAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(project);
    Set<String> addedTypes = runDashboardManager.getTypes();
    RunDashboardTypesPanel.showAddPopup(project, addedTypes,
                                        newTypes -> {
                                          Set<String> updatedTypes = new HashSet<>(addedTypes);
                                          for (ConfigurationType type : newTypes) {
                                              updatedTypes.add(type.getId());
                                          }
                                          runDashboardManager.setTypes(updatedTypes);
                                        },
                                        popup -> popup.showInBestPositionFor(e.getDataContext()));
  }
}
