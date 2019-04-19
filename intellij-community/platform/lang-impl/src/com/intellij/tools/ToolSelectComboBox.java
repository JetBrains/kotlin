// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToolSelectComboBox extends BaseToolSelectComboBox<Tool> {
  @Nullable
  private final Project myProject;

  public ToolSelectComboBox() {
    this(null);
  }

  public ToolSelectComboBox(@Nullable Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  protected BaseToolManager<Tool> getToolManager() {
    return ToolManager.getInstance();
  }

  @Override
  @NotNull
  protected ToolSelectDialog getToolSelectDialog(@Nullable String toolIdToSelect) {
    return new ToolSelectDialog(myProject, toolIdToSelect, new ToolsPanel());
  }
}
