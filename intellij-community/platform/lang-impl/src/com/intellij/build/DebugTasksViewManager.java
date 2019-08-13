// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class DebugTasksViewManager extends TasksViewManager   {
  public DebugTasksViewManager(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @NotNull
  @Override
  public String getViewName() {
    return "Debug";
  }

  @Override
  protected Icon getContentIcon() {
    return AllIcons.Actions.StartDebugger;
  }
}
