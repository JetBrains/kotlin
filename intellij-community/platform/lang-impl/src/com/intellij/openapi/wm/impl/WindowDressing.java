// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class WindowDressing implements ProjectComponent {
  @NotNull private final Project myProject;

  public WindowDressing(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    getWindowActionGroup().addProject(myProject);
  }

  @Override
  public void projectClosed() {
    getWindowActionGroup().removeProject(myProject);
  }

  public static ProjectWindowActionGroup getWindowActionGroup() {
    return (ProjectWindowActionGroup)ActionManager.getInstance().getAction("OpenProjectWindows");
  }
}
