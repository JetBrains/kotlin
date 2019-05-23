// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroupByTypeAction extends ToggleAction implements DumbAware {
  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    return selectedView != null && selectedView.isGroupByType();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    ServiceView selectedView = getSelectedView(e);
    if (selectedView != null) {
      selectedView.setGroupByType(state);
    }
  }

  @Nullable
  private static ServiceView getSelectedView(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return null;

    return ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getSelectedView();
  }
}
