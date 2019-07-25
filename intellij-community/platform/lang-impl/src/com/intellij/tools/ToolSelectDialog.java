/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.tools;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ToolSelectDialog extends DialogWrapper {
  private final BaseToolsPanel myToolsPanel;

  public ToolSelectDialog(@Nullable Project project, @Nullable String actionIdToSelect, BaseToolsPanel toolsPanel) {
    super(project);
    myToolsPanel = toolsPanel;
    myToolsPanel.reset();
    init();
    pack();
    if (actionIdToSelect != null) {
      myToolsPanel.selectTool(actionIdToSelect);
    }
    setTitle(ToolsBundle.message("tools.dialog.title"));
  }

  @Override
  protected void doOKAction() {
    myToolsPanel.apply();
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myToolsPanel;
  }

  @Nullable
  public Tool getSelectedTool() {
    return myToolsPanel.getSingleSelectedTool();
  }

  public boolean isModified() {
    return myToolsPanel.isModified();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "com.intellij.tools.ToolSelectDialog.dimensionServiceKey";
  }
}
