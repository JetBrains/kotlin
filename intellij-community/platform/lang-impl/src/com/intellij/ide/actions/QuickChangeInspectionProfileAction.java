// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import org.jetbrains.annotations.NotNull;

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
    group.add(new DumbAwareAction(scheme.getName(), "", scheme == current ? AllIcons.Actions.Forward : ourNotCurrentAction) {
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
