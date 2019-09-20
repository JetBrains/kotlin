// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ModuleNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
public class IgnoreExternalProjectAction extends ExternalSystemToggleAction {

  private static final Logger LOG = Logger.getInstance(IgnoreExternalProjectAction.class);

  public IgnoreExternalProjectAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.ignore.external.projects.text", "External", 1));
    getTemplatePresentation()
      .setDescription(ExternalSystemBundle.message("action.ignore.external.projects.description", "external", 1));
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    final ProjectSystemId projectSystemId = getSystemId(e);
    final List<ExternalSystemNode<ExternalConfigPathAware>> projectNodes = getProjectNodes(e);
    if (projectNodes.isEmpty()) return;

    final Project project = getProject(e);
    ExternalSystemActionsCollector.trigger(project, projectSystemId, this, e);

    projectNodes.forEach(projectNode -> projectNode.setIgnored(state));

    Set<DataNode<ProjectData>> uniqueExternalProjects = projectNodes.stream()
      .map(
        projectNode -> {
          final String externalProjectPath = projectNode.getData().getLinkedExternalProjectPath();
          final ExternalProjectInfo externalProjectInfo =
            ExternalSystemUtil.getExternalProjectInfo(project, projectSystemId, externalProjectPath);
          final DataNode<ProjectData> projectDataNode =
            externalProjectInfo == null ? null : externalProjectInfo.getExternalProjectStructure();

          if (projectDataNode == null && LOG.isDebugEnabled()) {
            LOG.debug(String.format("external project data not found, path: %s, data: %s", externalProjectPath, externalProjectInfo));
          }
          return projectDataNode;
        }
      )
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    // async import to not block UI on big projects
    ProgressManager.getInstance().run(new Task.Backgroundable(project, e.getPresentation().getText(), false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        uniqueExternalProjects.forEach(
          externalProjectInfo -> ServiceManager.getService(ProjectDataManager.class).importData(externalProjectInfo, project, true)
        );
      }
    });
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    boolean selected = super.isSelected(e);
    ProjectSystemId systemId = getSystemId(e);
    final String systemIdNameText = systemId != null ? systemId.getReadableName() : "External";
    final String systemIdNameDescription = systemId != null ? systemId.getReadableName() : "external";

    int size = getProjectNodes(e).size();
    if (selected) {
      setText(e, ExternalSystemBundle.message("action.unignore.external.projects.text", systemIdNameText, size));
      setDescription(e, ExternalSystemBundle.message("action.unignore.external.projects.description", systemIdNameDescription, size));
    }
    else {
      setText(e, ExternalSystemBundle.message("action.ignore.external.projects.text", systemIdNameText, size));
      setDescription(e, ExternalSystemBundle.message("action.ignore.external.projects.description", systemIdNameDescription, size));
    }
    return selected;
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;
    return !getProjectNodes(e).isEmpty();
  }

  @Override
  protected boolean doIsSelected(@NotNull AnActionEvent e) {
    return ContainerUtil.exists(getProjectNodes(e), projectNode -> projectNode.isIgnored());
  }

  @NotNull
  private static List<ExternalSystemNode<ExternalConfigPathAware>> getProjectNodes(@NotNull AnActionEvent e) {
    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null || selectedNodes.isEmpty()) return Collections.emptyList();

    return selectedNodes.stream()
      .map(node -> (node instanceof ModuleNode || node instanceof ProjectNode) ? (ExternalSystemNode<ExternalConfigPathAware>)node : null)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
}
