// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Forces the ide to retrieve the most up-to-date info about the linked external project and updates project state if necessary
 * (e.g. imports missing libraries).
 *
 * @author Vladislav.Soroka
 */
public class RefreshExternalProjectAction extends ExternalSystemNodeAction<AbstractExternalEntityData> implements DumbAware {
  public RefreshExternalProjectAction() {
    super(AbstractExternalEntityData.class);
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.refresh.project.text", "External"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.refresh.project.description", "External"));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    if(this.getClass() != RefreshExternalProjectAction.class) return;

    ProjectSystemId systemId = getSystemId(e);
    final String systemIdNameText = systemId != null ? systemId.getReadableName() : "External";
    final String systemIdNameDescription = systemId != null ? systemId.getReadableName() : "external";
    Presentation presentation = e.getPresentation();
    presentation.setText(ExternalSystemBundle.message("action.refresh.project.text", systemIdNameText));
    presentation.setDescription(ExternalSystemBundle.message("action.refresh.project.description", systemIdNameDescription));
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;
    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null || selectedNodes.size() != 1) return false;
    final Object externalData = selectedNodes.get(0).getData();
    return (externalData instanceof ProjectData || externalData instanceof ModuleData);
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

    ExternalSystemUtil.refreshProject(externalProjectPath, new ImportSpecBuilder(project, projectSystemId));
  }
}
