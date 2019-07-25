// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class ProjectViewToolWindowFactory implements ToolWindowFactory, DumbAware {
  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ((ProjectViewImpl) ProjectView.getInstance(project)).setupImpl(toolWindow);
  }

  @Override
  public void init(ToolWindow window) {
    window.setIcon(IconLoader.getIcon(ApplicationInfoEx.getInstanceEx().getToolWindowIconUrl()));
    window.setStripeTitle(IdeUICustomization.getInstance().getProjectViewTitle());
  }
}
