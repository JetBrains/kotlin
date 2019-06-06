// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.action.ExternalSystemAction;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.CompositeDefinitionSource;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.ui.GradleProjectCompositeSelectorDialog;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Vladislav.Soroka
 */
public class GradleOpenProjectCompositeConfigurationAction extends ExternalSystemAction {

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;
    if (getSystemId(e) == null) return false;

    return e.getData(ExternalSystemDataKeys.SELECTED_PROJECT_NODE) != null;
  }

  @Override
  protected boolean isVisible(@NotNull AnActionEvent e) {
    final Project project = getProject(e);
    if (project == null) return false;
    ProjectSystemId systemId = getSystemId(e);
    if(!GradleConstants.SYSTEM_ID.equals(systemId)) return false;

    if (GradleSettings.getInstance(project).getLinkedProjectsSettings().size() > 1) {
      final ProjectNode projectNode = e.getData(ExternalSystemDataKeys.SELECTED_PROJECT_NODE);
      if (projectNode == null || projectNode.getData() == null) return false;

      GradleProjectSettings projectSettings =
        GradleSettings.getInstance(project).getLinkedProjectSettings(projectNode.getData().getLinkedExternalProjectPath());
      GradleProjectSettings.CompositeBuild compositeBuild = null;
      if (projectSettings != null) {
        compositeBuild = projectSettings.getCompositeBuild();
      }
      if (compositeBuild == null || compositeBuild.getCompositeDefinitionSource() == CompositeDefinitionSource.IDE) return true;
    }
    return false;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getProject(e);
    if (project == null) return;
    final ProjectNode projectNode = e.getData(ExternalSystemDataKeys.SELECTED_PROJECT_NODE);
    if (projectNode == null || projectNode.getData() == null) return;
    new GradleProjectCompositeSelectorDialog(project, projectNode.getData().getLinkedExternalProjectPath()).showAndGet();
  }
}