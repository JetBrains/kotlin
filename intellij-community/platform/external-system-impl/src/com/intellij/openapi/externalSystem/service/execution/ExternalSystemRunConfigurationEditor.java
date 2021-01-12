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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemRunConfigurationEditor extends SettingsEditor<ExternalSystemRunConfiguration> {

  @NotNull private final ExternalSystemTaskSettingsControl myControl;

  public ExternalSystemRunConfigurationEditor(@NotNull Project project, @NotNull ProjectSystemId externalSystemId) {
    myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
  }

  @Override
  protected void resetEditorFrom(@NotNull ExternalSystemRunConfiguration s) {
    myControl.setOriginalSettings(s.getSettings());
    myControl.reset(s.getProject());
  }

  @Override
  protected void applyEditorTo(@NotNull ExternalSystemRunConfiguration s) throws ConfigurationException {
    myControl.apply(s.getSettings());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    PaintAwarePanel result = new PaintAwarePanel(new GridBagLayout());
    myControl.fillUi(result, 0);
    result.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, 200), new Dimension(0, 0)),
               ExternalSystemUiUtil.getFillLineConstraints(0));
    ExternalSystemUiUtil.fillBottom(result);
    return result;
  }

  @Override
  protected void disposeEditor() {
    myControl.disposeUIResources();
  }
}
