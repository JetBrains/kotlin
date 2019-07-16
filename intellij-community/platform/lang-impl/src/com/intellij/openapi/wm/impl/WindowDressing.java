// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public final class WindowDressing implements ProjectManagerListener {
  @Override
  public void projectOpened(@NotNull Project project) {
    getWindowActionGroup().addProject(project);
  }

  @Override
  public void projectClosed(@NotNull Project project) {
    getWindowActionGroup().removeProject(project);
  }

  @NotNull
  public static ProjectWindowActionGroup getWindowActionGroup() {
    return (ProjectWindowActionGroup)ActionManager.getInstance().getAction("OpenProjectWindows");
  }
}
