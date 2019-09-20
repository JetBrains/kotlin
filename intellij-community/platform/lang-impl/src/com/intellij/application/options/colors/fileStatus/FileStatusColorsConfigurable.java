/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors.fileStatus;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.FileStatusFactory;
import com.intellij.openapi.vcs.FileStatusManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FileStatusColorsConfigurable implements SearchableConfigurable, Configurable.NoScroll, Configurable.VariableProjectAppLevel {

  private final static String FILE_STATUS_COLORS_ID = "file.status.colors";

  private final FileStatusColorsPanel myPanel;

  public FileStatusColorsConfigurable() {
    myPanel = new FileStatusColorsPanel(FileStatusFactory.getInstance().getAllFileStatuses());
  }

  @NotNull
  @Override
  public String getId() {
    return FILE_STATUS_COLORS_ID;
  }

  @Override
  public String getHelpTopic() {
    return "reference.versionControl.highlight";
  }

  @Nls
  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.file.status.colors");
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel.getComponent();
  }

  @Override
  public boolean isModified() {
    return myPanel.getModel().isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myPanel.getModel().apply();
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      FileStatusManager.getInstance(project).fileStatusesChanged();
    }
  }

  @Override
  public void reset() {
    myPanel.getModel().reset();
  }

  @Override
  public boolean isProjectLevel() {
    return false;
  }
}
