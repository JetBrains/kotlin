// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK;

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
    if (myGradleJdkComboBox == null) return;

    final String gradleJvm = settings.getGradleJvm();
    myGradleJdkComboBox.setProject(project);

    Sdk projectJdk = wizardContext != null ? wizardContext.getProjectJdk() : null;
    final String sdkItem = ObjectUtils.nullizeByCondition(gradleJvm, s ->
      (projectJdk == null && project == null && StringUtil.equals(USE_PROJECT_JDK, s)) || StringUtil.isEmpty(s));

    myGradleJdkComboBox.refreshData(sdkItem, projectJdk);

    if (myGradleJdkSetUpButton != null) {
      ProjectSdksModel sdksModel = ProjectStructureConfigurable.getInstance(
        project == null || project.isDisposed() ? ProjectManager.getInstance().getDefaultProject() : project).getProjectJdksModel();
      myGradleJdkComboBox.setSetupButton(myGradleJdkSetUpButton, sdksModel, null, id -> id instanceof JavaSdk);
    }
  }
}
