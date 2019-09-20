// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManagerImpl;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class DetachExternalProjectAction extends ExternalSystemNodeAction<ProjectData> {

  public DetachExternalProjectAction() {
    super(ProjectData.class);
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.detach.external.project.text", "External"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.detach.external.project.description"));
    getTemplatePresentation().setIcon(AllIcons.General.Remove);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    if(this.getClass() != DetachExternalProjectAction.class) return;

    ProjectSystemId systemId = getSystemId(e);
    final String systemIdName = systemId != null ? systemId.getReadableName() : "External";
    Presentation presentation = e.getPresentation();
    presentation.setText(ExternalSystemBundle.message("action.detach.external.project.text", systemIdName));
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;
    return e.getData(ExternalSystemDataKeys.SELECTED_PROJECT_NODE) != null;
  }

  @Override
  public void perform(@NotNull final Project project,
                      @NotNull ProjectSystemId projectSystemId,
                      @NotNull ProjectData projectData,
                      @NotNull AnActionEvent e) {

    e.getPresentation().setText(
      ExternalSystemBundle.message("action.detach.external.project.text", projectSystemId.getReadableName())
    );

    final ProjectNode projectNode = e.getData(ExternalSystemDataKeys.SELECTED_PROJECT_NODE);
    assert projectNode != null;

    ExternalSystemApiUtil.getLocalSettings(project, projectSystemId).
      forgetExternalProjects(Collections.singleton(projectData.getLinkedExternalProjectPath()));
    ExternalSystemApiUtil.getSettings(project, projectSystemId).unlinkExternalProject(projectData.getLinkedExternalProjectPath());

    ExternalProjectsManagerImpl.getInstance(project).forgetExternalProjectData(projectSystemId, projectData.getLinkedExternalProjectPath());

    // Process orphan modules.
    List<Module> orphanModules = new ArrayList<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (!ExternalSystemApiUtil.isExternalSystemAwareModule(projectSystemId, module)) continue;

      String path = ExternalSystemApiUtil.getExternalRootProjectPath(module);
      if (projectData.getLinkedExternalProjectPath().equals(path)) {
        orphanModules.add(module);
      }
    }

    if (!orphanModules.isEmpty()) {
      projectNode.getGroup().remove(projectNode);
      ProjectDataManagerImpl.getInstance().removeData(
        ProjectKeys.MODULE, orphanModules, Collections.emptyList(), projectData, project, false);
    }
  }
}
