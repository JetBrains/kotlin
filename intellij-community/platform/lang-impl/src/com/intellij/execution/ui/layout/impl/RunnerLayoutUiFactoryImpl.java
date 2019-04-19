/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RunnerLayoutUiFactoryImpl extends RunnerLayoutUi.Factory {
  private final Project myProject;

  public RunnerLayoutUiFactoryImpl(Project project, ExecutionManager executionManager) {
    myProject = project;
    executionManager.getContentManager(); // ensure dockFactory is registered
  }

  @NotNull
  @Override
  public RunnerLayoutUi create(@NotNull final String runnerId, @NotNull final String runnerTitle, @NotNull final String sessionName, @NotNull final Disposable parent) {
    return new RunnerLayoutUiImpl(myProject, parent, runnerId, runnerTitle, sessionName);
  }
}
