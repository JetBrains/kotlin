// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;

/**
 * @author Vladislav.Soroka
 */
public class JavaGradleProjectSettingsControlBuilder extends IdeaGradleProjectSettingsControlBuilder {

  public JavaGradleProjectSettingsControlBuilder(@NotNull GradleProjectSettings initialSettings) {
    super(initialSettings);
  }

  @Override
  protected void resetGradleJdkComboBox(@Nullable Project project,
                                        GradleProjectSettings settings,
                                        @Nullable WizardContext wizardContext) {
    project = project == null || project.isDisposed() ? ProjectManager.getInstance().getDefaultProject() : project;
    ProjectStructureConfigurable structureConfigurable = ProjectStructureConfigurable.getInstance(project);
    ProjectSdksModel sdksModel = structureConfigurable.getProjectJdksModel();
    resetGradleJdkComboBox(project, settings, wizardContext, sdksModel);
  }
}
