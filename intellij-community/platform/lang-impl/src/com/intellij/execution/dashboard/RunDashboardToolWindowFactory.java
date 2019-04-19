/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.execution.dashboard;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author konstantin.aleev
 */
public class RunDashboardToolWindowFactory implements ToolWindowFactory, Condition<Project>, DumbAware {
  @Override
  public boolean value(Project project) {
    return !Registry.is("ide.service.view") && !RunDashboardManager.getInstance(project).getTypes().isEmpty();
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return !Registry.is("ide.service.view") && RunDashboardManager.getInstance(project).isToolWindowAvailable();
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    RunDashboardManager.getInstance(project).createToolWindowContent(toolWindow);
  }
}
