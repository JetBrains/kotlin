/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuickChangeInspectionProfileAction extends QuickSwitchSchemeAction {
  @Override
  protected void fillActions(Project project, @NotNull DefaultActionGroup group, @NotNull DataContext dataContext) {
    final ProjectInspectionProfileManager projectProfileManager = ProjectInspectionProfileManager.getInstance(project);
    InspectionProfile current = projectProfileManager.getCurrentProfile();
    for (InspectionProfile profile : projectProfileManager.getProfiles()) {
      addScheme(group, projectProfileManager, current, profile);
    }
  }

  private static void addScheme(final DefaultActionGroup group,
                                final ProjectInspectionProfileManager projectProfileManager,
                                final InspectionProfile current,
                                final InspectionProfile scheme) {
    group.add(new DumbAwareAction(scheme.getName(), "", scheme == current ? ourCurrentAction : ourNotCurrentAction) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        projectProfileManager.setRootProfile(scheme.getName());
      }
    });
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = getEventProject(e);
    e.getPresentation().setEnabledAndVisible(project != null && InspectionProjectProfileManager.getInstance(project).getProfiles().size() > 1);
  }
}
