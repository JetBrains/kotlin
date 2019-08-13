/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.action.ExternalSystemToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Vladislav.Soroka
 */
public class ToggleOfflineAction extends ExternalSystemToggleAction {

  @Override
  protected boolean isVisible(@NotNull AnActionEvent e) {
    if (!super.isVisible(e)) return false;
    return GradleConstants.SYSTEM_ID.equals(getSystemId(e));
  }

  @Override
  protected boolean doIsSelected(@NotNull AnActionEvent e) {
    return GradleSettings.getInstance(getProject(e)).isOfflineWork();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    Project project = getProject(e);
    GradleSettings.getInstance(project).setOfflineWork(state);
  }
}
