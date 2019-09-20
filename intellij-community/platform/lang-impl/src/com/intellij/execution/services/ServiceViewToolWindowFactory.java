// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowId;
import org.jetbrains.annotations.NotNull;

public class ServiceViewToolWindowFactory implements ToolWindowFactory, Condition<Project>, DumbAware {
  @Override
  public boolean value(Project project) {
    return ServiceModel.getContributors().length > 0;
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    // Init manager to check availability on background thread.
    ServiceViewManager.getInstance(project);
    return false;
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).createToolWindowContent(ToolWindowId.SERVICES, toolWindow);
  }
}
