// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import static com.intellij.execution.services.ServiceViewActionProvider.getSelectedView;

public class JumpToServicesAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    if (selectedView == null) return;

    selectedView.jumpToServices();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setVisible(false);
    ServiceView selectedView = getSelectedView(e);
    presentation.setEnabled(selectedView != null && !(selectedView.getModel() instanceof ServiceViewModel.SingeServiceModel));
  }
}
