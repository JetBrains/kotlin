// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.action.RefreshExternalProjectAction;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleRefreshProjectDependenciesAction extends RefreshExternalProjectAction {
  public GradleRefreshProjectDependenciesAction() {
    getTemplatePresentation().setText(GradleBundle.message("gradle.action.refresh.dependencies.text"));
    getTemplatePresentation().setDescription(GradleBundle.message("gradle.action.refresh.dependencies.description"));
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!GradleConstants.SYSTEM_ID.equals(getSystemId(e))) return false;
    return super.isEnabled(e);
  }

  @Override
  protected boolean isVisible(@NotNull AnActionEvent e) {
    if (!GradleConstants.SYSTEM_ID.equals(getSystemId(e))) return false;
    return super.isVisible(e);
  }

  @Override
  public void perform(@NotNull final Project project,
                      @NotNull ProjectSystemId projectSystemId,
                      @NotNull AbstractExternalEntityData externalEntityData,
                      @NotNull AnActionEvent e) {

    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    final ExternalSystemNode<?> externalSystemNode = ContainerUtil.getFirstItem(selectedNodes);
    assert externalSystemNode != null;

    final ExternalConfigPathAware externalConfigPathAware =
      externalSystemNode.getData() instanceof ExternalConfigPathAware ? (ExternalConfigPathAware)externalSystemNode.getData() : null;
    assert externalConfigPathAware != null;

    // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
    FileDocumentManager.getInstance().saveAllDocuments();

    final ExternalProjectSettings linkedProjectSettings = ExternalSystemApiUtil.getSettings(
      project, projectSystemId).getLinkedProjectSettings(externalConfigPathAware.getLinkedExternalProjectPath());
    final String externalProjectPath = linkedProjectSettings == null
                                       ? externalConfigPathAware.getLinkedExternalProjectPath()
                                       : linkedProjectSettings.getExternalProjectPath();


    ExternalSystemUtil.refreshProject(externalProjectPath,
                                      new ImportSpecBuilder(project, projectSystemId)
                                        .withArguments("--refresh-dependencies"));
  }
}