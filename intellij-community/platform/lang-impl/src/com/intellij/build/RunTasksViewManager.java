// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class RunTasksViewManager extends TasksViewManager {
  public RunTasksViewManager(Project project) {
    super(project);
  }

  @NotNull
  @Override
  public String getViewName() {
    return "Run";
  }

  @Override
  protected Icon getContentIcon() {
    return AllIcons.RunConfigurations.TestState.Run;
  }
}
