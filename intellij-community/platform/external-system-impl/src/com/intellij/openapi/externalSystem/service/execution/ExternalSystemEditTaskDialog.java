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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ExternalSystemEditTaskDialog extends DialogWrapper {

  @NotNull private final ExternalSystemTaskExecutionSettings myTaskExecutionSettings;
  @NotNull private final ExternalSystemTaskSettingsControl myControl;
  @Nullable private JComponent contentPane;
  @NotNull private final Project myProject;

  public ExternalSystemEditTaskDialog(@NotNull Project project,
                                      @NotNull ExternalSystemTaskExecutionSettings taskExecutionSettings,
                                      @NotNull ProjectSystemId externalSystemId) {
    super(project, true);
    myProject = project;
    myTaskExecutionSettings = taskExecutionSettings;

    setTitle(ExternalSystemBundle.message("tasks.edit.task.title", externalSystemId.getReadableName()));
    myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
    myControl.setOriginalSettings(taskExecutionSettings);
    setModal(true);
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    if (contentPane == null) {
      contentPane = new PaintAwarePanel(new GridBagLayout());
      myControl.fillUi((PaintAwarePanel)contentPane, 0);
      myControl.reset(myProject);
    }
    return contentPane;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  protected void dispose() {
    super.dispose();
    myControl.disposeUIResources();
  }

  @Override
  protected void doOKAction() {
    myControl.apply(myTaskExecutionSettings);
    super.doOKAction();
  }
}
