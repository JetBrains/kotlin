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

package com.intellij.ui.tabs;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.FileColorManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author spleaner
 */
public class FileColorsConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private final Project myProject;
  private FileColorsConfigurablePanel myPanel;

  public FileColorsConfigurable(@NotNull final Project project) {
    myProject = project;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "File Colors";
  }

  @Override
  public String getHelpTopic() {
    return "reference.settings.ide.settings.file-colors";
  }

  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new FileColorsConfigurablePanel((FileColorManagerImpl) FileColorManager.getInstance(myProject));
    }

    return myPanel;
  }

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    if (myPanel != null) myPanel.apply();
  }

  @Override
  public void reset() {
    if (myPanel != null) myPanel.reset();
  }

  @Override
  public void disposeUIResources() {
    if (myPanel !=  null) {
      Disposer.dispose(myPanel);
      myPanel = null;
    }
  }

  @NotNull
  @Override
  public String getId() {
    return getHelpTopic();
  }
}
