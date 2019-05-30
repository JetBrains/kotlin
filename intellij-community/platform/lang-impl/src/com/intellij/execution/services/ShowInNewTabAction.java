// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceViewDragHelper.ServiceViewDragBean;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.intellij.execution.services.ServiceViewActionProvider.getSelectedView;

public class ShowInNewTabAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    ServiceView serviceView = getSelectedView(e);
    e.getPresentation().setEnabled(serviceView != null && !serviceView.getSelectedItems().isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    ServiceView serviceView = getSelectedView(e);
    if (serviceView == null) return;

    ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project))
      .extract(new ServiceViewDragBean(serviceView, serviceView.getSelectedItems()));
  }
}
