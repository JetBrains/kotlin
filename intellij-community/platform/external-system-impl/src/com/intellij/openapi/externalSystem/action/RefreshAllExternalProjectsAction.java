// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Forces the ide to retrieve the most up-to-date info about the linked external projects and updates project state if necessary
 * (e.g. imports missing libraries).
 *
 * @author Denis Zhdanov
 */
public class RefreshAllExternalProjectsAction extends AnAction implements AnAction.TransparentUpdate, DumbAware {

  public RefreshAllExternalProjectsAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.refresh.all.projects.text", "external"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.refresh.all.projects.description", "external"));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final List<ProjectSystemId> systemIds = getSystemIds(e);
    if (systemIds.isEmpty()) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final String name = StringUtil.join(systemIds, projectSystemId -> projectSystemId.getReadableName(), ",");
    e.getPresentation().setText(ExternalSystemBundle.message("action.refresh.all.projects.text", name));
    e.getPresentation().setDescription(ExternalSystemBundle.message("action.refresh.all.projects.description", name));

    ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);
    e.getPresentation().setEnabled(!processingManager.hasTaskOfTypeInProgress(ExternalSystemTaskType.RESOLVE_PROJECT, project));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final List<ProjectSystemId> systemIds = getSystemIds(e);
    if (systemIds.isEmpty()) {
      e.getPresentation().setEnabled(false);
      return;
    }

    // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
    FileDocumentManager.getInstance().saveAllDocuments();

    ExternalProjectsManagerImpl.disableProjectWatcherAutoUpdate(project);
    for (ProjectSystemId externalSystemId : systemIds) {
      ExternalSystemActionsCollector.trigger(project, externalSystemId, this, e);
      ExternalSystemUtil.refreshProjects(
        new ImportSpecBuilder(project, externalSystemId)
          .forceWhenUptodate(true)
          .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      );
    }
  }

  private static List<ProjectSystemId> getSystemIds(@NotNull AnActionEvent e) {
    final List<ProjectSystemId> systemIds = new ArrayList<>();

    final ProjectSystemId externalSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId != null) {
      systemIds.add(externalSystemId);
    }
    else {
      for (ExternalSystemManager manager : ExternalSystemManager.EP_NAME.getExtensions()) {
        systemIds.add(manager.getSystemId());
      }
    }

    return systemIds;
  }
}
