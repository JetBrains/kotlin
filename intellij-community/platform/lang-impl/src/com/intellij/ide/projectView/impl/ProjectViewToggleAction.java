// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class ProjectViewToggleAction extends ToggleAction implements DumbAware {
  ProjectViewToggleAction(@NotNull String text, @Nullable String description) {
    super(text, description, null);
  }

  abstract boolean isSupported(@NotNull ProjectViewImpl view, @NotNull String id);

  abstract boolean isSelected(@NotNull ProjectViewImpl view, @NotNull String id);

  abstract void setSelected(@NotNull ProjectViewImpl view, @NotNull String id, boolean flag);

  @Override
  public boolean isSelected(@NotNull AnActionEvent event) {
    ProjectViewImpl view = getProjectView(event);
    String id = view == null ? null : view.getCurrentViewId();
    return id != null && isSelected(view, id);
  }

  @Override
  public void setSelected(@NotNull AnActionEvent event, boolean flag) {
    ProjectViewImpl view = getProjectView(event);
    String id = view == null ? null : view.getCurrentViewId();
    if (id != null) setSelected(view, id, flag);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    ProjectViewImpl view = getProjectView(event);
    String id = view == null ? null : view.getCurrentViewId();
    event.getPresentation().setEnabledAndVisible(id != null && isSupported(view, id));
  }

  @Nullable
  private static ProjectViewImpl getProjectView(AnActionEvent event) {
    Project project = event == null ? null : event.getProject();
    ProjectView view = project == null ? null : ProjectView.getInstance(project);
    return view instanceof ProjectViewImpl ? (ProjectViewImpl)view : null;
  }
}
