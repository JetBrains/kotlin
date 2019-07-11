// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.project.ProjectRenameAware;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.ui.ExternalToolWindowManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.DisposeAwareRunnable;
import org.jetbrains.annotations.NotNull;

final class ExternalSystemStartupActivity implements StartupActivity, DumbAware {
  @Override
  public void runActivity(@NotNull final Project project) {
    ExternalProjectsManagerImpl.getInstance(project).init();

    Runnable task = () -> {
      for (ExternalSystemManager<?, ?, ?, ?, ?> manager: ExternalSystemApiUtil.getAllManagers()) {
        if (manager instanceof StartupActivity) {
          ((StartupActivity)manager).runActivity(project);
        }
      }
      if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) != Boolean.TRUE) {
        for (ExternalSystemManager manager: ExternalSystemManager.EP_NAME.getExtensions()) {
          final boolean isNewProject = project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) == Boolean.TRUE;
          if (isNewProject) {
            ExternalSystemUtil.refreshProjects(new ImportSpecBuilder(project, manager.getSystemId())
                                                 .createDirectoriesForEmptyContentRoots());
          }
        }
      }
      ExternalToolWindowManager.handle(project);
      ProjectRenameAware.beAware(project);
    };

    DumbService.getInstance(project).runWhenSmart(DisposeAwareRunnable.create(task, project));
  }
}
