// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RunnerLayoutUiFactoryImpl extends RunnerLayoutUi.Factory {
  private final Project myProject;

  public RunnerLayoutUiFactoryImpl(Project project) {
    myProject = project;
    // ensure dockFactory is registered
    ExecutionManager.getInstance(project).getContentManager();
  }

  @NotNull
  @Override
  public RunnerLayoutUi create(@NotNull final String runnerId, @NotNull final String runnerTitle, @NotNull final String sessionName, @NotNull final Disposable parent) {
    return new RunnerLayoutUiImpl(myProject, parent, runnerId, runnerTitle, sessionName);
  }
}
