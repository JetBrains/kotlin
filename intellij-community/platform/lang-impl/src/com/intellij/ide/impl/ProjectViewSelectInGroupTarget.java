// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.impl;

import com.intellij.ide.CompositeSelectInTarget;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.IdeUICustomization;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class ProjectViewSelectInGroupTarget implements CompositeSelectInTarget, DumbAware {
  @Override
  @NotNull
  public Collection<SelectInTarget> getSubTargets(@NotNull SelectInContext context) {
    return ProjectView.getInstance(context.getProject()).getSelectInTargets();
  }

  @Override
  public boolean canSelect(SelectInContext context) {
    ProjectView projectView = ProjectView.getInstance(context.getProject());
    Collection<SelectInTarget> targets = projectView.getSelectInTargets();
    for (SelectInTarget projectViewTarget : targets) {
      if (projectViewTarget.canSelect(context)) return true;
    }
    return false;
  }

  @Override
  public void selectIn(final SelectInContext context, final boolean requestFocus) {
    ProjectView projectView = ProjectView.getInstance(context.getProject());
    Collection<SelectInTarget> targets = projectView.getSelectInTargets();
    Collection<SelectInTarget> targetsToCheck = new LinkedHashSet<>();
    String currentId = projectView.getCurrentViewId();
    for (SelectInTarget projectViewTarget : targets) {
      if (Objects.equals(currentId, projectViewTarget.getMinorViewId())) {
        targetsToCheck.add(projectViewTarget);
        break;
      }
    }
    targetsToCheck.addAll(targets);
    for (SelectInTarget target : targetsToCheck) {
      if (context.selectIn(target, requestFocus)) break;
    }
  }

  @Override
  public String getToolWindowId() {
    return ToolWindowId.PROJECT_VIEW;
  }

  @Override
  public String getMinorViewId() {
    return null;
  }

  @Override
  public String toString() {
    return IdeUICustomization.getInstance().projectMessage("select.in.item.project.view");
  }
}
