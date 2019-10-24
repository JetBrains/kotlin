// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.intellij.execution.services.ServiceViewActionProvider.getSelectedView;

public class SplitByTypeAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    ServiceView selectedView = getSelectedView(e);
    if (selectedView == null || !ServiceViewManagerImpl.isMainView(selectedView)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    Presentation presentation = e.getPresentation();
    boolean enabled = ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).isSplitByTypeEnabled(selectedView);
    presentation.setEnabledAndVisible(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    ServiceView selectedView = getSelectedView(e);
    if (selectedView == null) return;

    ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).splitByType(selectedView);
  }
}
