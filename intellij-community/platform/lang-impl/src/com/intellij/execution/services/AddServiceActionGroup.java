// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import static com.intellij.execution.services.ServiceViewActionProvider.getSelectedView;

public class AddServiceActionGroup extends DefaultActionGroup implements DumbAware {
  @Override
  public void update(@NotNull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    e.getPresentation().setEnabled(selectedView != null);
  }

  @Override
  public boolean canBePerformed(@NotNull DataContext context) {
    return false;
  }
}
