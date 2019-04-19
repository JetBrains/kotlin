// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.task.ui.ConfigureTasksActivationDialog;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class OpenTasksActivationManagerAction extends ExternalSystemNodeAction<AbstractExternalEntityData> {

  public OpenTasksActivationManagerAction() {
    super(AbstractExternalEntityData.class);
    getTemplatePresentation().setText(ExternalSystemBundle.message("external.system.task.activation.title"));
    getTemplatePresentation().setDescription(
      ExternalSystemBundle.message("external.system.task.activation.description", "external system"));
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;
    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null || selectedNodes.size() != 1) return false;
    final Object externalData = selectedNodes.get(0).getData();

    ProjectSystemId projectSystemId = getSystemId(e);
    e.getPresentation().setText(ExternalSystemBundle.message("external.system.task.activation.title"));
    e.getPresentation().setDescription(
      ExternalSystemBundle.message("external.system.task.activation.description", projectSystemId.getReadableName()));
    final boolean isProjectNode = externalData instanceof ProjectData || externalData instanceof ModuleData;
    return isProjectNode && StringUtil.isNotEmpty(((ExternalConfigPathAware) externalData).getLinkedExternalProjectPath());
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

    new ConfigureTasksActivationDialog(project, projectSystemId, externalConfigPathAware.getLinkedExternalProjectPath()).showAndGet();
  }
}
