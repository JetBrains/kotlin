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
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemSettingsControlCustomizer;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public interface GradleProjectSettingsControlBuilder {

  /**
   * Hides/shows components added by the current control}.
   * @param show  flag which indicates if current control' components should be visible
   */
  void showUi(boolean show);

  /**
   * get initial settings
   * @return
   */
  GradleProjectSettings getInitialSettings();

  /**
   * Add Gradle JDK component to the panel
   */
  GradleProjectSettingsControlBuilder addGradleJdkComponents(JPanel content, int indentLevel);

  /**
   * Add Gradle distribution chooser component to the panel
   */
  GradleProjectSettingsControlBuilder addGradleChooserComponents(JPanel content, int indentLevel);

  boolean validate(GradleProjectSettings settings) throws ConfigurationException;

  void apply(GradleProjectSettings settings);

  /**
   * check if something was changed against initial settings
   * @return
   */
  boolean isModified();

  void reset(@Nullable Project project, GradleProjectSettings settings, boolean isDefaultModuleCreation);

  default void reset(@Nullable Project project,
                     GradleProjectSettings settings,
                     boolean isDefaultModuleCreation,
                     @Nullable WizardContext wizardContext) {
    reset(project, settings, isDefaultModuleCreation);
  }

  void createAndFillControls(PaintAwarePanel content, int indentLevel);

  void update(String linkedProjectPath, GradleProjectSettings settings, boolean isDefaultModuleCreation);

  @Nullable
  ExternalSystemSettingsControlCustomizer getExternalSystemSettingsControlCustomizer();

  void disposeUIResources();
}
