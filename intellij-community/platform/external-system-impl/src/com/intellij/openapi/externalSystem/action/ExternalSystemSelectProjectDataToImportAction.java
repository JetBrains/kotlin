// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectDataSelectorDialog;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemSelectProjectDataToImportAction extends ExternalSystemAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getProject(e);
    final ProjectSystemId projectSystemId = getSystemId(e);
    ExternalSystemActionsCollector.trigger(project, projectSystemId, this, e);

    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);

    final ExternalProjectInfo projectInfo;
    final ExternalSystemNode<?> externalSystemNode = ContainerUtil.getFirstItem(selectedNodes);
    if (externalSystemNode == null) {
      projectInfo = ContainerUtil.getFirstItem(ProjectDataManager.getInstance().getExternalProjectsData(project, projectSystemId));
    }
    else {
      final ProjectNode projectNode =
        externalSystemNode instanceof ProjectNode ? (ProjectNode)externalSystemNode : externalSystemNode.findParent(ProjectNode.class);
      assert projectNode != null;

      final ProjectData projectData = projectNode.getData();
      assert projectData != null;
      projectInfo =
        ProjectDataManager.getInstance().getExternalProjectData(project, projectSystemId, projectData.getLinkedExternalProjectPath());
    }

    final ExternalProjectDataSelectorDialog dialog;
    if (projectInfo != null) {
      dialog = new ExternalProjectDataSelectorDialog(project, projectInfo, externalSystemNode != null ? externalSystemNode.getData() : null);
      dialog.showAndGet();
    }
  }
}