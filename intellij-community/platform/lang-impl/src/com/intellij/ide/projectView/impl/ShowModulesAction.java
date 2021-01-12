// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author anna
 * @deprecated added in {@link ProjectViewImpl} automatically
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
public final class ShowModulesAction extends ToggleAction implements DumbAware {
  private final Project myProject;
  private final String myId;

  public ShowModulesAction(@NotNull Project project, @NotNull String id) {
    super(IdeBundle.message("action.show.modules"), IdeBundle.message("action.description.show.modules"), AllIcons.Actions.GroupByModule);
    myProject = project;
    myId = id;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent event) {
    ProjectView view = getProjectView();
    return view != null && view.isShowModules(myId);
  }

  @Override
  public void setSelected(@NotNull AnActionEvent event, boolean flag) {
    ProjectView view = getProjectView();
    if (view != null) view.setShowModules(myId, flag);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    Presentation presentation = event.getPresentation();
    presentation.setEnabledAndVisible(hasModules() && isCurrentViewSelected(getProjectView()));
  }

  @Nullable
  private ProjectView getProjectView() {
    return myProject.isDisposed() ? null : ProjectView.getInstance(myProject);
  }

  private boolean isCurrentViewSelected(@Nullable ProjectView view) {
    return view != null && myId.equals(view.getCurrentViewId());
  }

  public static boolean hasModules() {
    return PlatformUtils.isIntelliJ();
  }
}
